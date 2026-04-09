package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InboundItemVO {
    private Long productId;
    private String productName;
    private String specification;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal amount;
}