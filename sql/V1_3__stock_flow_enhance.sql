ALTER TABLE stock_adjust_log
    ADD COLUMN product_name_snapshot VARCHAR(100) NULL COMMENT '商品名称快照' AFTER product_id,
    ADD COLUMN change_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL_ADJUST' COMMENT '变更类型' AFTER change_quantity,
    ADD COLUMN biz_order_id BIGINT NULL COMMENT '关联单据ID' AFTER change_type,
    ADD COLUMN biz_order_no VARCHAR(50) NULL COMMENT '关联单号' AFTER biz_order_id,
    ADD COLUMN operator_name VARCHAR(64) NULL COMMENT '操作人' AFTER biz_order_no,
    ADD COLUMN remark VARCHAR(255) NULL COMMENT '备注' AFTER reason;
