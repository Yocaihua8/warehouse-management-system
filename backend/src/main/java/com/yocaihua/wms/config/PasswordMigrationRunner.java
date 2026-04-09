package com.yocaihua.wms.config;

import com.yocaihua.wms.entity.User;
import com.yocaihua.wms.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时自动将数据库中的明文密码迁移为 BCrypt 哈希。
 * 判断依据：BCrypt 哈希均以 "$2a$" 开头，明文密码不会。
 * 已迁移过的用户会被跳过，幂等安全。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordMigrationRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userMapper.selectAll();
        int migratedCount = 0;
        for (User user : users) {
            String pwd = user.getPassword();
            if (pwd != null && !isBcryptHash(pwd)) {
                String encoded = passwordEncoder.encode(pwd);
                userMapper.updatePasswordById(user.getId(), encoded);
                migratedCount++;
                log.info("[密码迁移] 用户 '{}' 密码已迁移为 BCrypt 哈希", user.getUsername());
            }
        }
        if (migratedCount > 0) {
            log.info("[密码迁移] 共迁移 {} 个用户的明文密码，迁移完成", migratedCount);
        }
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }
}
