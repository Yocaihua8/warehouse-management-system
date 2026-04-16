package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.service.SystemService;
import com.yocaihua.wms.vo.SystemBootstrapVO;
import com.yocaihua.wms.vo.SystemHealthVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SystemServiceImpl implements SystemService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Value("${ai.python.base-url:http://127.0.0.1:9000}")
    private String aiPythonBaseUrl;

    @Value("${spring.application.name:warehouse-management-system}")
    private String appName;

    @Value("${app.display-name:仓库管理系统}")
    private String appDisplayName;

    @Value("${app.version:1.7.0-SNAPSHOT}")
    private String appVersion;

    @Value("${auth.session-timeout-minutes:10080}")
    private Integer sessionTimeoutMinutes;

    public SystemServiceImpl(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    @Override
    public SystemHealthVO getSystemHealth() {
        SystemHealthVO healthVO = new SystemHealthVO();
        healthVO.setCheckedAt(LocalDateTime.now());
        healthVO.setAppStatus("UP");
        healthVO.setAppMessage("spring-boot 服务运行正常");

        String normalizedAiBaseUrl = resolveAiBaseUrl();
        healthVO.setAiBaseUrl(normalizedAiBaseUrl);

        fillDatabaseHealth(healthVO);
        fillAiHealth(healthVO, normalizedAiBaseUrl);

        boolean databaseUp = "UP".equals(healthVO.getDatabaseStatus());
        boolean aiUp = "UP".equals(healthVO.getAiStatus());
        healthVO.setOverallStatus(databaseUp && aiUp ? "UP" : "DEGRADED");
        return healthVO;
    }

    @Override
    public SystemBootstrapVO getSystemBootstrap() {
        SystemBootstrapVO bootstrapVO = new SystemBootstrapVO();
        bootstrapVO.setAppName(appName);
        bootstrapVO.setAppDisplayName(appDisplayName);
        bootstrapVO.setAppVersion(appVersion);
        bootstrapVO.setDesktopSupported(Boolean.TRUE);
        bootstrapVO.setAuthRequired(Boolean.TRUE);
        bootstrapVO.setSessionTimeoutMinutes(sessionTimeoutMinutes);
        bootstrapVO.setAiEnabled(Boolean.TRUE);
        bootstrapVO.setAiBaseUrl(resolveAiBaseUrl());
        bootstrapVO.setHealthPath("/system/health");
        bootstrapVO.setLoginPath("/user/login");
        bootstrapVO.setLogoutPath("/user/logout");
        bootstrapVO.setCurrentUserPath("/user/me");
        return bootstrapVO;
    }

    private void fillDatabaseHealth(SystemHealthVO healthVO) {
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (Integer.valueOf(1).equals(value)) {
                healthVO.setDatabaseStatus("UP");
                healthVO.setDatabaseMessage("mysql 连接正常");
                return;
            }
            healthVO.setDatabaseStatus("DOWN");
            healthVO.setDatabaseMessage("mysql 连通性检查返回异常结果");
        } catch (Exception ex) {
            healthVO.setDatabaseStatus("DOWN");
            healthVO.setDatabaseMessage("mysql 连接失败：" + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fillAiHealth(SystemHealthVO healthVO, String aiBaseUrl) {
        try {
            Map<String, Object> response = restTemplate.getForObject(aiBaseUrl + "/health", Map.class);
            boolean success = response != null && Boolean.TRUE.equals(response.get("success"));
            healthVO.setAiStatus(success ? "UP" : "DOWN");
            healthVO.setAiMessage(resolveAiMessage(response, success));
        } catch (Exception ex) {
            healthVO.setAiStatus("DOWN");
            healthVO.setAiMessage("python-ai-service 不可达：" + ex.getMessage());
        }
    }

    private String resolveAiMessage(Map<String, Object> response, boolean success) {
        if (response == null) {
            return "python-ai-service 返回空响应";
        }
        Object message = response.get("message");
        String text = message == null ? "" : String.valueOf(message).trim();
        if (!text.isEmpty()) {
            return text;
        }
        return success ? "python-ai-service 运行正常" : "python-ai-service 返回异常状态";
    }

    private String resolveAiBaseUrl() {
        String normalizedAiBaseUrl = aiPythonBaseUrl == null ? "" : aiPythonBaseUrl.trim();
        if (normalizedAiBaseUrl.isEmpty()) {
            return "http://127.0.0.1:9000";
        }
        return normalizedAiBaseUrl;
    }
}
