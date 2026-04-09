package com.yocaihua.wms.desktop.module.inbound;

import java.math.BigDecimal;

public class InboundOrderRow {

    private Long id;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String sourceType;
    private String remark;
    private String createdTime;

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

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
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

    public String getTotalAmountText() {
        return totalAmount == null ? "-" : totalAmount.stripTrailingZeros().toPlainString();
    }

    public String getSourceTypeText() {
        return "AI".equalsIgnoreCase(sourceType) ? "AI" : "手工";
    }

    public String getOrderStatusText() {
        if (Integer.valueOf(2).equals(orderStatus)) {
            return "已入库";
        }
        if (Integer.valueOf(3).equals(orderStatus)) {
            return "已作废";
        }
        return "草稿";
    }
}
