package com.yocaihua.wms.desktop.module.stock;

public class StockRow {

    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private String unit;
    private String category;
    private Integer quantity;
    private Integer warningQuantity;
    private Integer lowStock;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getWarningQuantity() {
        return warningQuantity;
    }

    public void setWarningQuantity(Integer warningQuantity) {
        this.warningQuantity = warningQuantity;
    }

    public Integer getLowStock() {
        return lowStock;
    }

    public void setLowStock(Integer lowStock) {
        this.lowStock = lowStock;
    }

    public String getLowStockText() {
        return Integer.valueOf(1).equals(lowStock) ? "是" : "否";
    }
}
