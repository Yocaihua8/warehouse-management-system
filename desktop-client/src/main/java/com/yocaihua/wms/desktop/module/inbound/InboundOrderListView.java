package com.yocaihua.wms.desktop.module.inbound;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.InboundOrderApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
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

public class InboundOrderListView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final VBox root;
    private final TableView<InboundOrderRow> tableView;
    private final TextField orderNoField;
    private final ComboBox<String> sourceTypeBox;
    private final Label pageLabel;
    private final Label statusLabel;
    private final Button createButton;
    private final Button searchButton;
    private final Button resetButton;
    private final Button refreshButton;
    private final Button restoreFilterButton;
    private final Button firstPageButton;
    private final Button previousButton;
    private final Button nextButton;
    private final Button lastPageButton;
    private final ComboBox<Integer> pageSizeBox;
    private final Consumer<Long> onViewDetail;

    private int currentPage = 1;
    private int pageSize = 10;
    private long total = 0;
    private final Runnable onCreateDraft;
    private Long pendingRevealOrderId;
    private String pendingRevealOrderNo;
    private boolean revealRetryByOrderNo;
    private String restorableOrderNo;
    private String restorableSourceType;
    private Integer restorablePage;
    private Integer restorablePageSize;
    private String nextSuccessStatusMessage;

    public InboundOrderListView(StartupContext startupContext, ApiClient apiClient, Consumer<Long> onViewDetail, Runnable onCreateDraft) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onViewDetail = onViewDetail;
        this.onCreateDraft = onCreateDraft;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        FlowPane queryRow = new FlowPane();
        queryRow.setHgap(12);
        queryRow.setVgap(12);
        queryRow.setAlignment(Pos.CENTER_LEFT);

        this.orderNoField = new TextField();
        this.orderNoField.setPromptText("入库单号");
        this.orderNoField.setPrefWidth(220);

        this.sourceTypeBox = new ComboBox<>();
        this.sourceTypeBox.setItems(FXCollections.observableArrayList("", "MANUAL", "AI"));
        this.sourceTypeBox.setValue("");
        this.sourceTypeBox.setPrefWidth(140);

        this.searchButton = new Button("查询");
        this.resetButton = new Button("重置");
        this.refreshButton = new Button("刷新");
        this.restoreFilterButton = new Button("恢复上次筛选");
        this.createButton = new Button("新增草稿");
        this.restoreFilterButton.setVisible(false);
        this.restoreFilterButton.setManaged(false);

        this.pageSizeBox = new ComboBox<>();
        this.pageSizeBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        this.pageSizeBox.setValue(pageSize);
        this.pageSizeBox.setPrefWidth(100);

        queryRow.getChildren().addAll(
                new Label("入库单号"),
                orderNoField,
                new Label("来源类型"),
                sourceTypeBox,
                searchButton,
                resetButton,
                refreshButton,
                restoreFilterButton,
                createButton,
                new Label("每页"),
                pageSizeBox
        );

        this.tableView = new TableView<>();
        this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.tableView.setPlaceholder(new Label("暂无入库单数据"));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        buildColumns();

        this.pageLabel = new Label();
        this.pageLabel.getStyleClass().add("page-label");

        this.statusLabel = new Label("正在加载入库单列表...");
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

        card.getChildren().addAll(queryRow, tableView, footerRow);
        root.getChildren().add(card);

        bindActions();
        loadInboundOrderPage(1);
    }

    public Parent getRoot() {
        return root;
    }

    public void reloadCurrentPage() {
        loadInboundOrderPage(currentPage);
    }

    public void revealCreatedOrder(Long orderId, String orderNo) {
        this.pendingRevealOrderId = orderId;
        this.pendingRevealOrderNo = normalizeText(orderNo);
        this.revealRetryByOrderNo = false;
        loadInboundOrderPage(currentPage);
    }

    private void restorePreviousFilter() {
        if (!hasRestoreState()) {
            return;
        }
        orderNoField.setText(defaultTextForInput(restorableOrderNo));
        sourceTypeBox.setValue(defaultTextForInput(restorableSourceType));
        pageSize = restorablePageSize == null || restorablePageSize < 1 ? 10 : restorablePageSize;
        pageSizeBox.setValue(pageSize);
        int targetPage = restorablePage == null || restorablePage < 1 ? 1 : restorablePage;
        clearRestoreState();
        nextSuccessStatusMessage = "已恢复上次筛选状态。";
        loadInboundOrderPage(targetPage);
    }

    private void bindActions() {
        searchButton.setOnAction(event -> {
            clearRestoreState();
            loadInboundOrderPage(1);
        });
        createButton.setOnAction(event -> onCreateDraft.run());
        restoreFilterButton.setOnAction(event -> restorePreviousFilter());
        resetButton.setOnAction(event -> {
            clearRestoreState();
            orderNoField.clear();
            sourceTypeBox.setValue("");
            pageSize = 10;
            pageSizeBox.setValue(pageSize);
            loadInboundOrderPage(1);
        });
        refreshButton.setOnAction(event -> loadInboundOrderPage(currentPage));
        pageSizeBox.setOnAction(event -> {
            Integer selectedSize = pageSizeBox.getValue();
            pageSize = selectedSize == null || selectedSize < 1 ? 10 : selectedSize;
            clearRestoreState();
            loadInboundOrderPage(1);
        });
        firstPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadInboundOrderPage(1);
            }
        });
        previousButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadInboundOrderPage(currentPage - 1);
            }
        });
        nextButton.setOnAction(event -> {
            if (hasNextPage()) {
                loadInboundOrderPage(currentPage + 1);
            }
        });
        lastPageButton.setOnAction(event -> {
            long lastPage = calculateTotalPages();
            if (lastPage > currentPage) {
                loadInboundOrderPage((int) lastPage);
            }
        });
    }

    private void buildColumns() {
        tableView.getColumns().add(createTextColumn("入库单号", InboundOrderRow::getOrderNo, 160));
        tableView.getColumns().add(createTextColumn("供应商", InboundOrderRow::getSupplierName, 160));
        tableView.getColumns().add(createTextColumn("总金额", InboundOrderRow::getTotalAmountText, 100));
        tableView.getColumns().add(createSourceTypeColumn());
        tableView.getColumns().add(createStatusColumn());
        tableView.getColumns().add(createTextColumn("备注", InboundOrderRow::getRemark, 180));
        tableView.getColumns().add(createTextColumn("创建时间", InboundOrderRow::getCreatedTime, 160));
        tableView.getColumns().add(createActionColumn());
    }

    private TableColumn<InboundOrderRow, String> createTextColumn(String title, ValueProvider provider, double width) {
        TableColumn<InboundOrderRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<InboundOrderRow, String> createSourceTypeColumn() {
        TableColumn<InboundOrderRow, String> column = new TableColumn<>("来源");
        column.setPrefWidth(80);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSourceTypeText()));
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
                InboundOrderRow row = getTableRow() == null ? null : (InboundOrderRow) getTableRow().getItem();
                if (row != null && "AI".equalsIgnoreCase(row.getSourceType())) {
                    setStyle("-fx-text-fill: #e6a23c; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #909399;");
                }
            }
        });
        return column;
    }

    private TableColumn<InboundOrderRow, String> createStatusColumn() {
        TableColumn<InboundOrderRow, String> column = new TableColumn<>("状态");
        column.setPrefWidth(80);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrderStatusText()));
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
                InboundOrderRow row = getTableRow() == null ? null : (InboundOrderRow) getTableRow().getItem();
                if (row == null) {
                    setStyle("");
                    return;
                }
                if (Integer.valueOf(2).equals(row.getOrderStatus())) {
                    setStyle("-fx-text-fill: #67c23a; -fx-font-weight: 700;");
                } else if (Integer.valueOf(3).equals(row.getOrderStatus())) {
                    setStyle("-fx-text-fill: #909399;");
                } else {
                    setStyle("-fx-text-fill: #e6a23c; -fx-font-weight: 700;");
                }
            }
        });
        return column;
    }

    private TableColumn<InboundOrderRow, String> createActionColumn() {
        TableColumn<InboundOrderRow, String> column = new TableColumn<>("操作");
        column.setPrefWidth(100);
        column.setCellValueFactory(cellData -> new SimpleStringProperty("查看详情"));
        column.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("查看详情");

            {
                viewButton.setOnAction(event -> {
                    InboundOrderRow row = getTableRow() == null ? null : (InboundOrderRow) getTableRow().getItem();
                    if (row != null && row.getId() != null) {
                        onViewDetail.accept(row.getId());
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
                setGraphic(viewButton);
            }
        });
        return column;
    }

    private void loadInboundOrderPage(int targetPage) {
        setLoadingState(true, "正在加载入库单列表...");

        Task<InboundOrderPageData> loadTask = new Task<>() {
            @Override
            protected InboundOrderPageData call() {
                ApiResponse<InboundOrderPageData> response = apiClient.get(InboundOrderApi.LIST, buildQueryParams(targetPage), InboundOrderPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "入库单列表加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "入库单列表加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            InboundOrderPageData pageData = loadTask.getValue();
            List<InboundOrderRow> rows = pageData.getList();
            tableView.setItems(FXCollections.observableArrayList(rows));
            currentPage = safeInt(pageData.getPageNum(), targetPage);
            pageSize = safeInt(pageData.getPageSize(), pageSize);
            pageSizeBox.setValue(pageSize);
            total = pageData.getTotal() == null ? 0 : pageData.getTotal();
            refreshPageInfo();
            String status = handlePendingReveal(rows);
            if (status == null) {
                status = takeNextSuccessStatusMessage();
            }
            if (status == null) {
                status = total == 0
                        ? "未查到入库单数据。"
                        : "入库单列表加载完成，共 " + total + " 条记录。";
            }
            setLoadingState(false, status);
        });

        loadTask.setOnFailed(event -> {
            tableView.setItems(FXCollections.observableArrayList());
            total = 0;
            currentPage = targetPage;
            refreshPageInfo();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-inbound-query");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildQueryParams(int targetPage) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("orderNo", orderNoField.getText());
        params.put("sourceType", sourceTypeBox.getValue());
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

    private long calculateTotalPages() {
        return pageSize <= 0 ? 1 : Math.max(1, (total + pageSize - 1) / pageSize);
    }

    private boolean hasNextPage() {
        return currentPage * (long) pageSize < total;
    }

    private void setLoadingState(boolean loading, String message) {
        searchButton.setDisable(loading);
        createButton.setDisable(loading);
        resetButton.setDisable(loading);
        refreshButton.setDisable(loading);
        restoreFilterButton.setDisable(loading || !hasRestoreState());
        sourceTypeBox.setDisable(loading);
        pageSizeBox.setDisable(loading);
        firstPageButton.setDisable(loading || currentPage <= 1);
        previousButton.setDisable(loading || currentPage <= 1);
        nextButton.setDisable(loading || !hasNextPage());
        lastPageButton.setDisable(loading || currentPage >= calculateTotalPages());
        tableView.setDisable(loading);
        statusLabel.setText(message);
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private String handlePendingReveal(List<InboundOrderRow> rows) {
        if (pendingRevealOrderId == null) {
            return null;
        }

        int rowIndex = findRowIndex(rows, pendingRevealOrderId);
        if (rowIndex >= 0) {
            tableView.getSelectionModel().select(rowIndex);
            tableView.scrollTo(rowIndex);
            String orderNo = isBlank(pendingRevealOrderNo) ? String.valueOf(pendingRevealOrderId) : pendingRevealOrderNo;
            clearPendingReveal();
            return "已定位到新建入库单：" + orderNo;
        }

        if (!revealRetryByOrderNo && !isBlank(pendingRevealOrderNo)) {
            captureRestoreState();
            revealRetryByOrderNo = true;
            orderNoField.setText(pendingRevealOrderNo);
            sourceTypeBox.setValue("MANUAL");
            loadInboundOrderPage(1);
            return "当前筛选页未包含新建入库单，正在按单号定位...";
        }

        String orderNo = isBlank(pendingRevealOrderNo) ? String.valueOf(pendingRevealOrderId) : pendingRevealOrderNo;
        clearPendingReveal();
        return "已刷新列表，但未能定位到新建入库单：" + orderNo;
    }

    private int findRowIndex(List<InboundOrderRow> rows, Long orderId) {
        if (rows == null || orderId == null) {
            return -1;
        }
        for (int i = 0; i < rows.size(); i++) {
            InboundOrderRow row = rows.get(i);
            if (row != null && orderId.equals(row.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void clearPendingReveal() {
        pendingRevealOrderId = null;
        pendingRevealOrderNo = null;
        revealRetryByOrderNo = false;
    }

    private void captureRestoreState() {
        if (hasRestoreState()) {
            return;
        }
        restorableOrderNo = orderNoField.getText();
        restorableSourceType = sourceTypeBox.getValue();
        restorablePage = currentPage;
        restorablePageSize = pageSize;
        refreshRestoreButton();
    }

    private boolean hasRestoreState() {
        return restorablePage != null;
    }

    private void clearRestoreState() {
        restorableOrderNo = null;
        restorableSourceType = null;
        restorablePage = null;
        restorablePageSize = null;
        refreshRestoreButton();
    }

    private void refreshRestoreButton() {
        boolean visible = hasRestoreState();
        restoreFilterButton.setVisible(visible);
        restoreFilterButton.setManaged(visible);
    }

    private String takeNextSuccessStatusMessage() {
        String message = nextSuccessStatusMessage;
        nextSuccessStatusMessage = null;
        return message;
    }

    private String defaultTextForInput(String value) {
        return value == null ? "" : value;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return "入库单列表加载失败，请稍后重试。";
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

    @FunctionalInterface
    private interface ValueProvider {
        String get(InboundOrderRow row);
    }
}
