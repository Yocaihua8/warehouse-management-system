package com.yocaihua.wms.desktop.module.stock;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.module.inbound.InboundOrderDetailView;
import com.yocaihua.wms.desktop.module.outbound.OutboundOrderDetailView;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class StockModuleView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final StackPane root;
    private StockListView listView;
    private StockAdjustLogListView logListView;

    public StockModuleView(StartupContext startupContext, ApiClient apiClient) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.root = new StackPane();
        showList();
    }

    public Parent getRoot() {
        return root;
    }

    private void showList() {
        if (listView == null) {
            listView = new StockListView(startupContext, apiClient, this::showLogList);
        }
        root.getChildren().setAll(listView.getRoot());
    }

    private void showLogList() {
        if (logListView == null) {
            logListView = new StockAdjustLogListView(startupContext, apiClient, this::showList, this::showBizOrderDetail);
        }
        root.getChildren().setAll(logListView.getRoot());
    }

    private void showBizOrderDetail(StockAdjustLogRow row) {
        if (row == null || row.getBizOrderId() == null) {
            showLogList();
            return;
        }
        if (row.isInboundOrderType()) {
            root.getChildren().setAll(new InboundOrderDetailView(startupContext, apiClient, row.getBizOrderId(), this::showLogList).getRoot());
            return;
        }
        if (row.isOutboundOrderType()) {
            root.getChildren().setAll(new OutboundOrderDetailView(startupContext, apiClient, row.getBizOrderId(), this::showLogList).getRoot());
            return;
        }
        showLogList();
    }
}
