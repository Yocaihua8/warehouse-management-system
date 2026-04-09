package com.yocaihua.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OutboundOrder {

    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerNameSnapshot;

    private BigDecimal totalAmount;

    private Integer orderStatus;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
