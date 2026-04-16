package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.vo.SystemBootstrapVO;
import com.yocaihua.wms.vo.SystemHealthVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SystemServiceImpl systemService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(systemService, "aiPythonBaseUrl", "http://127.0.0.1:9000");
        ReflectionTestUtils.setField(systemService, "appName", "warehouse-management-system");
        ReflectionTestUtils.setField(systemService, "appDisplayName", "仓库管理系统");
        ReflectionTestUtils.setField(systemService, "appVersion", "1.7.0-SNAPSHOT");
        ReflectionTestUtils.setField(systemService, "sessionTimeoutMinutes", 10080);
    }

    @Test
    void getSystemHealth_shouldReturnUp_whenDatabaseAndAiAreHealthy() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(restTemplate.getForObject("http://127.0.0.1:9000/health", Map.class))
                .thenReturn(Map.of("success", true, "message", "python-ai ok"));

        SystemHealthVO result = systemService.getSystemHealth();

        assertEquals("UP", result.getOverallStatus());
        assertEquals("UP", result.getAppStatus());
        assertEquals("spring-boot 服务运行正常", result.getAppMessage());
        assertEquals("UP", result.getDatabaseStatus());
        assertEquals("mysql 连接正常", result.getDatabaseMessage());
        assertEquals("UP", result.getAiStatus());
        assertEquals("python-ai ok", result.getAiMessage());
        assertEquals("http://127.0.0.1:9000", result.getAiBaseUrl());
        assertNotNull(result.getCheckedAt());
    }

    @Test
    void getSystemHealth_shouldFallbackAiMessage_whenAiSuccessMessageBlank() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(restTemplate.getForObject("http://127.0.0.1:9000/health", Map.class))
                .thenReturn(Map.of("success", true, "message", "   "));

        SystemHealthVO result = systemService.getSystemHealth();

        assertEquals("UP", result.getOverallStatus());
        assertEquals("UP", result.getAiStatus());
        assertEquals("python-ai-service 运行正常", result.getAiMessage());
    }

    @Test
    void getSystemHealth_shouldReturnDegraded_whenDatabaseResultUnexpectedAndAiReportsFailure() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(0);
        when(restTemplate.getForObject("http://127.0.0.1:9000/health", Map.class))
                .thenReturn(Map.of("success", false));

        SystemHealthVO result = systemService.getSystemHealth();

        assertEquals("DEGRADED", result.getOverallStatus());
        assertEquals("DOWN", result.getDatabaseStatus());
        assertEquals("mysql 连通性检查返回异常结果", result.getDatabaseMessage());
        assertEquals("DOWN", result.getAiStatus());
        assertEquals("python-ai-service 返回异常状态", result.getAiMessage());
    }

    @Test
    void getSystemHealth_shouldReturnDegraded_whenDatabaseFailsAndAiReturnsNull() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new IllegalStateException("db down"));
        when(restTemplate.getForObject("http://127.0.0.1:9000/health", Map.class)).thenReturn(null);

        SystemHealthVO result = systemService.getSystemHealth();

        assertEquals("DEGRADED", result.getOverallStatus());
        assertEquals("DOWN", result.getDatabaseStatus());
        assertTrue(result.getDatabaseMessage().contains("db down"));
        assertEquals("DOWN", result.getAiStatus());
        assertEquals("python-ai-service 返回空响应", result.getAiMessage());
    }

    @Test
    void getSystemHealth_shouldReturnDegraded_whenAiEndpointThrows() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(restTemplate.getForObject("http://127.0.0.1:9000/health", Map.class))
                .thenThrow(new IllegalStateException("timeout"));

        SystemHealthVO result = systemService.getSystemHealth();

        assertEquals("DEGRADED", result.getOverallStatus());
        assertEquals("UP", result.getDatabaseStatus());
        assertEquals("DOWN", result.getAiStatus());
        assertEquals("python-ai-service 不可达：timeout", result.getAiMessage());
    }

    @Test
    void getSystemBootstrap_shouldAssembleConfiguredFields() {
        SystemBootstrapVO result = systemService.getSystemBootstrap();

        assertEquals("warehouse-management-system", result.getAppName());
        assertEquals("仓库管理系统", result.getAppDisplayName());
        assertEquals("1.7.0-SNAPSHOT", result.getAppVersion());
        assertEquals(Boolean.TRUE, result.getDesktopSupported());
        assertEquals(Boolean.TRUE, result.getAuthRequired());
        assertEquals(10080, result.getSessionTimeoutMinutes());
        assertEquals(Boolean.TRUE, result.getAiEnabled());
        assertEquals("http://127.0.0.1:9000", result.getAiBaseUrl());
        assertEquals("/system/health", result.getHealthPath());
        assertEquals("/user/login", result.getLoginPath());
        assertEquals("/user/logout", result.getLogoutPath());
        assertEquals("/user/me", result.getCurrentUserPath());
    }

    @Test
    void getSystemBootstrap_shouldFallbackAiBaseUrl_whenConfiguredBlank() {
        ReflectionTestUtils.setField(systemService, "aiPythonBaseUrl", "   ");

        SystemBootstrapVO result = systemService.getSystemBootstrap();

        assertEquals("http://127.0.0.1:9000", result.getAiBaseUrl());
    }
}
