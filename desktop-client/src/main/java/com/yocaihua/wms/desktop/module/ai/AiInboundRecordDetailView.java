package com.yocaihua.wms.desktop.module.ai;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.AiApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class AiInboundRecordDetailView {

    private final ApiClient apiClient;
    private final Long recordId;
    private final Runnable onBack;
    private final Consumer<Long> onContinueConfirm;
    private final Consumer<Long> onViewInboundOrderDetail;
    private final String uploadResultNotice;
    private final VBox root;
    private final Label statusLabel;
    private final Label uploadResultLabel;
    private final Button continueConfirmButton;
    private final Button viewInboundOrderButton;
    private final Label recordIdValueLabel;
    private final Label taskNoValueLabel;
    private final Label sourceFileNameValueLabel;
    private final Label supplierNameValueLabel;
    private final Label matchedSupplierIdValueLabel;
    private final Label supplierMatchStatusValueLabel;
    private final Label recognitionStatusValueLabel;
    private final Label warningValueLabel;
    private final Label confirmedOrderIdValueLabel;
    private final Label confirmedOrderNoValueLabel;
    private final TextArea rawTextArea;
    private final TableView<AiInboundRecordItemRow> itemTable;
    private String currentRecognitionStatus;
    private Long currentConfirmedOrderId;

    public AiInboundRecordDetailView(
            StartupContext startupContext,
            ApiClient apiClient,
            Long recordId,
            Runnable onBack,
            Consumer<Long> onContinueConfirm,
            Consumer<Long> onViewInboundOrderDetail,
            String uploadResultNotice
    ) {
        this.apiClient = apiClient;
        this.recordId = recordId;
        this.onBack = onBack;
        this.onContinueConfirm = onContinueConfirm;
        this.onViewInboundOrderDetail = onViewInboundOrderDetail;
        this.uploadResultNotice = uploadResultNotice;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("AI入库识别详情");
        titleLabel.getStyleClass().add("page-title");

        this.continueConfirmButton = new Button("继续确认");
        this.continueConfirmButton.setOnAction(event -> handleContinueConfirm());
        this.viewInboundOrderButton = new Button("查看正式入库单");
        this.viewInboundOrderButton.setOnAction(event -> handleViewInboundOrderDetail());

        Button backButton = new Button("返回列表");
        backButton.setOnAction(event -> onBack.run());

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleLabel, viewInboundOrderButton, continueConfirmButton, backButton);

        this.statusLabel = new Label("正在加载AI识别详情...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        this.uploadResultLabel = new Label(defaultText(uploadResultNotice));
        this.uploadResultLabel.getStyleClass().add("page-label");
        this.uploadResultLabel.setWrapText(true);
        this.uploadResultLabel.setManaged(!isBlank(uploadResultNotice));
        this.uploadResultLabel.setVisible(!isBlank(uploadResultNotice));
        this.uploadResultLabel.setStyle("-fx-text-fill: #409eff; -fx-font-weight: 700;");

        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(16);
        summaryGrid.setVgap(12);

        this.recordIdValueLabel = createValueLabel();
        this.taskNoValueLabel = createValueLabel();
        this.sourceFileNameValueLabel = createValueLabel();
        this.supplierNameValueLabel = createValueLabel();
        this.matchedSupplierIdValueLabel = createValueLabel();
        this.supplierMatchStatusValueLabel = createValueLabel();
        this.recognitionStatusValueLabel = createValueLabel();
        this.warningValueLabel = createValueLabel();
        this.confirmedOrderIdValueLabel = createValueLabel();
        this.confirmedOrderNoValueLabel = createValueLabel();

        addSummaryRow(summaryGrid, 0, "记录ID", recordIdValueLabel, "任务编号", taskNoValueLabel);
        addSummaryRow(summaryGrid, 1, "文件名", sourceFileNameValueLabel, "供应商", supplierNameValueLabel);
        addSummaryRow(summaryGrid, 2, "匹配供应商ID", matchedSupplierIdValueLabel, "供应商匹配", supplierMatchStatusValueLabel);
        addSummaryRow(summaryGrid, 3, "识别状态", recognitionStatusValueLabel, "警告信息", warningValueLabel);
        addSummaryRow(summaryGrid, 4, "关联入库单ID", confirmedOrderIdValueLabel, "正式入库单号", confirmedOrderNoValueLabel);

        Label rawTextTitle = new Label("原始文本");
        rawTextTitle.getStyleClass().add("page-title");
        rawTextTitle.setStyle("-fx-font-size: 18px;");

        this.rawTextArea = new TextArea();
        this.rawTextArea.setEditable(false);
        this.rawTextArea.setWrapText(true);
        this.rawTextArea.setPrefRowCount(5);

        Label itemTitle = new Label("识别明细");
        itemTitle.getStyleClass().add("page-title");
        itemTitle.setStyle("-fx-font-size: 18px;");

        this.itemTable = new TableView<>();
        this.itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.itemTable.setPlaceholder(new Label("暂无识别明细"));
        VBox.setVgrow(itemTable, Priority.ALWAYS);
        buildColumns();

        card.getChildren().addAll(headerRow, statusLabel, uploadResultLabel, summaryGrid, rawTextTitle, rawTextArea, itemTitle, itemTable);
        root.getChildren().add(card);

        refreshActionButtons();
        loadDetail();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildColumns() {
        itemTable.getColumns().add(createTextColumn("行号", row -> row.getLineNo() == null ? "-" : String.valueOf(row.getLineNo()), 70));
        itemTable.getColumns().add(createTextColumn("商品名称", AiInboundRecordItemRow::getProductName, 180));
        itemTable.getColumns().add(createTextColumn("规格", AiInboundRecordItemRow::getSpecification, 120));
        itemTable.getColumns().add(createTextColumn("单位", AiInboundRecordItemRow::getUnit, 80));
        itemTable.getColumns().add(createTextColumn("数量", row -> row.getQuantity() == null ? "-" : String.valueOf(row.getQuantity()), 80));
        itemTable.getColumns().add(createTextColumn("单价", AiInboundRecordItemRow::getUnitPriceText, 100));
        itemTable.getColumns().add(createTextColumn("金额", AiInboundRecordItemRow::getAmountText, 100));
        itemTable.getColumns().add(createTextColumn("匹配商品ID", AiInboundRecordItemRow::getMatchedProductIdText, 120));
        itemTable.getColumns().add(createMatchStatusColumn());
        itemTable.getColumns().add(createTextColumn("备注", AiInboundRecordItemRow::getRemark, 160));
    }

    private TableColumn<AiInboundRecordItemRow, String> createTextColumn(String title, ValueProvider provider, double width) {
        TableColumn<AiInboundRecordItemRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<AiInboundRecordItemRow, String> createMatchStatusColumn() {
        TableColumn<AiInboundRecordItemRow, String> column = new TableColumn<>("匹配状态");
        column.setPrefWidth(120);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMatchStatusText()));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                AiInboundRecordItemRow row = getTableRow() == null ? null : (AiInboundRecordItemRow) getTableRow().getItem();
                if (row == null) {
                    setStyle("");
                    return;
                }
                String status = normalizeText(row.getMatchStatus());
                if ("matched_exact".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #67c23a; -fx-font-weight: 700;");
                } else if ("matched_fuzzy".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #e6a23c; -fx-font-weight: 700;");
                } else if (status != null && status.startsWith("manual_")) {
                    setStyle("-fx-text-fill: #409eff; -fx-font-weight: 700;");
                } else if ("unmatched".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #f56c6c; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #909399;");
                }
            }
        });
        return column;
    }

    private void loadDetail() {
        setLoadingState(true, "正在加载AI识别详情...");

        Task<AiInboundRecordDetailData> loadTask = new Task<>() {
            @Override
            protected AiInboundRecordDetailData call() {
                ApiResponse<AiInboundRecordDetailData> response = apiClient.get(AiApi.INBOUND_DETAIL_PREFIX + recordId, AiInboundRecordDetailData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "AI识别详情加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "AI识别详情加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            renderDetail(loadTask.getValue());
            setLoadingState(false, "AI识别详情加载完成。");
        });

        loadTask.setOnFailed(event -> {
            clearDetail();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-ai-inbound-detail");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderDetail(AiInboundRecordDetailData detail) {
        currentRecognitionStatus = detail.getRecognitionStatus();
        currentConfirmedOrderId = detail.getConfirmedOrderId();
        recordIdValueLabel.setText(detail.getRecordId() == null ? "-" : String.valueOf(detail.getRecordId()));
        taskNoValueLabel.setText(defaultText(detail.getTaskNo()));
        sourceFileNameValueLabel.setText(defaultText(detail.getSourceFileName()));
        supplierNameValueLabel.setText(defaultText(detail.getSupplierName()));
        matchedSupplierIdValueLabel.setText(detail.getMatchedSupplierIdText());
        supplierMatchStatusValueLabel.setText(defaultText(detail.getSupplierMatchStatusText()));
        recognitionStatusValueLabel.setText(defaultText(detail.getRecognitionStatusText()));
        warningValueLabel.setText(defaultText(detail.getWarningText()));
        confirmedOrderIdValueLabel.setText(detail.getConfirmedOrderIdText());
        confirmedOrderNoValueLabel.setText(defaultText(detail.getConfirmedOrderNoText()));
        rawTextArea.setText(detail.getRawText() == null ? "" : detail.getRawText());
        itemTable.setItems(FXCollections.observableArrayList(detail.getItemList() == null ? List.of() : detail.getItemList()));
        refreshActionButtons();
    }

    private void clearDetail() {
        currentRecognitionStatus = null;
        currentConfirmedOrderId = null;
        recordIdValueLabel.setText("-");
        taskNoValueLabel.setText("-");
        sourceFileNameValueLabel.setText("-");
        supplierNameValueLabel.setText("-");
        matchedSupplierIdValueLabel.setText("-");
        supplierMatchStatusValueLabel.setText("-");
        recognitionStatusValueLabel.setText("-");
        warningValueLabel.setText("-");
        confirmedOrderIdValueLabel.setText("-");
        confirmedOrderNoValueLabel.setText("-");
        rawTextArea.setText("");
        itemTable.setItems(FXCollections.observableArrayList());
        refreshActionButtons();
    }

    private void setLoadingState(boolean loading, String message) {
        continueConfirmButton.setDisable(loading || !canContinueConfirm());
        viewInboundOrderButton.setDisable(loading || !canViewInboundOrderDetail());
        itemTable.setDisable(loading);
        rawTextArea.setDisable(loading);
        statusLabel.setText(message);
    }

    private void handleContinueConfirm() {
        if (!canContinueConfirm()) {
            statusLabel.setText("当前记录不能继续确认。");
            return;
        }
        onContinueConfirm.accept(recordId);
    }

    private void handleViewInboundOrderDetail() {
        if (!canViewInboundOrderDetail()) {
            statusLabel.setText("当前记录还没有生成正式入库单。");
            return;
        }
        onViewInboundOrderDetail.accept(currentConfirmedOrderId);
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
            return "AI识别详情加载失败，请稍后重试。";
        }
        return throwable.getMessage().trim();
    }

    private String defaultText(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void refreshActionButtons() {
        boolean canContinue = canContinueConfirm();
        boolean canViewOrder = canViewInboundOrderDetail();
        continueConfirmButton.setVisible(canContinue);
        continueConfirmButton.setManaged(canContinue);
        continueConfirmButton.setDisable(!canContinue);
        viewInboundOrderButton.setVisible(canViewOrder);
        viewInboundOrderButton.setManaged(canViewOrder);
        viewInboundOrderButton.setDisable(!canViewOrder);
    }

    private boolean canContinueConfirm() {
        return "success".equalsIgnoreCase(currentRecognitionStatus) && currentConfirmedOrderId == null;
    }

    private boolean canViewInboundOrderDetail() {
        return currentConfirmedOrderId != null;
    }

    @FunctionalInterface
    private interface ValueProvider {
        String get(AiInboundRecordItemRow row);
    }
}
