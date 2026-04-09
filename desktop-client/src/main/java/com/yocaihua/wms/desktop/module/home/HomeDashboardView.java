package com.yocaihua.wms.desktop.module.home;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.DashboardApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class HomeDashboardView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final VBox root;
    private final Button refreshButton;
    private final Label statusLabel;
    private final Label productCountValueLabel;
    private final Label customerCountValueLabel;
    private final Label stockProductCountValueLabel;
    private final Label lowStockCountValueLabel;
    private final Label inboundOrderCountValueLabel;
    private final Label outboundOrderCountValueLabel;

    public HomeDashboardView(StartupContext startupContext, ApiClient apiClient) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        Label titleLabel = new Label("工作台");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("第一版先展示核心统计概览和当前桌面端运行状态，不扩展图表和最近单据。");
        subtitleLabel.getStyleClass().add("page-subtitle");

        this.refreshButton = new Button("刷新统计");
        this.statusLabel = new Label("正在加载首页统计...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerActionRow = new HBox(12, statusLabel, headerSpacer, refreshButton);
        headerActionRow.setAlignment(Pos.CENTER_LEFT);

        this.productCountValueLabel = createStatValueLabel();
        this.customerCountValueLabel = createStatValueLabel();
        this.stockProductCountValueLabel = createStatValueLabel();
        this.lowStockCountValueLabel = createStatValueLabel();
        this.inboundOrderCountValueLabel = createStatValueLabel();
        this.outboundOrderCountValueLabel = createStatValueLabel();

        HBox firstRow = new HBox(16,
                createStatCard("商品总数", productCountValueLabel, "#409eff"),
                createStatCard("客户总数", customerCountValueLabel, "#67c23a"),
                createStatCard("库存商品数", stockProductCountValueLabel, "#303133")
        );
        HBox secondRow = new HBox(16,
                createStatCard("低库存商品数", lowStockCountValueLabel, "#e6a23c"),
                createStatCard("入库单总数", inboundOrderCountValueLabel, "#409eff"),
                createStatCard("出库单总数", outboundOrderCountValueLabel, "#f56c6c")
        );
        firstRow.setAlignment(Pos.CENTER_LEFT);
        secondRow.setAlignment(Pos.CENTER_LEFT);

        VBox summaryCard = new VBox(10);
        summaryCard.getStyleClass().add("page-card");
        Label summaryTitleLabel = new Label("当前运行状态");
        summaryTitleLabel.getStyleClass().add("page-label");
        Label summaryTextLabel = new Label(buildSummaryText());
        summaryTextLabel.getStyleClass().add("page-subtitle");
        summaryTextLabel.setWrapText(true);
        summaryCard.getChildren().addAll(summaryTitleLabel, summaryTextLabel);

        root.getChildren().addAll(titleLabel, subtitleLabel, headerActionRow, firstRow, secondRow, summaryCard);

        bindActions();
        loadDashboard();
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        refreshButton.setOnAction(event -> loadDashboard());
    }

    private void loadDashboard() {
        setLoadingState(true, "正在加载首页统计...");

        Task<DashboardData> loadTask = new Task<>() {
            @Override
            protected DashboardData call() {
                ApiResponse<DashboardData> response = apiClient.get(DashboardApi.DATA, DashboardData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "首页统计加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "首页统计加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            DashboardData data = loadTask.getValue();
            productCountValueLabel.setText(formatCount(data.getProductCount()));
            customerCountValueLabel.setText(formatCount(data.getCustomerCount()));
            stockProductCountValueLabel.setText(formatCount(data.getStockProductCount()));
            lowStockCountValueLabel.setText(formatCount(data.getLowStockCount()));
            inboundOrderCountValueLabel.setText(formatCount(data.getInboundOrderCount()));
            outboundOrderCountValueLabel.setText(formatCount(data.getOutboundOrderCount()));
            setLoadingState(false, "首页统计加载完成。");
        });

        loadTask.setOnFailed(event -> {
            clearDashboard();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-dashboard-query");
        thread.setDaemon(true);
        thread.start();
    }

    private VBox createStatCard(String title, Label valueLabel, String accentColor) {
        VBox card = new VBox(12);
        card.getStyleClass().add("page-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(260);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dcdfe6; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-label");

        valueLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: 700; -fx-text-fill: " + accentColor + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private Label createStatValueLabel() {
        Label label = new Label("0");
        label.setMinHeight(40);
        return label;
    }

    private void clearDashboard() {
        productCountValueLabel.setText("0");
        customerCountValueLabel.setText("0");
        stockProductCountValueLabel.setText("0");
        lowStockCountValueLabel.setText("0");
        inboundOrderCountValueLabel.setText("0");
        outboundOrderCountValueLabel.setText("0");
    }

    private String buildSummaryText() {
        return "当前用户：" + defaultText(startupContext.getCurrentUsername(), "未知")
                + "｜后端 " + defaultText(startupContext.getAppStatus(), "UNKNOWN")
                + " / 数据库 " + defaultText(startupContext.getDatabaseStatus(), "UNKNOWN")
                + " / AI " + defaultText(startupContext.getAiStatus(), "UNKNOWN")
                + "｜最近检查：" + defaultText(startupContext.getLastStatusRefreshTime(), "未记录");
    }

    private String formatCount(Long value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private void setLoadingState(boolean loading, String message) {
        refreshButton.setDisable(loading);
        statusLabel.setText(message);
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return "首页统计加载失败，请稍后重试。";
        }
        return throwable.getMessage().trim();
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
