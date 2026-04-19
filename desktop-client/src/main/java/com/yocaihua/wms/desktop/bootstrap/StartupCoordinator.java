package com.yocaihua.wms.desktop.bootstrap;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.SystemApi;
import com.yocaihua.wms.desktop.api.endpoint.UserApi;
import com.yocaihua.wms.desktop.auth.AuthService;
import com.yocaihua.wms.desktop.auth.CurrentUser;
import com.yocaihua.wms.desktop.config.AppConfig;
import com.yocaihua.wms.desktop.config.AppConfigService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StartupCoordinator {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppConfigService appConfigService;
    private final AuthService authService;

    public StartupCoordinator(AppConfigService appConfigService, AuthService authService) {
        this.appConfigService = appConfigService;
        this.authService = authService;
    }

    public StartupContext prepareInitialContext() {
        AppConfig appConfig = appConfigService.load();
        ApiClient apiClient = new ApiClient(appConfigService);

        StartupContext context = new StartupContext();
        context.setAppDisplayName("仓库管理系统");
        context.setAppVersion("unknown");
        context.setServerBaseUrl(resolveServerBaseUrl(appConfig));
        context.setAiBaseUrl("http://127.0.0.1:9000");
        context.setHasLocalToken(authService.hasLocalToken());
        context.setLastUsername(appConfig.getLastUsername());
        context.setStartupState(StartupState.BOOTSTRAPPING);
        context.setStatusMessage("正在检查本地服务环境...");

        try {
            applyHealth(apiClient, context);
            applyBootstrap(apiClient, context);
            context.setLastStatusRefreshTime(formatNow());
            if (isDown(context.getDatabaseStatus())) {
                context.setStartupState(StartupState.ERROR);
                context.setStatusMessage("数据库连接不可用，请先启动本机数据库服务。");
                return context;
            }
            if (!context.isDesktopSupported()) {
                context.setStartupState(StartupState.ERROR);
                context.setStatusMessage("当前服务端未开放桌面端接入。");
                return context;
            }

            if (!context.isAuthRequired()) {
                context.setStartupState(StartupState.READY);
                context.setStatusMessage(buildReadyMessage(context, null));
                return context;
            }

            if (!context.isHasLocalToken()) {
                context.setStartupState(StartupState.NEEDS_LOGIN);
                context.setStatusMessage(buildNeedsLoginMessage(context, false));
                return context;
            }

            CurrentUser currentUser = fetchCurrentUser(apiClient);
            if (currentUser == null || isBlank(currentUser.getUsername())) {
                authService.clearLocalToken();
                context.setHasLocalToken(false);
                context.setStartupState(StartupState.NEEDS_LOGIN);
                context.setStatusMessage(buildNeedsLoginMessage(context, true));
                return context;
            }

            authService.saveCurrentUser(currentUser);
            context.setCurrentUsername(currentUser.getUsername());
            context.setStartupState(StartupState.READY);
            context.setStatusMessage(buildReadyMessage(context, currentUser));
            return context;
        } catch (StartupException ex) {
            context.setStartupState(StartupState.ERROR);
            context.setStatusMessage(ex.getMessage());
            context.setLastStatusRefreshTime(formatNow());
            if (isBlank(context.getAppStatus())) {
                context.setAppStatus("UNKNOWN");
            }
            if (isBlank(context.getDatabaseStatus())) {
                context.setDatabaseStatus("UNKNOWN");
            }
            if (isBlank(context.getAiStatus())) {
                context.setAiStatus("UNKNOWN");
            }
            return context;
        } catch (ApiException ex) {
            context.setStartupState(StartupState.ERROR);
            context.setStatusMessage("无法连接服务端，请检查服务地址或确认本机后端是否启动。");
            context.setLastStatusRefreshTime(formatNow());
            context.setAppStatus("DOWN");
            if (isBlank(context.getDatabaseStatus())) {
                context.setDatabaseStatus("UNKNOWN");
            }
            if (isBlank(context.getAiStatus())) {
                context.setAiStatus("UNKNOWN");
            }
            return context;
        }
    }

    private void applyHealth(ApiClient apiClient, StartupContext context) {
        ApiResponse<SystemHealthData> response = apiClient.get(SystemApi.HEALTH, SystemHealthData.class);
        SystemHealthData data = unwrapResponse(response, "系统健康检查失败");
        if (data == null) {
            throw new StartupException("系统健康检查未返回有效数据");
        }

        context.setAppStatus(defaultText(data.getAppStatus(), "UNKNOWN"));
        context.setDatabaseStatus(defaultText(data.getDatabaseStatus(), "UNKNOWN"));
        context.setAiStatus(defaultText(data.getAiStatus(), "UNKNOWN"));
        context.setAiMessage(defaultText(data.getAiMessage(), "AI 服务状态未知"));
        context.setAiBaseUrl(defaultText(data.getAiBaseUrl(), context.getAiBaseUrl()));
    }

    private void applyBootstrap(ApiClient apiClient, StartupContext context) {
        ApiResponse<SystemBootstrapData> response = apiClient.get(SystemApi.BOOTSTRAP, SystemBootstrapData.class);
        SystemBootstrapData data = unwrapResponse(response, "读取系统启动配置失败");
        if (data == null) {
            throw new StartupException("系统启动配置未返回有效数据");
        }

        context.setAppDisplayName(defaultText(data.getAppDisplayName(), context.getAppDisplayName()));
        context.setAppVersion(defaultText(data.getAppVersion(), context.getAppVersion()));
        context.setDesktopSupported(!Boolean.FALSE.equals(data.getDesktopSupported()));
        context.setAuthRequired(!Boolean.FALSE.equals(data.getAuthRequired()));
        context.setAiBaseUrl(defaultText(data.getAiBaseUrl(), context.getAiBaseUrl()));
    }

    private CurrentUser fetchCurrentUser(ApiClient apiClient) {
        ApiResponse<CurrentUser> response = apiClient.post(UserApi.CURRENT_USER, null, CurrentUser.class);
        if (response == null || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    private <T> T unwrapResponse(ApiResponse<T> response, String defaultMessage) {
        if (response == null) {
            throw new StartupException(defaultMessage);
        }
        if (!response.isSuccess()) {
            throw new StartupException(defaultText(response.getMessage(), defaultMessage));
        }
        return response.getData();
    }

    private String buildNeedsLoginMessage(StartupContext context, boolean tokenExpired) {
        if (tokenExpired) {
            return isDown(context.getAiStatus())
                    ? "本地登录已失效，请重新登录。当前 AI 服务不可用，但基础业务仍可登录后继续。"
                    : "本地登录已失效，请重新登录。";
        }
        return isDown(context.getAiStatus())
                ? "未检测到本地登录信息，请先登录。当前 AI 服务不可用，登录后只能先做非 AI 业务。"
                : "未检测到本地登录信息，请先登录。";
    }

    private String buildReadyMessage(StartupContext context, CurrentUser currentUser) {
        String userText = currentUser == null || isBlank(currentUser.getUsername())
                ? "已完成启动检查。"
                : "已完成启动检查，当前用户：" + currentUser.getUsername() + "。";
        if (isDown(context.getAiStatus())) {
            return userText + " 当前 AI 服务不可用，可先进入系统处理基础业务。";
        }
        return userText + " 后端、数据库、AI 服务均可用。";
    }

    private boolean isDown(String status) {
        return "DOWN".equalsIgnoreCase(status);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String resolveServerBaseUrl(AppConfig appConfig) {
        if (appConfig == null || appConfig.getServerConfig() == null) {
            return "http://127.0.0.1:8080";
        }
        String baseUrl = appConfig.getServerConfig().getBaseUrl();
        return isBlank(baseUrl) ? "http://127.0.0.1:8080" : baseUrl.trim();
    }

    private String formatNow() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
