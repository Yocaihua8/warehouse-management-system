SET @stock_adjust_log_product_name_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_adjust_log'
      AND COLUMN_NAME = 'product_name_snapshot'
);

SET @stock_adjust_log_product_name_snapshot_sql = IF(
    @stock_adjust_log_product_name_snapshot_exists = 0,
    'ALTER TABLE stock_adjust_log ADD COLUMN product_name_snapshot VARCHAR(100) NULL COMMENT ''商品名称快照'' AFTER product_id',
    'SELECT 1'
);

PREPARE stmt_stock_adjust_log_product_name_snapshot FROM @stock_adjust_log_product_name_snapshot_sql;
EXECUTE stmt_stock_adjust_log_product_name_snapshot;
DEALLOCATE PREPARE stmt_stock_adjust_log_product_name_snapshot;

SET @stock_adjust_log_change_type_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_adjust_log'
      AND COLUMN_NAME = 'change_type'
);

SET @stock_adjust_log_change_type_sql = IF(
    @stock_adjust_log_change_type_exists = 0,
    'ALTER TABLE stock_adjust_log ADD COLUMN change_type VARCHAR(32) NOT NULL DEFAULT ''MANUAL_ADJUST'' COMMENT ''变更类型'' AFTER change_quantity',
    'SELECT 1'
);

PREPARE stmt_stock_adjust_log_change_type FROM @stock_adjust_log_change_type_sql;
EXECUTE stmt_stock_adjust_log_change_type;
DEALLOCATE PREPARE stmt_stock_adjust_log_change_type;

SET @stock_adjust_log_biz_order_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_adjust_log'
      AND COLUMN_NAME = 'biz_order_id'
);

SET @stock_adjust_log_biz_order_id_sql = IF(
    @stock_adjust_log_biz_order_id_exists = 0,
    'ALTER TABLE stock_adjust_log ADD COLUMN biz_order_id BIGINT NULL COMMENT ''关联单据ID'' AFTER change_type',
    'SELECT 1'
);

PREPARE stmt_stock_adjust_log_biz_order_id FROM @stock_adjust_log_biz_order_id_sql;
EXECUTE stmt_stock_adjust_log_biz_order_id;
DEALLOCATE PREPARE stmt_stock_adjust_log_biz_order_id;

SET @stock_adjust_log_biz_order_no_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_adjust_log'
      AND COLUMN_NAME = 'biz_order_no'
);

SET @stock_adjust_log_biz_order_no_sql = IF(
    @stock_adjust_log_biz_order_no_exists = 0,
    'ALTER TABLE stock_adjust_log ADD COLUMN biz_order_no VARCHAR(50) NULL COMMENT ''关联单号'' AFTER biz_order_id',
    'SELECT 1'
);

PREPARE stmt_stock_adjust_log_biz_order_no FROM @stock_adjust_log_biz_order_no_sql;
EXECUTE stmt_stock_adjust_log_biz_order_no;
DEALLOCATE PREPARE stmt_stock_adjust_log_biz_order_no;

SET @stock_adjust_log_operator_name_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_adjust_log'
      AND COLUMN_NAME = 'operator_name'
);

SET @stock_adjust_log_operator_name_sql = IF(
    @stock_adjust_log_operator_name_exists = 0,
    'ALTER TABLE stock_adjust_log ADD COLUMN operator_name VARCHAR(64) NULL COMMENT ''操作人'' AFTER biz_order_no',
    'SELECT 1'
);

PREPARE stmt_stock_adjust_log_operator_name FROM @stock_adjust_log_operator_name_sql;
EXECUTE stmt_stock_adjust_log_operator_name;
DEALLOCATE PREPARE stmt_stock_adjust_log_operator_name;

SET @stock_adjust_log_remark_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_adjust_log'
      AND COLUMN_NAME = 'remark'
);

SET @stock_adjust_log_remark_sql = IF(
    @stock_adjust_log_remark_exists = 0,
    'ALTER TABLE stock_adjust_log ADD COLUMN remark VARCHAR(255) NULL COMMENT ''备注'' AFTER reason',
    'SELECT 1'
);

PREPARE stmt_stock_adjust_log_remark FROM @stock_adjust_log_remark_sql;
EXECUTE stmt_stock_adjust_log_remark;
DEALLOCATE PREPARE stmt_stock_adjust_log_remark;
