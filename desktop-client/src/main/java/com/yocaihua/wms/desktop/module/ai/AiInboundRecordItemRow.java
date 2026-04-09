package com.yocaihua.wms.desktop.module.ai;

import java.math.BigDecimal;

public class AiInboundRecordItemRow {

    private Integer lineNo;
    private String productName;
    private String specification;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Long matchedProductId;
    private String matchStatus;
    private String remark;

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getMatchedProductId() {
        return matchedProductId;
    }

    public void setMatchedProductId(Long matchedProductId) {
        this.matchedProductId = matchedProductId;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getUnitPriceText() {
        return unitPrice == null ? "-" : unitPrice.stripTrailingZeros().toPlainString();
    }

    public String getAmountText() {
        return amount == null ? "-" : amount.stripTrailingZeros().toPlainString();
    }

    public String getMatchedProductIdText() {
        return matchedProductId == null ? "-" : String.valueOf(matchedProductId);
    }

    public String getMatchStatusText() {
        if ("matched_exact".equalsIgnoreCase(matchStatus)) {
            return "精确匹配";
        }
        if ("matched_fuzzy".equalsIgnoreCase(matchStatus)) {
            return "模糊匹配";
        }
        if ("manual_selected".equalsIgnoreCase(matchStatus)) {
            return "人工选择";
        }
        if ("manual_created".equalsIgnoreCase(matchStatus)) {
            return "新建后回填";
        }
        if ("unmatched".equalsIgnoreCase(matchStatus)) {
            return "未匹配";
        }
        return matchStatus == null || matchStatus.trim().isEmpty() ? "-" : matchStatus.trim();
    }
}
