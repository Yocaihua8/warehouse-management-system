package com.yocaihua.wms.vo;

import lombok.Data;

@Data
public class DashboardVO {

    /**
     * 商品总数
     */
    private Long productCount;

    /**
     * 客户总数
     */
    private Long customerCount;

    /**
     * 库存商品数
     */
    private Long stockProductCount;

    /**
     * 低库存商品数
     */
    private Long lowStockCount;

    /**
     * 入库单总数
     */
    private Long inboundOrderCount;

    /**
     * 出库单总数
     */
    private Long outboundOrderCount;
}