package com.yocaihua.wms.desktop.module.outbound;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.OutboundOrderApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.print.DesktopPrintPreviewHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Path;

public class OutboundOrderDetailView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final Long orderId;
    private final Runnable onBack;
    private final VBox root;
    private final Label statusLabel;
    private final Button printPreviewButton;
    private final Button exportExcelButton;
    private final Button exportPdfButton;
    private final Button confirmButton;
    private final Button voidButton;
    private final Label orderNoValueLabel;
    private final Label customerValueLabel;
    private final Label totalAmountValueLabel;
    private final Label orderStatusValueLabel;
    private final Label createdTimeValueLabel;
    private final Label remarkValueLabel;
    private final TableView<OutboundOrderItemDetailRow> itemTable;
    private OutboundOrderDetailData currentDetail;
    private Integer currentOrderStatus;

    public OutboundOrderDetailView(StartupContext startupContext, ApiClient apiClient, Long orderId, Runnable onBack) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.orderId = orderId;
        this.onBack = onBack;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("出库单详情");
        titleLabel.getStyleClass().add("page-title");

        this.printPreviewButton = new Button("打印预览");
        this.printPreviewButton.setOnAction(event -> handlePrintPreview());
        this.exportExcelButton = new Button("导出Excel");
        this.exportExcelButton.setOnAction(event -> handleExportExcel());
        this.exportPdfButton = new Button("导出PDF");
        this.exportPdfButton.setOnAction(event -> handleExportPdf());
        this.confirmButton = new Button("确认出库");
        this.confirmButton.setOnAction(event -> handleConfirm());
        this.voidButton = new Button("作废");
        this.voidButton.setOnAction(event -> handleVoid());

        Button backButton = new Button("返回列表");
        backButton.setOnAction(event -> onBack.run());

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleLabel, printPreviewButton, exportExcelButton, exportPdfButton, confirmButton, voidButton, backButton);

        this.statusLabel = new Label("正在加载出库单详情...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(16);
        summaryGrid.setVgap(12);

        this.orderNoValueLabel = createValueLabel();
        this.customerValueLabel = createValueLabel();
        this.totalAmountValueLabel = createValueLabel();
        this.orderStatusValueLabel = createValueLabel();
        this.createdTimeValueLabel = createValueLabel();
        this.remarkValueLabel = createValueLabel();

        addSummaryRow(summaryGrid, 0, "出库单号", orderNoValueLabel, "客户名称", customerValueLabel);
        addSummaryRow(summaryGrid, 1, "总金额", totalAmountValueLabel, "状态", orderStatusValueLabel);
        addSummaryRow(summaryGrid, 2, "创建时间", createdTimeValueLabel, "备注", remarkValueLabel);

        Label itemTitleLabel = new Label("出库明细");
        itemTitleLabel.getStyleClass().add("page-title");
        itemTitleLabel.setStyle("-fx-font-size: 18px;");

        this.itemTable = new TableView<>();
        this.itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.itemTable.setPlaceholder(new Label("暂无明细数据"));
        VBox.setVgrow(itemTable, Priority.ALWAYS);
        buildColumns();

        card.getChildren().addAll(headerRow, statusLabel, summaryGrid, itemTitleLabel, itemTable);
        root.getChildren().add(card);

        refreshActionButtons();
        loadDetail();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildColumns() {
        itemTable.getColumns().add(createTextColumn("商品编码", OutboundOrderItemDetailRow::getProductCode, 140));
        itemTable.getColumns().add(createTextColumn("商品名称", OutboundOrderItemDetailRow::getProductName, 180));
        itemTable.getColumns().add(createTextColumn("规格", OutboundOrderItemDetailRow::getSpecification, 120));
        itemTable.getColumns().add(createTextColumn("单位", OutboundOrderItemDetailRow::getUnit, 80));
        itemTable.getColumns().add(createTextColumn("数量", row -> String.valueOf(row.getQuantity() == null ? 0 : row.getQuantity()), 80));
        itemTable.getColumns().add(createTextColumn("单价", OutboundOrderItemDetailRow::getUnitPriceText, 100));
        itemTable.getColumns().add(createTextColumn("金额", OutboundOrderItemDetailRow::getAmountText, 100));
        itemTable.getColumns().add(createTextColumn("备注", OutboundOrderItemDetailRow::getRemark, 160));
    }

    private TableColumn<OutboundOrderItemDetailRow, String> createTextColumn(String title, ValueProvider provider, double width) {
        TableColumn<OutboundOrderItemDetailRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private void loadDetail() {
        setLoadingState(true, "正在加载出库单详情...");

        Task<OutboundOrderDetailData> loadTask = new Task<>() {
            @Override
            protected OutboundOrderDetailData call() {
                ApiResponse<OutboundOrderDetailData> response = apiClient.get(OutboundOrderApi.DETAIL_PREFIX + orderId, OutboundOrderDetailData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "出库单详情加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "出库单详情加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            OutboundOrderDetailData detail = loadTask.getValue();
            renderDetail(detail);
            setLoadingState(false, "出库单详情加载完成。");
        });

        loadTask.setOnFailed(event -> {
            clearDetail();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-outbound-detail");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderDetail(OutboundOrderDetailData detail) {
        currentDetail = detail;
        currentOrderStatus = detail.getOrderStatus();
        orderNoValueLabel.setText(defaultText(detail.getOrderNo()));
        customerValueLabel.setText(defaultText(detail.getCustomerName()));
        totalAmountValueLabel.setText(defaultText(detail.getTotalAmountText()));
        orderStatusValueLabel.setText(defaultText(detail.getOrderStatusText()));
        createdTimeValueLabel.setText(defaultText(detail.getCreatedTime()));
        remarkValueLabel.setText(defaultText(detail.getRemark()));
        itemTable.setItems(FXCollections.observableArrayList(detail.getItemList()));
        refreshActionButtons();
    }

    private void clearDetail() {
        currentDetail = null;
        currentOrderStatus = null;
        orderNoValueLabel.setText("-");
        customerValueLabel.setText("-");
        totalAmountValueLabel.setText("-");
        orderStatusValueLabel.setText("-");
        createdTimeValueLabel.setText("-");
        remarkValueLabel.setText("-");
        itemTable.setItems(FXCollections.observableArrayList());
        refreshActionButtons();
    }

    private void setLoadingState(boolean loading, String message) {
        printPreviewButton.setDisable(loading || currentDetail == null);
        exportExcelButton.setDisable(loading || currentDetail == null);
        exportPdfButton.setDisable(loading || currentDetail == null);
        confirmButton.setDisable(loading || !canConfirm());
        voidButton.setDisable(loading || !canVoid());
        itemTable.setDisable(loading);
        statusLabel.setText(message);
    }

    private void handlePrintPreview() {
        if (currentDetail == null) {
            statusLabel.setText("出库单详情尚未加载完成，暂时不能打印预览。");
            return;
        }

        try {
            Path previewFile = DesktopPrintPreviewHelper.exportOutboundPreview(currentDetail);
            boolean opened = DesktopPrintPreviewHelper.openPreview(previewFile);
            if (opened) {
                statusLabel.setText("已打开出库单打印预览：" + previewFile.getFileName());
            } else {
                statusLabel.setText("已导出出库单打印预览文件：" + previewFile.toAbsolutePath());
            }
        } catch (Exception ex) {
            statusLabel.setText("打开出库单打印预览失败：" + resolveErrorMessage(ex));
        }
    }

    private void handleExportExcel() {
        exportOrderFile("导出出库单Excel", ".xlsx", OutboundOrderApi.EXPORT_EXCEL_SUFFIX, "正在导出出库单Excel...", "出库单Excel导出成功：");
    }

    private void handleExportPdf() {
        exportOrderFile("导出出库单PDF", ".pdf", OutboundOrderApi.EXPORT_PDF_SUFFIX, "正在导出出库单PDF...", "出库单PDF导出成功：");
    }

    private void exportOrderFile(String title, String extension, String apiSuffix, String loadingMessage, String successPrefix) {
        if (currentDetail == null) {
            statusLabel.setText("出库单详情尚未加载完成，暂时不能导出。");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(sanitizeFileName(defaultText(currentDetail.getOrderNo())) + extension);
        if (".xlsx".equals(extension)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF 文件", "*.pdf"));
        }

        java.io.File targetFile = fileChooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (targetFile == null) {
            statusLabel.setText("已取消导出。");
            return;
        }

        setLoadingState(true, loadingMessage);

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws IOException {
                byte[] content = apiClient.download("/outbound-order/" + orderId + apiSuffix, null);
                Files.write(Path.of(targetFile.toURI()), content);
                return null;
            }
        };

        exportTask.setOnSucceeded(event -> setLoadingState(false, successPrefix + targetFile.getName()));
        exportTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(exportTask.getException())));

        Thread thread = new Thread(exportTask, "desktop-outbound-export");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleConfirm() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认出库");
        confirmAlert.setHeaderText("确认后才会真正扣减库存");
        confirmAlert.setContentText("是否继续确认出库？");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        setLoadingState(true, "正在确认出库...");

        Task<String> confirmTask = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response = apiClient.post(
                        "/outbound-order/" + orderId + OutboundOrderApi.CONFIRM_SUFFIX,
                        null,
                        String.class
                );
                if (response == null || !response.isSuccess()) {
                    String message = response == null ? "确认出库失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "确认出库失败" : message);
                }
                return normalizeApiText(response.getData());
            }
        };

        confirmTask.setOnSucceeded(event -> {
            String message = normalizeApiText(confirmTask.getValue());
            loadDetail();
            statusLabel.setText(isBlank(message) ? "确认出库成功。" : message);
        });

        confirmTask.setOnFailed(event -> {
            setLoadingState(false, resolveErrorMessage(confirmTask.getException()));
        });

        Thread thread = new Thread(confirmTask, "desktop-outbound-confirm");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleVoid() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("作废出库单");
        dialog.setHeaderText(canConfirm() ? "作废后该草稿单据将不能再确认出库" : "作废后将自动回补本单据已扣减的库存");
        dialog.setContentText("作废原因：");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String voidReason = result.get() == null ? "" : result.get().trim();
        if (voidReason.isEmpty()) {
            statusLabel.setText("请输入作废原因。");
            return;
        }

        setLoadingState(true, "正在作废出库单...");

        Task<String> voidTask = new Task<>() {
            @Override
            protected String call() {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("voidReason", voidReason);

                ApiResponse<String> response = apiClient.post(
                        "/outbound-order/" + orderId + OutboundOrderApi.VOID_SUFFIX,
                        params,
                        null,
                        String.class
                );
                if (response == null || !response.isSuccess()) {
                    String message = response == null ? "作废出库单失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "作废出库单失败" : message);
                }
                return normalizeApiText(response.getData());
            }
        };

        voidTask.setOnSucceeded(event -> {
            String message = normalizeApiText(voidTask.getValue());
            loadDetail();
            statusLabel.setText(isBlank(message) ? "作废出库单成功。" : message);
        });

        voidTask.setOnFailed(event -> {
            setLoadingState(false, resolveErrorMessage(voidTask.getException()));
        });

        Thread thread = new Thread(voidTask, "desktop-outbound-void");
        thread.setDaemon(true);
        thread.start();
    }

    private void addSummaryRow(GridPane gridPane, int rowIndex, String label1, Label value1, String label2, Label value2) {
        Label leftLabel = new Label(label1 + "：");
        leftLabel.getStyleClass().add("page-label");
        Label rightLabel = new Label(label2 + "：");
        rightLabel.getStyleClass().add("page-label");

        gridPane.add(leftLabel, 0, rowIndex);
        gridPane.add(value1, 1, rowIndex);
        gridPane.add(rightLabel, 2, rowIndex);
        gridPane.add(value2, 3, rowIndex);
    }

    private Label createValueLabel() {
        Label label = new Label("-");
        label.getStyleClass().add("page-label");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return "出库单详情加载失败，请稍后重试。";
        }
        return throwable.getMessage().trim();
    }

    private String defaultText(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private String normalizeApiText(String value) {
        return value == null ? "" : value.trim();
    }

    private void refreshActionButtons() {
        printPreviewButton.setDisable(currentDetail == null);
        exportExcelButton.setDisable(currentDetail == null);
        exportPdfButton.setDisable(currentDetail == null);
        confirmButton.setVisible(canConfirm());
        confirmButton.setManaged(canConfirm());
        voidButton.setVisible(canVoid());
        voidButton.setManaged(canVoid());
        confirmButton.setDisable(!canConfirm());
        voidButton.setDisable(!canVoid());
    }

    private String sanitizeFileName(String value) {
        String source = isBlank(value) ? "outbound-order" : value.trim();
        return source.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private boolean canConfirm() {
        return Integer.valueOf(1).equals(currentOrderStatus);
    }

    private boolean canVoid() {
        return Integer.valueOf(1).equals(currentOrderStatus) || Integer.valueOf(2).equals(currentOrderStatus);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface ValueProvider {
        String get(OutboundOrderItemDetailRow row);
    }
}
