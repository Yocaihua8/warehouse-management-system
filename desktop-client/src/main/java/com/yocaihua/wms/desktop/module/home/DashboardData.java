package com.yocaihua.wms.desktop.module.home;

public class DashboardData {

    private Long productCount;
    private Long customerCount;
    private Long stockProductCount;
    private Long lowStockCount;
    private Long inboundOrderCount;
    private Long outboundOrderCount;

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public Long getCustomerCount() {
        return customerCount;
    }

    public void setCustomerCount(Long customerCount) {
        this.customerCount = customerCount;
    }

    public Long getStockProductCount() {
        return stockProductCount;
    }

    public void setStockProductCount(Long stockProductCount) {
        this.stockProductCount = stockProductCount;
    }

    public Long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(Long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public Long getInboundOrderCount() {
        return inboundOrderCount;
    }

    public void setInboundOrderCount(Long inboundOrderCount) {
        this.inboundOrderCount = inboundOrderCount;
    }

    public Long getOutboundOrderCount() {
        return outboundOrderCount;
    }

    public void setOutboundOrderCount(Long outboundOrderCount) {
        this.outboundOrderCount = outboundOrderCount;
    }
}
