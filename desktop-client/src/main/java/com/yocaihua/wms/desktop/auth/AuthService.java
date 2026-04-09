package com.yocaihua.wms.desktop.auth;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.UserApi;
import com.yocaihua.wms.desktop.config.AppConfig;
import com.yocaihua.wms.desktop.config.AppConfigService;

public class AuthService {

    private final AppConfigService appConfigService;

    public AuthService(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    public boolean hasLocalToken() {
        AppConfig appConfig = appConfigService.load();
        String token = appConfig.getToken();
        return token != null && !token.trim().isEmpty();
    }

    public String getLocalToken() {
        return appConfigService.load().getToken();
    }

    public void clearLocalToken() {
        AppConfig appConfig = appConfigService.load();
        appConfig.setToken("");
        appConfigService.save(appConfig);
    }

    public LoginResult login(ApiClient apiClient, String username, String password) {
        if (isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (isBlank(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        LoginRequest request = new LoginRequest();
        request.setUsername(username.trim());
        request.setPassword(password);

        ApiResponse<LoginResult> response = apiClient.post(UserApi.LOGIN, request, LoginResult.class);
        if (response == null || !response.isSuccess() || response.getData() == null || isBlank(response.getData().getToken())) {
            String message = response == null ? "登录失败，请检查服务端是否可用" : response.getMessage();
            throw new ApiException(isBlank(message) ? "登录失败" : message);
        }

        saveLoginResult(response.getData());
        return response.getData();
    }

    public String logout(ApiClient apiClient) {
        String message = "已退出登录";
        try {
            ApiResponse<String> response = apiClient.post(UserApi.LOGOUT, null, String.class);
            if (response != null && response.isSuccess()) {
                String responseMessage = response.getData();
                if (isBlank(responseMessage)) {
                    responseMessage = response.getMessage();
                }
                message = isBlank(responseMessage) ? "已退出登录" : responseMessage;
            } else if (response != null && !isBlank(response.getMessage())) {
                message = "已退出本地登录，服务端提示：" + response.getMessage().trim();
            } else {
                message = "已退出本地登录";
            }
        } catch (ApiException ex) {
            message = "已退出本地登录，服务端退出确认失败。";
        } finally {
            clearLocalToken();
        }
        return message;
    }

    public void saveCurrentUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUsername() == null || currentUser.getUsername().trim().isEmpty()) {
            return;
        }
        AppConfig appConfig = appConfigService.load();
        appConfig.setLastUsername(currentUser.getUsername().trim());
        appConfigService.save(appConfig);
    }

    private void saveLoginResult(LoginResult loginResult) {
        AppConfig appConfig = appConfigService.load();
        appConfig.setToken(defaultText(loginResult.getToken()));
        appConfig.setLastUsername(defaultText(loginResult.getUsername()));
        appConfigService.save(appConfig);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }
}
