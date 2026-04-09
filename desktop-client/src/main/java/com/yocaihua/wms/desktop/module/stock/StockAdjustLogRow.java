package com.yocaihua.wms.desktop.module.stock;

public class StockAdjustLogRow {

    private Long id;
    private String productCode;
    private String productName;
    private String changeType;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private Integer changeQuantity;
    private Long bizOrderId;
    private String bizOrderNo;
    private String operatorName;
    private String reason;
    private String remark;
    private String createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Integer getBeforeQuantity() {
        return beforeQuantity;
    }

    public void setBeforeQuantity(Integer beforeQuantity) {
        this.beforeQuantity = beforeQuantity;
    }

    public Integer getAfterQuantity() {
        return afterQuantity;
    }

    public void setAfterQuantity(Integer afterQuantity) {
        this.afterQuantity = afterQuantity;
    }

    public Integer getChangeQuantity() {
        return changeQuantity;
    }

    public void setChangeQuantity(Integer changeQuantity) {
        this.changeQuantity = changeQuantity;
    }

    public Long getBizOrderId() {
        return bizOrderId;
    }

    public void setBizOrderId(Long bizOrderId) {
        this.bizOrderId = bizOrderId;
    }

    public String getBizOrderNo() {
        return bizOrderNo;
    }

    public void setBizOrderNo(String bizOrderNo) {
        this.bizOrderNo = bizOrderNo;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public String getChangeTypeText() {
        if ("MANUAL_INBOUND".equals(changeType)) {
            return "手工入库";
        }
        if ("AI_CONFIRM_INBOUND".equals(changeType)) {
            return "AI确认入库";
        }
        if ("MANUAL_OUTBOUND".equals(changeType)) {
            return "手工出库";
        }
        if ("AI_CONFIRM_OUTBOUND".equals(changeType)) {
            return "AI确认出库";
        }
        if ("VOID_INBOUND".equals(changeType)) {
            return "作废入库回退";
        }
        if ("VOID_OUTBOUND".equals(changeType)) {
            return "作废出库回补";
        }
        if ("MANUAL_ADJUST".equals(changeType)) {
            return "手工调整";
        }
        return changeType == null || changeType.trim().isEmpty() ? "-" : changeType.trim();
    }

    public String getRemarkText() {
        if (remark != null && !remark.trim().isEmpty()) {
            return remark.trim();
        }
        if (reason != null && !reason.trim().isEmpty()) {
            return reason.trim();
        }
        return "-";
    }

    public boolean canViewBizOrderDetail() {
        return bizOrderId != null && (isInboundType(changeType) || isOutboundType(changeType));
    }

    public boolean isInboundOrderType() {
        return isInboundType(changeType);
    }

    public boolean isOutboundOrderType() {
        return isOutboundType(changeType);
    }

    private boolean isInboundType(String value) {
        return "MANUAL_INBOUND".equals(value)
                || "AI_CONFIRM_INBOUND".equals(value)
                || "VOID_INBOUND".equals(value);
    }

    private boolean isOutboundType(String value) {
        return "MANUAL_OUTBOUND".equals(value)
                || "AI_CONFIRM_OUTBOUND".equals(value)
                || "VOID_OUTBOUND".equals(value);
    }
}
