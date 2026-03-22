package com.yocaihua.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OutboundOrderItem {

    private Long id;

    private Long outboundOrderId;

    private Long productId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}