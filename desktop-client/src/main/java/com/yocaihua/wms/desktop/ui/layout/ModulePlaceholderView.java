package com.yocaihua.wms.desktop.ui.layout;

import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ModulePlaceholderView {

    private final VBox root;

    public ModulePlaceholderView(ShellModule module, StartupContext startupContext) {
        this.root = new VBox(16);
        this.root.setPadding(new Insets(24));
        this.root.getStyleClass().add("page-root");

        VBox card = new VBox(10);
        card.getStyleClass().add("page-card");

        Label titleLabel = new Label(module.getPageTitle());
        titleLabel.getStyleClass().add("page-title");

        Label descriptionLabel = new Label(module.getDescription());
        descriptionLabel.getStyleClass().add("page-subtitle");

        Label serverLabel = new Label("服务地址：" + defaultText(startupContext.getServerBaseUrl(), "未配置"));
        serverLabel.getStyleClass().add("page-label");

        Label userLabel = new Label("当前用户：" + defaultText(startupContext.getCurrentUsername(), startupContext.getLastUsername()));
        userLabel.getStyleClass().add("page-label");

        Label statusLabel = new Label(
                "启动状态：后端 " + defaultText(startupContext.getAppStatus(), "UNKNOWN")
                        + " / 数据库 " + defaultText(startupContext.getDatabaseStatus(), "UNKNOWN")
                        + " / AI " + defaultText(startupContext.getAiStatus(), "UNKNOWN")
        );
        statusLabel.getStyleClass().add("page-label");

        Label nextStepLabel = new Label(module.getNextStepHint());
        nextStepLabel.getStyleClass().add("placeholder-note");

        card.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                serverLabel,
                userLabel,
                statusLabel,
                nextStepLabel
        );

        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }

    private String defaultText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue == null || defaultValue.trim().isEmpty() ? "未知" : defaultValue;
        }
        return value.trim();
    }
}
