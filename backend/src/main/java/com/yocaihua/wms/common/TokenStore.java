package com.yocaihua.wms.common;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);
    private static final String TABLE_NAME = "user_session";
    private static final String REDIS_VALUE_SEPARATOR = "::";

    private final Map<String, SessionInfo> tokenMap = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${auth.session-timeout-minutes:10080}")
    private long sessionTimeoutMinutes;

    @Value("${auth.session-store:db}")
    private String sessionStore;

    @Value("${auth.redis.key-prefix:wms:session:}")
    private String redisKeyPrefix;

    public TokenStore(JdbcTemplate jdbcTemplate, ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @PostConstruct
    public void init() {
        if (isRedisStoreEnabled()) {
            log.info("TokenStore 使用 Redis 会话存储，key 前缀：{}", redisKeyPrefix);
            return;
        }
        ensureSessionTable();
        cleanupExpiredSessions();
        loadFromDatabase();
        log.info("TokenStore 使用数据库会话存储，表：{}", TABLE_NAME);
    }

    public void saveToken(String token, String username, String role) {
        if (token == null || token.trim().isEmpty() || username == null || username.trim().isEmpty()) {
            return;
        }
        SessionInfo sessionInfo = new SessionInfo(username.trim(), UserRoleConstant.normalize(role), buildExpiresAt());
        String normalizedToken = token.trim();
        tokenMap.put(normalizedToken, sessionInfo);

        if (isRedisStoreEnabled()) {
            if (!persistToRedis(normalizedToken, sessionInfo)) {
                tokenMap.remove(normalizedToken);
                throw new BusinessException("Redis 会话写入失败，请检查 Redis 服务配置");
            }
            return;
        }

        if (!persistToDatabase(normalizedToken, sessionInfo)) {
            tokenMap.remove(normalizedToken);
            throw new BusinessException("数据库会话写入失败，请检查数据库连接或会话表状态");
        }
    }

    public boolean containsToken(String token) {
        return getSession(token) != null;
    }

    public String getUsername(String token) {
        UserSession session = getSession(token);
        return session == null ? null : session.getUsername();
    }

    public String getRole(String token) {
        UserSession session = getSession(token);
        return session == null ? null : session.getRole();
    }

    public UserSession getSession(String token) {
        SessionInfo sessionInfo = getValidSession(token);
        if (sessionInfo == null) {
            return null;
        }
        return new UserSession(sessionInfo.getUsername(), sessionInfo.getRole());
    }

    public void removeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        String normalizedToken = token.trim();
        tokenMap.remove(normalizedToken);

        if (isRedisStoreEnabled()) {
            deleteFromRedis(normalizedToken);
            return;
        }
        deleteFromDatabase(normalizedToken);
    }

    private SessionInfo getValidSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        String normalizedToken = token.trim();

        if (isRedisStoreEnabled()) {
            SessionInfo redisSession = loadSessionByTokenFromRedis(normalizedToken);
            if (redisSession == null) {
                tokenMap.remove(normalizedToken);
                return null;
            }
            tokenMap.put(normalizedToken, redisSession);
            return redisSession;
        }

        long now = System.currentTimeMillis();
        SessionInfo dbSession = loadSessionByTokenFromDatabase(normalizedToken);
        if (dbSession == null) {
            tokenMap.remove(normalizedToken);
            return null;
        }
        if (dbSession.getExpiresAt() < now) {
            tokenMap.remove(normalizedToken);
            deleteFromDatabase(normalizedToken);
            return null;
        }

        SessionInfo refreshedSession = refreshSessionInDatabase(normalizedToken, dbSession);
        tokenMap.put(normalizedToken, refreshedSession);
        return refreshedSession;
    }

    private boolean isRedisStoreEnabled() {
        return "redis".equalsIgnoreCase(sessionStore);
    }

    private SessionInfo refreshSessionInDatabase(String token, SessionInfo currentSession) {
        SessionInfo refreshedSession = new SessionInfo(
                currentSession.getUsername(),
                UserRoleConstant.normalize(currentSession.getRole()),
                buildExpiresAt()
        );
        if (!persistToDatabase(token, refreshedSession)) {
            tokenMap.put(token, currentSession);
            return currentSession;
        }
        tokenMap.put(token, refreshedSession);
        return refreshedSession;
    }

    private long buildExpiresAt() {
        return System.currentTimeMillis() + sessionTimeoutMinutes * 60 * 1000;
    }

    private Duration sessionTtl() {
        return Duration.ofMinutes(Math.max(sessionTimeoutMinutes, 1));
    }

    private String buildRedisKey(String token) {
        return redisKeyPrefix + token;
    }

    private String encodeRedisValue(SessionInfo sessionInfo) {
        return sessionInfo.getUsername() + REDIS_VALUE_SEPARATOR + UserRoleConstant.normalize(sessionInfo.getRole());
    }

    private SessionInfo decodeRedisValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String[] segments = value.split(REDIS_VALUE_SEPARATOR, 2);
        if (segments.length != 2) {
            return null;
        }
        String username = segments[0] == null ? "" : segments[0].trim();
        if (username.isEmpty()) {
            return null;
        }
        String role = UserRoleConstant.normalize(segments[1]);
        return new SessionInfo(username, role, buildExpiresAt());
    }

    private boolean persistToRedis(String token, SessionInfo sessionInfo) {
        if (redisTemplate == null) {
            log.error("auth.session-store=redis 但 RedisTemplate 不可用");
            return false;
        }
        try {
            redisTemplate.opsForValue().set(buildRedisKey(token), encodeRedisValue(sessionInfo), sessionTtl());
            return true;
        } catch (Exception ex) {
            log.error("保存 Redis 会话失败，token={}", token, ex);
            return false;
        }
    }

    private SessionInfo loadSessionByTokenFromRedis(String token) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String key = buildRedisKey(token);
            String rawValue = redisTemplate.opsForValue().get(key);
            SessionInfo sessionInfo = decodeRedisValue(rawValue);
            if (sessionInfo == null) {
                return null;
            }
            redisTemplate.expire(key, sessionTtl());
            return sessionInfo;
        } catch (Exception ex) {
            log.warn("查询 Redis 会话失败，token={}", token, ex);
            return null;
        }
    }

    private void deleteFromRedis(String token) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(buildRedisKey(token));
        } catch (Exception ex) {
            log.warn("删除 Redis 会话失败，token={}", token, ex);
        }
    }

    private void ensureSessionTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                            "token VARCHAR(128) PRIMARY KEY," +
                            "username VARCHAR(64) NOT NULL," +
                            "role VARCHAR(20) NOT NULL," +
                            "expires_at BIGINT NOT NULL," +
                            "updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                            ")"
            );
        } catch (Exception ex) {
            throw new BusinessException("初始化会话表失败，请检查数据库权限");
        }
    }

    private void cleanupExpiredSessions() {
        try {
            long now = System.currentTimeMillis();
            jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE expires_at < ?", now);
        } catch (Exception ex) {
            log.warn("清理过期会话失败", ex);
        }
    }

    private void loadFromDatabase() {
        try {
            long now = System.currentTimeMillis();
            List<SessionRecord> records = jdbcTemplate.query(
                    "SELECT token, username, role, expires_at FROM " + TABLE_NAME + " WHERE expires_at >= ?",
                    (rs, rowNum) -> new SessionRecord(
                            rs.getString("token"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getLong("expires_at")
                    ),
                    now
            );
            for (SessionRecord record : records) {
                tokenMap.put(
                        record.getToken(),
                        new SessionInfo(
                                record.getUsername(),
                                UserRoleConstant.normalize(record.getRole()),
                                record.getExpiresAt()
                        )
                );
            }
        } catch (Exception ex) {
            log.warn("加载会话失败，将使用空会话缓存", ex);
        }
    }

    private SessionInfo loadSessionByTokenFromDatabase(String token) {
        try {
            List<SessionInfo> list = jdbcTemplate.query(
                    "SELECT username, role, expires_at FROM " + TABLE_NAME + " WHERE token = ?",
                    (rs, rowNum) -> new SessionInfo(
                            rs.getString("username"),
                            UserRoleConstant.normalize(rs.getString("role")),
                            rs.getLong("expires_at")
                    ),
                    token
            );
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception ex) {
            log.warn("查询会话失败，token={}", token, ex);
            return null;
        }
    }

    private boolean persistToDatabase(String token, SessionInfo sessionInfo) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO " + TABLE_NAME + " (token, username, role, expires_at) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE username = VALUES(username), role = VALUES(role), expires_at = VALUES(expires_at)",
                    token,
                    sessionInfo.getUsername(),
                    UserRoleConstant.normalize(sessionInfo.getRole()),
                    sessionInfo.getExpiresAt()
            );
            return true;
        } catch (Exception ex) {
            log.warn("保存会话失败，token={}", token, ex);
            return false;
        }
    }

    private void deleteFromDatabase(String token) {
        try {
            jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE token = ?", token);
        } catch (Exception ex) {
            log.warn("删除会话失败，token={}", token, ex);
        }
    }

    private static final class SessionRecord {
        private final String token;
        private final String username;
        private final String role;
        private final long expiresAt;

        private SessionRecord(String token, String username, String role, long expiresAt) {
            this.token = token;
            this.username = username;
            this.role = role;
            this.expiresAt = expiresAt;
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }

    private static final class SessionInfo {
        private final String username;
        private final String role;
        private final long expiresAt;

        private SessionInfo(String username, String role, long expiresAt) {
            this.username = username;
            this.role = role;
            this.expiresAt = expiresAt;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }

    public static final class UserSession {
        private final String username;
        private final String role;

        private UserSession(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}
