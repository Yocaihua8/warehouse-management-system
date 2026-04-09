CREATE TABLE supplier
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    supplier_code VARCHAR(50) NOT NULL COMMENT '供应商编码',
    supplier_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) NULL COMMENT '联系人',
    phone VARCHAR(30) NULL COMMENT '联系电话',
    address VARCHAR(255) NULL COMMENT '地址',
    remark VARCHAR(255) NULL COMMENT '备注',
    status TINYINT DEFAULT 1 NOT NULL COMMENT '状态：1启用，0停用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_supplier_code UNIQUE (supplier_code)
);

ALTER TABLE inbound_order
    ADD COLUMN supplier_id BIGINT NULL COMMENT '供应商ID' AFTER order_no;
