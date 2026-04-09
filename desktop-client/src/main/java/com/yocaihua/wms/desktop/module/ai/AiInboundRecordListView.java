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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class AiInboundRecordListView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final VBox root;
    private final TableView<AiInboundRecordRow> tableView;
    private final Label statusLabel;
    private final Button refreshButton;
    private final Button uploadInboundButton;
    private final Button uploadOutboundButton;
    private final Consumer<Long> onViewDetail;
    private final Consumer<Long> onContinueConfirm;
    private final Consumer<AiInboundRecordDetailData> onUploadRecognized;
    private final Consumer<AiOutboundRecognizeData> onUploadOutboundRecognized;

    public AiInboundRecordListView(
            StartupContext startupContext,
            ApiClient apiClient,
            Consumer<Long> onViewDetail,
            Consumer<Long> onContinueConfirm,
            Consumer<AiInboundRecordDetailData> onUploadRecognized,
            Consumer<AiOutboundRecognizeData> onUploadOutboundRecognized
    ) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onViewDetail = onViewDetail;
        this.onContinueConfirm = onContinueConfirm;
        this.onUploadRecognized = onUploadRecognized;
        this.onUploadOutboundRecognized = onUploadOutboundRecognized;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label hintLabel = new Label("当前支持 AI 入库历史、AI 入库 / 出库上传识别、列表快捷继续确认和查看正式单据。");
        hintLabel.getStyleClass().add("placeholder-note");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.refreshButton = new Button("刷新");
        this.uploadInboundButton = new Button("上传入库识别");
        this.uploadOutboundButton = new Button("上传出库识别");
        actionRow.getChildren().addAll(hintLabel, spacer, uploadOutboundButton, uploadInboundButton, refreshButton);

        this.tableView = new TableView<>();
        this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.tableView.setPlaceholder(new Label("暂无AI入库历史"));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        buildColumns();

        this.statusLabel = new Label("正在加载AI入库历史...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        card.getChildren().addAll(actionRow, tableView, statusLabel);
        root.getChildren().add(card);

        bindActions();
        loadRecords();
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        refreshButton.setOnAction(event -> loadRecords());
        uploadInboundButton.setOnAction(event -> handleUploadInboundRecognize());
        uploadOutboundButton.setOnAction(event -> handleUploadOutboundRecognize());
    }

    private void buildColumns() {
        tableView.getColumns().add(createTextColumn("记录ID", row -> row.getId() == null ? "-" : String.valueOf(row.getId()), 90));
        tableView.getColumns().add(createTextColumn("任务编号", AiInboundRecordRow::getTaskNo, 220));
        tableView.getColumns().add(createTextColumn("文件名", AiInboundRecordRow::getSourceFileName, 220));
        tableView.getColumns().add(createTextColumn("供应商", AiInboundRecordRow::getSupplierName, 180));
        tableView.getColumns().add(createSupplierMatchColumn());
        tableView.getColumns().add(createRecognitionStatusColumn());
        tableView.getColumns().add(createTextColumn("正式入库单号", AiInboundRecordRow::getConfirmedOrderNoText, 160));
        tableView.getColumns().add(createTextColumn("创建时间", AiInboundRecordRow::getCreatedTime, 180));
        tableView.getColumns().add(createActionColumn());
    }

    private TableColumn<AiInboundRecordRow, String> createTextColumn(String title, ValueProvider provider, double width) {
        TableColumn<AiInboundRecordRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<AiInboundRecordRow, String> createSupplierMatchColumn() {
        TableColumn<AiInboundRecordRow, String> column = new TableColumn<>("供应商匹配");
        column.setPrefWidth(120);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSupplierMatchStatusText()));
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
                AiInboundRecordRow row = getTableRow() == null ? null : (AiInboundRecordRow) getTableRow().getItem();
                if (row == null) {
                    setStyle("");
                    return;
                }
                String status = normalizeText(row.getSupplierMatchStatus());
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

    private TableColumn<AiInboundRecordRow, String> createRecognitionStatusColumn() {
        TableColumn<AiInboundRecordRow, String> column = new TableColumn<>("识别状态");
        column.setPrefWidth(100);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRecognitionStatusText()));
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
                AiInboundRecordRow row = getTableRow() == null ? null : (AiInboundRecordRow) getTableRow().getItem();
                if (row == null) {
                    setStyle("");
                    return;
                }
                String status = normalizeText(row.getRecognitionStatus());
                if ("confirmed".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #67c23a; -fx-font-weight: 700;");
                } else if ("success".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #e6a23c; -fx-font-weight: 700;");
                } else if ("failed".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #f56c6c; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #909399;");
                }
            }
        });
        return column;
    }

    private TableColumn<AiInboundRecordRow, String> createActionColumn() {
        TableColumn<AiInboundRecordRow, String> column = new TableColumn<>("操作");
        column.setPrefWidth(180);
        column.setCellValueFactory(cellData -> new SimpleStringProperty("查看详情"));
        column.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("查看详情");
            private final Button continueConfirmButton = new Button("继续确认");
            private final HBox actionBox = new HBox(8, viewButton, continueConfirmButton);

            {
                viewButton.setOnAction(event -> {
                    AiInboundRecordRow row = getTableRow() == null ? null : (AiInboundRecordRow) getTableRow().getItem();
                    if (row != null && row.getId() != null) {
                        onViewDetail.accept(row.getId());
                    }
                });
                continueConfirmButton.setOnAction(event -> {
                    AiInboundRecordRow row = getTableRow() == null ? null : (AiInboundRecordRow) getTableRow().getItem();
                    if (row != null && row.getId() != null && canContinueConfirm(row)) {
                        onContinueConfirm.accept(row.getId());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                AiInboundRecordRow row = (AiInboundRecordRow) getTableRow().getItem();
                boolean canContinue = canContinueConfirm(row);
                continueConfirmButton.setVisible(canContinue);
                continueConfirmButton.setManaged(canContinue);
                setGraphic(actionBox);
            }
        });
        return column;
    }

    public void reloadRecords() {
        loadRecords();
    }

    private void handleUploadInboundRecognize() {
        Window ownerWindow = root.getScene() == null ? null : root.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择AI入库识别文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片或PDF", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp", "*.pdf"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile == null) {
            return;
        }

        setLoadingState(true, "正在上传文件并执行AI入库识别...");

        Task<AiInboundRecordDetailData> uploadTask = new Task<>() {
            @Override
            protected AiInboundRecordDetailData call() {
                ApiResponse<AiInboundRecordDetailData> response = apiClient.postMultipart(
                        AiApi.INBOUND_RECOGNIZE,
                        "file",
                        selectedFile.toPath(),
                        AiInboundRecordDetailData.class
                );
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "AI入库识别失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "AI入库识别失败" : message);
                }
                return response.getData();
            }
        };

        uploadTask.setOnSucceeded(event -> {
            AiInboundRecordDetailData detailData = uploadTask.getValue();
            if (detailData.getRecordId() == null) {
                setLoadingState(false, "AI入库识别成功，但未返回记录ID，请刷新历史列表查看。");
                reloadRecords();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("识别完成");
                alert.setHeaderText("AI入库识别已完成");
                alert.setContentText("未返回记录ID，请在历史列表中查看最新识别结果。");
                alert.showAndWait();
                return;
            }
            setLoadingState(false, "AI入库识别成功，正在打开识别详情...");
            onUploadRecognized.accept(detailData);
        });

        uploadTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(uploadTask.getException())));

        Thread thread = new Thread(uploadTask, "desktop-ai-inbound-upload");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleUploadOutboundRecognize() {
        Window ownerWindow = root.getScene() == null ? null : root.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择AI出库识别文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片或PDF", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp", "*.pdf"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile == null) {
            return;
        }

        setLoadingState(true, "正在上传文件并执行AI出库识别...");

        Task<AiOutboundRecognizeData> uploadTask = new Task<>() {
            @Override
            protected AiOutboundRecognizeData call() {
                ApiResponse<AiOutboundRecognizeData> response = apiClient.postMultipart(
                        AiApi.OUTBOUND_RECOGNIZE,
                        "file",
                        selectedFile.toPath(),
                        AiOutboundRecognizeData.class
                );
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "AI出库识别失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "AI出库识别失败" : message);
                }
                return response.getData();
            }
        };

        uploadTask.setOnSucceeded(event -> {
            AiOutboundRecognizeData detailData = uploadTask.getValue();
            if (detailData.getRecordId() == null) {
                setLoadingState(false, "AI出库识别成功，但未返回记录ID，请重新上传或检查后端返回结构。");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("识别完成");
                alert.setHeaderText("AI出库识别已完成");
                alert.setContentText("未返回记录ID，当前无法直接进入确认页。");
                alert.showAndWait();
                return;
            }
            setLoadingState(false, "AI出库识别成功，正在打开确认页...");
            onUploadOutboundRecognized.accept(detailData);
        });

        uploadTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(uploadTask.getException())));

        Thread thread = new Thread(uploadTask, "desktop-ai-outbound-upload");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadRecords() {
        setLoadingState(true, "正在加载AI入库历史...");

        Task<List<AiInboundRecordRow>> loadTask = new Task<>() {
            @Override
            protected List<AiInboundRecordRow> call() {
                ApiResponse<List<AiInboundRecordRow>> response = apiClient.getList(AiApi.INBOUND_LIST, AiInboundRecordRow.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "AI入库历史加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "AI入库历史加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<AiInboundRecordRow> records = loadTask.getValue();
            tableView.setItems(FXCollections.observableArrayList(records));
            String status = records.isEmpty()
                    ? "暂无AI入库历史记录。"
                    : "AI入库历史加载完成，共 " + records.size() + " 条记录。";
            setLoadingState(false, status);
        });

        loadTask.setOnFailed(event -> {
            tableView.setItems(FXCollections.observableArrayList());
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-ai-inbound-record-list");
        thread.setDaemon(true);
        thread.start();
    }

    private void setLoadingState(boolean loading, String message) {
        refreshButton.setDisable(loading);
        uploadInboundButton.setDisable(loading);
        uploadOutboundButton.setDisable(loading);
        tableView.setDisable(loading);
        statusLabel.setText(message);
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return "AI入库历史加载失败，请稍后重试。";
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

    private boolean canContinueConfirm(AiInboundRecordRow row) {
        return row != null
                && row.getId() != null
                && "success".equalsIgnoreCase(normalizeText(row.getRecognitionStatus()))
                && row.getConfirmedOrderId() == null;
    }

    @FunctionalInterface
    private interface ValueProvider {
        String get(AiInboundRecordRow row);
    }
}
