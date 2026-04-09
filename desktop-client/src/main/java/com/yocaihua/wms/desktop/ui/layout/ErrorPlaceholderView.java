package com.yocaihua.wms.desktop.ui.layout;

import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ErrorPlaceholderView {

    private final VBox root;

    public ErrorPlaceholderView(StartupContext startupContext) {
        this.root = new VBox(14);
        this.root.setPadding(new Insets(28));
        this.root.getStyleClass().add("page-root");

        VBox card = new VBox(12);
        card.getStyleClass().add("page-card");

        Label titleLabel = new Label("启动异常");
        titleLabel.getStyleClass().add("page-title");

        Label statusLabel = new Label(startupContext.getStatusMessage());
        statusLabel.getStyleClass().add("page-subtitle");

        Label serverLabel = new Label("服务地址：" + startupContext.getServerBaseUrl());
        serverLabel.getStyleClass().add("page-label");

        Label appLabel = new Label("后端状态：" + safeText(startupContext.getAppStatus()));
        appLabel.getStyleClass().add("page-label");

        Label dbLabel = new Label("数据库状态：" + safeText(startupContext.getDatabaseStatus()));
        dbLabel.getStyleClass().add("page-label");

        Label aiLabel = new Label("AI状态：" + safeText(startupContext.getAiStatus()));
        aiLabel.getStyleClass().add("page-label");

        Button retryButton = new Button("重新检查（下一步接入）");
        retryButton.setDisable(true);

        Label noteLabel = new Label("当前错误页只负责展示启动结果，下一步再接入“重试检测”和“修改服务地址”能力。");
        noteLabel.getStyleClass().add("placeholder-note");

        card.getChildren().addAll(
                titleLabel,
                statusLabel,
                serverLabel,
                appLabel,
                dbLabel,
                aiLabel,
                retryButton,
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
}
