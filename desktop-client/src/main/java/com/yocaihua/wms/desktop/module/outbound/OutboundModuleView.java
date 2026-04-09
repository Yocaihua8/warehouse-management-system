package com.yocaihua.wms.desktop.module.outbound;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.OrderCreatedData;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class OutboundModuleView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final Runnable onGoCustomerManagement;
    private final java.util.function.Consumer<String> onDraftStateChanged;
    private final boolean openCreateInitially;
    private final String initialCustomerCode;
    private final String initialCustomerName;
    private final StackPane root;
    private OutboundOrderListView listView;
    private OrderCreatedData pendingCreatedOrder;

    public OutboundModuleView(StartupContext startupContext,
                              ApiClient apiClient,
                              Runnable onGoCustomerManagement,
                              java.util.function.Consumer<String> onDraftStateChanged,
                              boolean openCreateInitially,
                              String initialCustomerCode,
                              String initialCustomerName) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onGoCustomerManagement = onGoCustomerManagement;
        this.onDraftStateChanged = onDraftStateChanged;
        this.openCreateInitially = openCreateInitially;
        this.initialCustomerCode = initialCustomerCode;
        this.initialCustomerName = initialCustomerName;
        this.root = new StackPane();
        if (openCreateInitially) {
            showCreate();
        } else {
            showList(false);
        }
    }

    public Parent getRoot() {
        return root;
    }

    private void showList(boolean reload) {
        notifyDraftState("未打开");
        if (listView == null) {
            listView = new OutboundOrderListView(startupContext, apiClient, this::showDetail, this::showCreate);
        } else if (pendingCreatedOrder != null) {
            listView.revealCreatedOrder(pendingCreatedOrder.getId(), pendingCreatedOrder.getOrderNo());
            pendingCreatedOrder = null;
        } else if (reload) {
            listView.reloadCurrentPage();
        }
        root.getChildren().setAll(listView.getRoot());
    }

    private void showCreate() {
        notifyDraftState("编辑中（手工出库草稿）");
        root.getChildren().setAll(new OutboundOrderCreateView(
                startupContext,
                apiClient,
                () -> showList(false),
                this::showCreatedDetail,
                onGoCustomerManagement,
                initialCustomerCode,
                initialCustomerName
        ).getRoot());
    }

    private void showDetail(Long orderId) {
        notifyDraftState("未打开");
        root.getChildren().setAll(new OutboundOrderDetailView(startupContext, apiClient, orderId, () -> showList(true)).getRoot());
    }

    private void showCreatedDetail(OrderCreatedData createdOrder) {
        this.pendingCreatedOrder = createdOrder;
        if (createdOrder == null || createdOrder.getId() == null) {
            showList(true);
            return;
        }
        showDetail(createdOrder.getId());
    }

    private void notifyDraftState(String statusText) {
        if (onDraftStateChanged != null) {
            onDraftStateChanged.accept(statusText);
        }
    }
}
