package com.yocaihua.wms.desktop.module.outbound;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OutboundOrderDetailData {

    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String remark;
    private String createdTime;
    private List<OutboundOrderItemDetailRow> itemList = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public List<OutboundOrderItemDetailRow> getItemList() {
        return itemList;
    }

    public void setItemList(List<OutboundOrderItemDetailRow> itemList) {
        this.itemList = itemList == null ? new ArrayList<>() : itemList;
    }

    public String getTotalAmountText() {
        return totalAmount == null ? "-" : totalAmount.stripTrailingZeros().toPlainString();
    }

    public String getOrderStatusText() {
        if (Integer.valueOf(2).equals(orderStatus)) {
            return "已出库";
        }
        if (Integer.valueOf(3).equals(orderStatus)) {
            return "已作废";
        }
        return "草稿";
    }
}
