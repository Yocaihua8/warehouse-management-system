ALTER TABLE inbound_order_item
    ADD COLUMN product_name_snapshot VARCHAR(100) NULL COMMENT '商品名称快照' AFTER product_id,
    ADD COLUMN specification_snapshot VARCHAR(100) NULL COMMENT '规格快照' AFTER product_name_snapshot,
    ADD COLUMN unit_snapshot VARCHAR(20) NULL COMMENT '单位快照' AFTER specification_snapshot;

ALTER TABLE outbound_order_item
    ADD COLUMN product_name_snapshot VARCHAR(100) NULL COMMENT '商品名称快照' AFTER product_id,
    ADD COLUMN specification_snapshot VARCHAR(100) NULL COMMENT '规格快照' AFTER product_name_snapshot,
    ADD COLUMN unit_snapshot VARCHAR(20) NULL COMMENT '单位快照' AFTER specification_snapshot;

ALTER TABLE outbound_order
    ADD COLUMN customer_name_snapshot VARCHAR(100) NULL COMMENT '客户名称快照' AFTER customer_id;
