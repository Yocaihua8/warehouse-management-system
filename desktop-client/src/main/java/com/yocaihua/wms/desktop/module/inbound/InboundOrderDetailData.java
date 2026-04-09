package com.yocaihua.wms.desktop.module.inbound;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InboundOrderDetailData {

    private Long id;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String sourceType;
    private Long aiRecordId;
    private String aiTaskNo;
    private String aiSourceFileName;
    private String remark;
    private String createdTime;
    private List<InboundOrderItemDetailRow> itemList = new ArrayList<>();

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

    public Long getAiRecordId() {
        return aiRecordId;
    }

    public void setAiRecordId(Long aiRecordId) {
        this.aiRecordId = aiRecordId;
    }

    public String getAiTaskNo() {
        return aiTaskNo;
    }

    public void setAiTaskNo(String aiTaskNo) {
        this.aiTaskNo = aiTaskNo;
    }

    public String getAiSourceFileName() {
        return aiSourceFileName;
    }

    public void setAiSourceFileName(String aiSourceFileName) {
        this.aiSourceFileName = aiSourceFileName;
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

    public List<InboundOrderItemDetailRow> getItemList() {
        return itemList;
    }

    public void setItemList(List<InboundOrderItemDetailRow> itemList) {
        this.itemList = itemList == null ? new ArrayList<>() : itemList;
    }

    public String getTotalAmountText() {
        return totalAmount == null ? "-" : totalAmount.stripTrailingZeros().toPlainString();
    }

    public String getSourceTypeText() {
        return "AI".equalsIgnoreCase(sourceType) ? "AI识别生成" : "手工创建";
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
