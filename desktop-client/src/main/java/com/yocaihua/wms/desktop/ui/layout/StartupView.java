package com.yocaihua.wms.desktop.ui.layout;

import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StartupView {

    private final VBox root;

    public StartupView(StartupContext startupContext) {
        this.root = new VBox(12);
        this.root.setAlignment(Pos.CENTER_LEFT);
        this.root.setPadding(new Insets(32));
        this.root.getStyleClass().add("startup-root");

        Label titleLabel = new Label(startupContext.getAppDisplayName());
        titleLabel.getStyleClass().add("startup-title");

        Label versionLabel = new Label("版本：" + startupContext.getAppVersion());
        versionLabel.getStyleClass().add("startup-text");

        Label serverLabel = new Label("服务地址：" + startupContext.getServerBaseUrl());
        serverLabel.getStyleClass().add("startup-text");

        Label tokenLabel = new Label("本地登录信息：" + (startupContext.isHasLocalToken() ? "已检测到" : "未检测到"));
        tokenLabel.getStyleClass().add("startup-text");

        Label appHealthLabel = new Label("后端状态：" + safeText(startupContext.getAppStatus()));
        appHealthLabel.getStyleClass().add("startup-text");

        Label databaseHealthLabel = new Label("数据库状态：" + safeText(startupContext.getDatabaseStatus()));
        databaseHealthLabel.getStyleClass().add("startup-text");

        Label aiHealthLabel = new Label("AI状态：" + safeText(startupContext.getAiStatus()));
        aiHealthLabel.getStyleClass().add("startup-text");

        Label authLabel = new Label("认证方式：" + (startupContext.isAuthRequired() ? "需要登录" : "免登录"));
        authLabel.getStyleClass().add("startup-text");

        String currentUserText = startupContext.getCurrentUsername() == null || startupContext.getCurrentUsername().trim().isEmpty()
                ? "当前用户：未完成用户自检"
                : "当前用户：" + startupContext.getCurrentUsername();
        Label currentUserLabel = new Label(currentUserText);
        currentUserLabel.getStyleClass().add("startup-text");

        Label statusLabel = new Label(startupContext.getStatusMessage());
        statusLabel.getStyleClass().add("startup-status");

        this.root.getChildren().addAll(
                titleLabel,
                versionLabel,
                serverLabel,
                tokenLabel,
                appHealthLabel,
                databaseHealthLabel,
                aiHealthLabel,
                authLabel,
                currentUserLabel,
                statusLabel
        );
    }

    public Parent getRoot() {
        return root;
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "UNKNOWN" : value;
    }
}
