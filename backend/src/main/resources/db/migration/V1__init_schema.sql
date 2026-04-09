CREATE TABLE IF NOT EXISTS product
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL COMMENT '商品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    specification VARCHAR(100) NULL COMMENT '规格',
    unit VARCHAR(20) NULL COMMENT '单位',
    category VARCHAR(50) NULL COMMENT '分类',
    sale_price DECIMAL(10, 2) DEFAULT 0.00 NULL COMMENT '销售单价',
    custom_fields_json TEXT NULL COMMENT '自定义字段JSON',
    remark VARCHAR(255) NULL COMMENT '备注',
    status TINYINT DEFAULT 1 NOT NULL COMMENT '状态：1启用，0停用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_product_code UNIQUE (product_code)
);

CREATE TABLE IF NOT EXISTS customer
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact_person VARCHAR(50) NULL COMMENT '联系人',
    phone VARCHAR(30) NULL COMMENT '联系电话',
    address VARCHAR(255) NULL COMMENT '地址',
    remark VARCHAR(255) NULL COMMENT '备注',
    status TINYINT DEFAULT 1 NOT NULL COMMENT '状态：1启用，0停用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_customer_code UNIQUE (customer_code)
);

CREATE TABLE IF NOT EXISTS supplier
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

CREATE TABLE IF NOT EXISTS stock
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT DEFAULT 0 NOT NULL COMMENT '当前库存数量',
    warning_quantity INT DEFAULT 10 NOT NULL COMMENT '预警库存',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_stock_product UNIQUE (product_id)
);

CREATE TABLE IF NOT EXISTS stock_adjust_log
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name_snapshot VARCHAR(100) NULL COMMENT '商品名称快照',
    before_quantity INT NOT NULL COMMENT '调整前库存',
    after_quantity INT NOT NULL COMMENT '调整后库存',
    change_quantity INT NOT NULL COMMENT '变动数量',
    change_type VARCHAR(32) DEFAULT 'MANUAL_ADJUST' NOT NULL COMMENT '变更类型',
    biz_order_id BIGINT NULL COMMENT '关联单据ID',
    biz_order_no VARCHAR(50) NULL COMMENT '关联单号',
    operator_name VARCHAR(64) NULL COMMENT '操作人',
    reason VARCHAR(255) NULL COMMENT '调整原因',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间'
);

CREATE TABLE IF NOT EXISTS inbound_order
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL COMMENT '入库单号',
    supplier_id BIGINT NULL COMMENT '供应商ID',
    supplier_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    total_amount DECIMAL(12, 2) DEFAULT 0.00 NOT NULL COMMENT '总金额',
    order_status TINYINT DEFAULT 1 NOT NULL COMMENT '状态：1草稿，2已入库，3作废',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_inbound_order_no UNIQUE (order_no)
);

CREATE TABLE IF NOT EXISTS inbound_order_item
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    inbound_order_id BIGINT NOT NULL COMMENT '入库单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name_snapshot VARCHAR(100) NULL COMMENT '商品名称快照',
    specification_snapshot VARCHAR(100) NULL COMMENT '规格快照',
    unit_snapshot VARCHAR(20) NULL COMMENT '单位快照',
    quantity INT NOT NULL COMMENT '入库数量',
    unit_price DECIMAL(10, 2) DEFAULT 0.00 NOT NULL COMMENT '单价',
    amount DECIMAL(12, 2) DEFAULT 0.00 NOT NULL COMMENT '金额',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

CREATE TABLE IF NOT EXISTS outbound_order
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL COMMENT '出库单号',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    customer_name_snapshot VARCHAR(100) NULL COMMENT '客户名称快照',
    total_amount DECIMAL(12, 2) DEFAULT 0.00 NOT NULL COMMENT '总金额',
    order_status TINYINT DEFAULT 1 NOT NULL COMMENT '状态：1草稿，2已出库，3作废',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_outbound_order_no UNIQUE (order_no)
);

CREATE TABLE IF NOT EXISTS outbound_order_item
(
    id BIGINT AUTO_INCREMENT COMMENT '主键ID'
        PRIMARY KEY,
    outbound_order_id BIGINT NOT NULL COMMENT '出库单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name_snapshot VARCHAR(100) NULL COMMENT '商品名称快照',
    specification_snapshot VARCHAR(100) NULL COMMENT '规格快照',
    unit_snapshot VARCHAR(20) NULL COMMENT '单位快照',
    quantity INT NOT NULL COMMENT '出库数量',
    unit_price DECIMAL(10, 2) DEFAULT 0.00 NOT NULL COMMENT '单价',
    amount DECIMAL(12, 2) DEFAULT 0.00 NOT NULL COMMENT '金额',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

CREATE TABLE IF NOT EXISTS ai_recognition_record
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_no VARCHAR(64) NOT NULL COMMENT '识别任务编号',
    doc_type VARCHAR(32) NOT NULL COMMENT '单据类型：inbound/outbound',
    source_file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    source_file_path VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    recognition_status VARCHAR(32) NOT NULL COMMENT '识别状态：pending/success/failed/confirmed',
    supplier_name VARCHAR(255) DEFAULT NULL COMMENT '识别出的往来单位名称',
    raw_text TEXT COMMENT 'OCR原始文本',
    warnings_json TEXT COMMENT '警告信息JSON',
    result_json LONGTEXT COMMENT '结构化识别结果JSON备份',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    confirmed_order_id BIGINT DEFAULT NULL COMMENT '确认后生成的正式单据ID',
    created_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_status (recognition_status),
    KEY idx_doc_type (doc_type),
    KEY idx_confirmed_order_id (confirmed_order_id)
) COMMENT='AI识别任务主表';

CREATE TABLE IF NOT EXISTS ai_recognition_item
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '识别主表ID',
    line_no INT NOT NULL COMMENT '行号',
    product_name VARCHAR(255) DEFAULT NULL COMMENT '识别出的商品名称',
    specification VARCHAR(255) DEFAULT NULL COMMENT '规格',
    unit VARCHAR(64) DEFAULT NULL COMMENT '单位',
    quantity INT DEFAULT NULL COMMENT '数量',
    unit_price DECIMAL(10, 2) DEFAULT NULL COMMENT '单价',
    amount DECIMAL(12, 2) DEFAULT NULL COMMENT '金额',
    matched_product_id BIGINT DEFAULT NULL COMMENT '匹配到的系统商品ID',
    match_status VARCHAR(32) DEFAULT NULL COMMENT '匹配状态',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_record_id (record_id),
    KEY idx_matched_product_id (matched_product_id)
) COMMENT='AI识别任务明细表';

CREATE TABLE IF NOT EXISTS user
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    status INT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    role VARCHAR(20) NOT NULL DEFAULT 'OPERATOR' COMMENT '角色 ADMIN/OPERATOR'
) COMMENT='用户表';

CREATE TABLE IF NOT EXISTS user_session
(
    token VARCHAR(128) PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    expires_at BIGINT NOT NULL,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO user (username, password, nickname, status, role)
VALUES ('admin', '123456', '管理员', 1, 'ADMIN')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = VALUES(status), role = VALUES(role);

INSERT INTO user (username, password, nickname, status, role)
VALUES ('operator', '123456', '操作员', 1, 'OPERATOR')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = VALUES(status), role = VALUES(role);
