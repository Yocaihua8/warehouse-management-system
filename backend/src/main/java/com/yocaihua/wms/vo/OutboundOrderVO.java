package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OutboundOrderVO {

    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private BigDecimal totalAmount;

    private Integer orderStatus;

    private String remark;

    private LocalDateTime createdTime;
}