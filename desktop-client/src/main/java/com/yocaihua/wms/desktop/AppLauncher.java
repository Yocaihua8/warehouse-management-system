package com.yocaihua.wms.desktop;

import com.yocaihua.wms.desktop.auth.AuthService;
import com.yocaihua.wms.desktop.auth.LoginResult;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.bootstrap.StartupCoordinator;
import com.yocaihua.wms.desktop.bootstrap.StartupState;
import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.config.AppConfigService;
import com.yocaihua.wms.desktop.ui.layout.ErrorPlaceholderView;
import com.yocaihua.wms.desktop.ui.layout.LoginPlaceholderView;
import com.yocaihua.wms.desktop.ui.layout.MainShellPlaceholderView;
import com.yocaihua.wms.desktop.ui.layout.StartupView;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application {

    private AppConfigService appConfigService;
    private AuthService authService;
    private ApiClient apiClient;
    private Scene scene;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.appConfigService = new AppConfigService();
        this.authService = new AuthService(appConfigService);
        this.apiClient = new ApiClient(appConfigService);
        StartupCoordinator startupCoordinator = new StartupCoordinator(appConfigService, authService);
        StartupContext startupContext = startupCoordinator.prepareInitialContext();

        this.scene = new Scene(createRoot(startupContext), 960, 640);
        String css = AppLauncher.class.getResource("/css/app.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle(startupContext.getAppDisplayName());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private Parent createRoot(StartupContext startupContext) {
        StartupState startupState = startupContext.getStartupState();
        if (startupState == StartupState.READY) {
            return new MainShellPlaceholderView(
                    startupContext,
                    apiClient,
                    appConfigService,
                    () -> handleLogout(startupContext),
                    () -> handleGoLogin(startupContext, "请重新登录后继续使用桌面端。")
            ).getRoot();
        }
        if (startupState == StartupState.NEEDS_LOGIN) {
            return new LoginPlaceholderView(
                    startupContext,
                    authService,
                    apiClient,
                    loginResult -> handleLoginSuccess(startupContext, loginResult)
            ).getRoot();
        }
        if (startupState == StartupState.ERROR) {
            return new ErrorPlaceholderView(startupContext).getRoot();
        }
        return new StartupView(startupContext).getRoot();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void handleLoginSuccess(StartupContext sourceContext, LoginResult loginResult) {
        StartupContext readyContext = new StartupContext();
        readyContext.setStartupState(StartupState.READY);
        readyContext.setAppDisplayName(sourceContext.getAppDisplayName());
        readyContext.setAppVersion(sourceContext.getAppVersion());
        readyContext.setServerBaseUrl(sourceContext.getServerBaseUrl());
        readyContext.setLastStatusRefreshTime(sourceContext.getLastStatusRefreshTime());
        readyContext.setHasLocalToken(true);
        readyContext.setLastUsername(loginResult.getUsername());
        readyContext.setAppStatus(sourceContext.getAppStatus());
        readyContext.setDatabaseStatus(sourceContext.getDatabaseStatus());
        readyContext.setAiStatus(sourceContext.getAiStatus());
        readyContext.setAiMessage(sourceContext.getAiMessage());
        readyContext.setAuthRequired(sourceContext.isAuthRequired());
        readyContext.setDesktopSupported(sourceContext.isDesktopSupported());
        readyContext.setCurrentUsername(loginResult.getUsername());
        readyContext.setStatusMessage("登录成功，已进入桌面端主界面占位。");

        scene.setRoot(new MainShellPlaceholderView(
                readyContext,
                apiClient,
                appConfigService,
                () -> handleLogout(readyContext),
                () -> handleGoLogin(readyContext, "请重新登录后继续使用桌面端。")
        ).getRoot());
        primaryStage.setTitle(readyContext.getAppDisplayName());
    }

    private void handleLogout(StartupContext sourceContext) {
        String message = authService.logout(apiClient);
        switchToLoginContext(sourceContext, message);
    }

    private void handleGoLogin(StartupContext sourceContext, String message) {
        authService.clearLocalToken();
        switchToLoginContext(sourceContext, message);
    }

    private void switchToLoginContext(StartupContext sourceContext, String message) {
        StartupContext loginContext = new StartupContext();
        loginContext.setStartupState(StartupState.NEEDS_LOGIN);
        loginContext.setAppDisplayName(sourceContext.getAppDisplayName());
        loginContext.setAppVersion(sourceContext.getAppVersion());
        loginContext.setServerBaseUrl(sourceContext.getServerBaseUrl());
        loginContext.setLastStatusRefreshTime(sourceContext.getLastStatusRefreshTime());
        loginContext.setHasLocalToken(false);
        loginContext.setLastUsername(
                sourceContext.getCurrentUsername() != null && !sourceContext.getCurrentUsername().trim().isEmpty()
                        ? sourceContext.getCurrentUsername().trim()
                        : sourceContext.getLastUsername()
        );
        loginContext.setAppStatus(sourceContext.getAppStatus());
        loginContext.setDatabaseStatus(sourceContext.getDatabaseStatus());
        loginContext.setAiStatus(sourceContext.getAiStatus());
        loginContext.setAiMessage(sourceContext.getAiMessage());
        loginContext.setAuthRequired(sourceContext.isAuthRequired());
        loginContext.setDesktopSupported(sourceContext.isDesktopSupported());
        loginContext.setStatusMessage(message);

        scene.setRoot(createRoot(loginContext));
        primaryStage.setTitle(loginContext.getAppDisplayName());
    }
}
