package com.yocaihua.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InboundOrderItem {

    private Long id;

    private Long inboundOrderId;

    private Long productId;

    private String productNameSnapshot;

    private String specificationSnapshot;

    private String unitSnapshot;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
