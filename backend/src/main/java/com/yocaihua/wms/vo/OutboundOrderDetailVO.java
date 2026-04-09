package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OutboundOrderDetailVO {

    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private BigDecimal totalAmount;

    private Integer orderStatus;

    private String remark;

    private LocalDateTime createdTime;

    private List<OutboundOrderItemVO> itemList;
}