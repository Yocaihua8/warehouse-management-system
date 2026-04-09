package com.yocaihua.wms.desktop.ui.layout;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.auth.AuthService;
import com.yocaihua.wms.desktop.auth.LoginResult;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginPlaceholderView {

    private final VBox root;

    public LoginPlaceholderView(
            StartupContext startupContext,
            AuthService authService,
            ApiClient apiClient,
            Consumer<LoginResult> onLoginSuccess
    ) {
        this.root = new VBox(14);
        this.root.setPadding(new Insets(28));
        this.root.getStyleClass().add("page-root");

        VBox card = new VBox(12);
        card.getStyleClass().add("page-card");
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("登录");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label(startupContext.getStatusMessage());
        subtitleLabel.getStyleClass().add("page-subtitle");

        Label serverLabel = new Label("服务地址：" + startupContext.getServerBaseUrl());
        serverLabel.getStyleClass().add("page-label");

        Label aiStatusLabel = new Label("AI状态：" + safeText(startupContext.getAiStatus()));
        aiStatusLabel.getStyleClass().add("page-label");

        TextField usernameField = new TextField(defaultText(startupContext.getLastUsername(), ""));
        usernameField.setPromptText("用户名");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("密码");

        Label statusLabel = new Label("请输入用户名和密码。");
        statusLabel.getStyleClass().add("page-subtitle");

        Button loginButton = new Button("登录");

        loginButton.setOnAction(event -> handleLogin(
                authService,
                apiClient,
                onLoginSuccess,
                usernameField,
                passwordField,
                loginButton,
                statusLabel
        ));

        Label noteLabel = new Label("当前已接入 /user/login，登录成功后会切到主界面占位，下一步再继续接真实主框架。");
        noteLabel.getStyleClass().add("placeholder-note");

        card.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                serverLabel,
                aiStatusLabel,
                usernameField,
                passwordField,
                statusLabel,
                loginButton,
                noteLabel
        );
        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "UNKNOWN" : value;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private void handleLogin(
            AuthService authService,
            ApiClient apiClient,
            Consumer<LoginResult> onLoginSuccess,
            TextField usernameField,
            PasswordField passwordField,
            Button loginButton,
            Label statusLabel
    ) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        loginButton.setDisable(true);
        statusLabel.setText("正在登录...");

        Task<LoginResult> loginTask = new Task<>() {
            @Override
            protected LoginResult call() {
                return authService.login(apiClient, username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            statusLabel.setText("登录成功，正在进入系统...");
            onLoginSuccess.accept(loginTask.getValue());
        });

        loginTask.setOnFailed(event -> {
            Throwable ex = loginTask.getException();
            statusLabel.setText(resolveErrorMessage(ex));
            loginButton.setDisable(false);
        });

        Thread thread = new Thread(loginTask, "desktop-login");
        thread.setDaemon(true);
        thread.start();
    }

    private String resolveErrorMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().trim().isEmpty()) {
            return "登录失败，请稍后重试。";
        }
        return ex.getMessage().trim();
    }
}
