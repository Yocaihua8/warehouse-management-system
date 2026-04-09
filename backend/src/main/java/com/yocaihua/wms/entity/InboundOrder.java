package com.yocaihua.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InboundOrder {

    private Long id;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private BigDecimal totalAmount;

    private Integer orderStatus;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
