package com.yocaihua.wms.desktop.module.stock;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.StockApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StockAdjustLogListView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final Runnable onBack;
    private final Consumer<StockAdjustLogRow> onViewBizOrderDetail;
    private final VBox root;
    private final TableView<StockAdjustLogRow> tableView;
    private final TextField productNameField;
    private final Label pageLabel;
    private final Label statusLabel;
    private final Button searchButton;
    private final Button resetButton;
    private final Button refreshButton;
    private final Button backButton;
    private final Button firstPageButton;
    private final Button previousButton;
    private final Button nextButton;
    private final Button lastPageButton;
    private final ComboBox<Integer> pageSizeBox;

    private int currentPage = 1;
    private int pageSize = 10;
    private long total = 0;

    public StockAdjustLogListView(StartupContext startupContext, ApiClient apiClient, Runnable onBack, Consumer<StockAdjustLogRow> onViewBizOrderDetail) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onBack = onBack;
        this.onViewBizOrderDetail = onViewBizOrderDetail;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        Label titleLabel = new Label("库存流水");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("第一版先支持按商品名称查询库存流水，用于追溯库存变化来源。");
        subtitleLabel.getStyleClass().add("page-subtitle");

        FlowPane queryRow = new FlowPane();
        queryRow.setHgap(12);
        queryRow.setVgap(12);
        queryRow.setAlignment(Pos.CENTER_LEFT);

        this.productNameField = new TextField();
        this.productNameField.setPromptText("商品名称");
        this.productNameField.setPrefWidth(220);

        this.searchButton = new Button("查询");
        this.resetButton = new Button("重置");
        this.refreshButton = new Button("刷新");
        this.backButton = new Button("返回库存列表");

        this.pageSizeBox = new ComboBox<>();
        this.pageSizeBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        this.pageSizeBox.setValue(pageSize);
        this.pageSizeBox.setPrefWidth(100);

        queryRow.getChildren().addAll(
                new Label("商品名称"),
                productNameField,
                searchButton,
                resetButton,
                refreshButton,
                backButton,
                new Label("每页"),
                pageSizeBox
        );

        this.tableView = new TableView<>();
        this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.tableView.setPlaceholder(new Label("暂无库存流水数据"));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        buildColumns();

        this.pageLabel = new Label();
        this.pageLabel.getStyleClass().add("page-label");

        this.statusLabel = new Label("正在加载库存流水...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        this.firstPageButton = new Button("首页");
        this.previousButton = new Button("上一页");
        this.nextButton = new Button("下一页");
        this.lastPageButton = new Button("末页");

        HBox footerRow = new HBox(12);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerRow.getChildren().addAll(statusLabel, spacer, pageLabel, firstPageButton, previousButton, nextButton, lastPageButton);

        card.getChildren().addAll(titleLabel, subtitleLabel, queryRow, tableView, footerRow);
        root.getChildren().add(card);

        bindActions();
        loadLogPage(1);
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        searchButton.setOnAction(event -> loadLogPage(1));
        resetButton.setOnAction(event -> {
            productNameField.clear();
            pageSize = 10;
            pageSizeBox.setValue(pageSize);
            loadLogPage(1);
        });
        refreshButton.setOnAction(event -> loadLogPage(currentPage));
        backButton.setOnAction(event -> {
            if (onBack != null) {
                onBack.run();
            }
        });
        pageSizeBox.setOnAction(event -> {
            Integer selectedSize = pageSizeBox.getValue();
            pageSize = selectedSize == null || selectedSize < 1 ? 10 : selectedSize;
            loadLogPage(1);
        });
        firstPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadLogPage(1);
            }
        });
        previousButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadLogPage(currentPage - 1);
            }
        });
        nextButton.setOnAction(event -> {
            if (hasNextPage()) {
                loadLogPage(currentPage + 1);
            }
        });
        lastPageButton.setOnAction(event -> {
            long lastPage = calculateTotalPages();
            if (lastPage > currentPage) {
                loadLogPage((int) lastPage);
            }
        });
    }

    private void buildColumns() {
        tableView.getColumns().add(createTextColumn("流水ID", row -> row.getId() == null ? "" : String.valueOf(row.getId()), 90));
        tableView.getColumns().add(createTextColumn("商品编码", StockAdjustLogRow::getProductCode, 120));
        tableView.getColumns().add(createTextColumn("商品名称", StockAdjustLogRow::getProductName, 160));
        tableView.getColumns().add(createChangeTypeColumn());
        tableView.getColumns().add(createNumberColumn("变更前库存", StockAdjustLogRow::getBeforeQuantity, 120));
        tableView.getColumns().add(createNumberColumn("变更后库存", StockAdjustLogRow::getAfterQuantity, 120));
        tableView.getColumns().add(createChangeQuantityColumn());
        tableView.getColumns().add(createTextColumn("关联单号", StockAdjustLogRow::getBizOrderNo, 160));
        tableView.getColumns().add(createTextColumn("操作人", StockAdjustLogRow::getOperatorName, 100));
        tableView.getColumns().add(createTextColumn("备注", StockAdjustLogRow::getRemarkText, 180));
        tableView.getColumns().add(createTextColumn("操作时间", StockAdjustLogRow::getCreatedTime, 180));
        tableView.getColumns().add(createActionColumn());
    }

    private TableColumn<StockAdjustLogRow, String> createTextColumn(String title, ValueProvider<String> provider, double width) {
        TableColumn<StockAdjustLogRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<StockAdjustLogRow, Integer> createNumberColumn(String title, ValueProvider<Integer> provider, double width) {
        TableColumn<StockAdjustLogRow, Integer> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(provider.get(cellData.getValue())));
        return column;
    }

    private TableColumn<StockAdjustLogRow, String> createChangeTypeColumn() {
        TableColumn<StockAdjustLogRow, String> column = new TableColumn<>("变更类型");
        column.setPrefWidth(140);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getChangeTypeText()));
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
                StockAdjustLogRow row = getTableRow() == null ? null : (StockAdjustLogRow) getTableRow().getItem();
                setStyle(resolveChangeTypeStyle(row == null ? null : row.getChangeType()));
            }
        });
        return column;
    }

    private TableColumn<StockAdjustLogRow, Integer> createChangeQuantityColumn() {
        TableColumn<StockAdjustLogRow, Integer> column = new TableColumn<>("变动数量");
        column.setPrefWidth(120);
        column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getChangeQuantity()));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(item));
                if (item >= 0) {
                    setStyle("-fx-text-fill: #67c23a; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #f56c6c; -fx-font-weight: 700;");
                }
            }
        });
        return column;
    }

    private TableColumn<StockAdjustLogRow, Void> createActionColumn() {
        TableColumn<StockAdjustLogRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(120);
        column.setCellFactory(col -> new TableCell<>() {
            private final Button detailButton = new Button("查看单据");
            private final Label placeholderLabel = new Label("-");

            {
                detailButton.setOnAction(event -> {
                    StockAdjustLogRow row = getTableRow() == null ? null : (StockAdjustLogRow) getTableRow().getItem();
                    if (row != null && row.canViewBizOrderDetail() && onViewBizOrderDetail != null) {
                        onViewBizOrderDetail.accept(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                StockAdjustLogRow row = (StockAdjustLogRow) getTableRow().getItem();
                if (row.canViewBizOrderDetail() && onViewBizOrderDetail != null) {
                    setGraphic(detailButton);
                } else {
                    setGraphic(placeholderLabel);
                }
            }
        });
        return column;
    }

    private void loadLogPage(int targetPage) {
        setLoadingState(true, "正在加载库存流水...");

        Task<StockAdjustLogPageData> loadTask = new Task<>() {
            @Override
            protected StockAdjustLogPageData call() {
                ApiResponse<StockAdjustLogPageData> response = apiClient.get(StockApi.LOG_LIST, buildQueryParams(targetPage), StockAdjustLogPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "库存流水加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "库存流水加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            StockAdjustLogPageData pageData = loadTask.getValue();
            List<StockAdjustLogRow> rows = pageData.getList();
            tableView.setItems(FXCollections.observableArrayList(rows));
            currentPage = safeInt(pageData.getPageNum(), targetPage);
            pageSize = safeInt(pageData.getPageSize(), pageSize);
            pageSizeBox.setValue(pageSize);
            total = pageData.getTotal() == null ? 0 : pageData.getTotal();
            refreshPageInfo();
            String status = total == 0
                    ? "未查到库存流水数据。"
                    : "库存流水加载完成，共 " + total + " 条记录。";
            setLoadingState(false, status);
        });

        loadTask.setOnFailed(event -> {
            tableView.setItems(FXCollections.observableArrayList());
            total = 0;
            currentPage = targetPage;
            refreshPageInfo();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-stock-log-query");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildQueryParams(int targetPage) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("productName", productNameField.getText());
        params.put("pageNum", targetPage);
        params.put("pageSize", pageSize);
        return params;
    }

    private void refreshPageInfo() {
        long totalPages = calculateTotalPages();
        pageLabel.setText("共 " + total + " 条，第 " + currentPage + " / " + totalPages + " 页");
        firstPageButton.setDisable(currentPage <= 1);
        previousButton.setDisable(currentPage <= 1);
        nextButton.setDisable(!hasNextPage());
        lastPageButton.setDisable(currentPage >= totalPages);
    }

    private boolean hasNextPage() {
        return currentPage * (long) pageSize < total;
    }

    private long calculateTotalPages() {
        return pageSize <= 0 ? 1 : Math.max(1, (total + pageSize - 1) / pageSize);
    }

    private void setLoadingState(boolean loading, String message) {
        searchButton.setDisable(loading);
        resetButton.setDisable(loading);
        refreshButton.setDisable(loading);
        backButton.setDisable(loading);
        pageSizeBox.setDisable(loading);
        firstPageButton.setDisable(loading || currentPage <= 1);
        previousButton.setDisable(loading || currentPage <= 1);
        nextButton.setDisable(loading || !hasNextPage());
        lastPageButton.setDisable(loading || currentPage >= calculateTotalPages());
        tableView.setDisable(loading);
        statusLabel.setText(message);
    }

    private String resolveChangeTypeStyle(String changeType) {
        if ("MANUAL_INBOUND".equals(changeType)) {
            return "-fx-text-fill: #67c23a; -fx-font-weight: 700;";
        }
        if ("AI_CONFIRM_INBOUND".equals(changeType)) {
            return "-fx-text-fill: #e6a23c; -fx-font-weight: 700;";
        }
        if ("MANUAL_OUTBOUND".equals(changeType) || "AI_CONFIRM_OUTBOUND".equals(changeType)) {
            return "-fx-text-fill: #f56c6c; -fx-font-weight: 700;";
        }
        if ("VOID_OUTBOUND".equals(changeType)) {
            return "-fx-text-fill: #67c23a; -fx-font-weight: 700;";
        }
        if ("VOID_INBOUND".equals(changeType)) {
            return "-fx-text-fill: #f56c6c; -fx-font-weight: 700;";
        }
        if ("MANUAL_ADJUST".equals(changeType)) {
            return "-fx-text-fill: #909399; -fx-font-weight: 700;";
        }
        return "-fx-text-fill: #606266;";
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return "库存流水加载失败，请稍后重试。";
        }
        return throwable.getMessage().trim();
    }

    private String defaultText(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface ValueProvider<T> {
        T get(StockAdjustLogRow row);
    }
}
