package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutboundOrderItemVO {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private String specification;

    private String unit;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String remark;
}