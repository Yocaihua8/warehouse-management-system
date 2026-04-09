package com.yocaihua.wms.vo;

import lombok.Data;

@Data
public class StockVO {

    private Long productId;

    private String productCode;

    private String productName;

    private String specification;

    private String unit;

    private String category;

    private Integer quantity;

    private Integer warningQuantity;

    private Integer lowStock;
}