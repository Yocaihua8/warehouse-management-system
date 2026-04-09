package com.yocaihua.wms.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockAdjustLogVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 变更类型
     */
    private String changeType;

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
