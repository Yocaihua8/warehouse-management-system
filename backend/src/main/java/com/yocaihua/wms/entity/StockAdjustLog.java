package com.yocaihua.wms.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockAdjustLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称快照
     */
    private String productNameSnapshot;

    /**
     * 调整前库存
     */
    private Integer beforeQuantity;

    /**
     * 调整后库存
     */
    private Integer afterQuantity;

    /**
     * 变动数量
     */
    private Integer changeQuantity;

    /**
     * 变更类型
     */
    private String changeType;

    /**
     * 关联单据ID
     */
    private Long bizOrderId;

    /**
     * 关联单号
     */
    private String bizOrderNo;

    /**
     * 操作人
     */
    private String operatorName;

    /**
     * 调整原因
     */
    private String reason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
