SET @inbound_order_item_product_name_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inbound_order_item'
      AND COLUMN_NAME = 'product_name_snapshot'
);

SET @inbound_order_item_product_name_snapshot_sql = IF(
    @inbound_order_item_product_name_snapshot_exists = 0,
    'ALTER TABLE inbound_order_item ADD COLUMN product_name_snapshot VARCHAR(100) NULL COMMENT ''商品名称快照'' AFTER product_id',
    'SELECT 1'
);

PREPARE stmt_inbound_order_item_product_name_snapshot FROM @inbound_order_item_product_name_snapshot_sql;
EXECUTE stmt_inbound_order_item_product_name_snapshot;
DEALLOCATE PREPARE stmt_inbound_order_item_product_name_snapshot;

SET @inbound_order_item_specification_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inbound_order_item'
      AND COLUMN_NAME = 'specification_snapshot'
);

SET @inbound_order_item_specification_snapshot_sql = IF(
    @inbound_order_item_specification_snapshot_exists = 0,
    'ALTER TABLE inbound_order_item ADD COLUMN specification_snapshot VARCHAR(100) NULL COMMENT ''规格快照'' AFTER product_name_snapshot',
    'SELECT 1'
);

PREPARE stmt_inbound_order_item_specification_snapshot FROM @inbound_order_item_specification_snapshot_sql;
EXECUTE stmt_inbound_order_item_specification_snapshot;
DEALLOCATE PREPARE stmt_inbound_order_item_specification_snapshot;

SET @inbound_order_item_unit_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inbound_order_item'
      AND COLUMN_NAME = 'unit_snapshot'
);

SET @inbound_order_item_unit_snapshot_sql = IF(
    @inbound_order_item_unit_snapshot_exists = 0,
    'ALTER TABLE inbound_order_item ADD COLUMN unit_snapshot VARCHAR(20) NULL COMMENT ''单位快照'' AFTER specification_snapshot',
    'SELECT 1'
);

PREPARE stmt_inbound_order_item_unit_snapshot FROM @inbound_order_item_unit_snapshot_sql;
EXECUTE stmt_inbound_order_item_unit_snapshot;
DEALLOCATE PREPARE stmt_inbound_order_item_unit_snapshot;

SET @outbound_order_item_product_name_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'outbound_order_item'
      AND COLUMN_NAME = 'product_name_snapshot'
);

SET @outbound_order_item_product_name_snapshot_sql = IF(
    @outbound_order_item_product_name_snapshot_exists = 0,
    'ALTER TABLE outbound_order_item ADD COLUMN product_name_snapshot VARCHAR(100) NULL COMMENT ''商品名称快照'' AFTER product_id',
    'SELECT 1'
);

PREPARE stmt_outbound_order_item_product_name_snapshot FROM @outbound_order_item_product_name_snapshot_sql;
EXECUTE stmt_outbound_order_item_product_name_snapshot;
DEALLOCATE PREPARE stmt_outbound_order_item_product_name_snapshot;

SET @outbound_order_item_specification_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'outbound_order_item'
      AND COLUMN_NAME = 'specification_snapshot'
);

SET @outbound_order_item_specification_snapshot_sql = IF(
    @outbound_order_item_specification_snapshot_exists = 0,
    'ALTER TABLE outbound_order_item ADD COLUMN specification_snapshot VARCHAR(100) NULL COMMENT ''规格快照'' AFTER product_name_snapshot',
    'SELECT 1'
);

PREPARE stmt_outbound_order_item_specification_snapshot FROM @outbound_order_item_specification_snapshot_sql;
EXECUTE stmt_outbound_order_item_specification_snapshot;
DEALLOCATE PREPARE stmt_outbound_order_item_specification_snapshot;

SET @outbound_order_item_unit_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'outbound_order_item'
      AND COLUMN_NAME = 'unit_snapshot'
);

SET @outbound_order_item_unit_snapshot_sql = IF(
    @outbound_order_item_unit_snapshot_exists = 0,
    'ALTER TABLE outbound_order_item ADD COLUMN unit_snapshot VARCHAR(20) NULL COMMENT ''单位快照'' AFTER specification_snapshot',
    'SELECT 1'
);

PREPARE stmt_outbound_order_item_unit_snapshot FROM @outbound_order_item_unit_snapshot_sql;
EXECUTE stmt_outbound_order_item_unit_snapshot;
DEALLOCATE PREPARE stmt_outbound_order_item_unit_snapshot;

SET @outbound_order_customer_name_snapshot_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'outbound_order'
      AND COLUMN_NAME = 'customer_name_snapshot'
);

SET @outbound_order_customer_name_snapshot_sql = IF(
    @outbound_order_customer_name_snapshot_exists = 0,
    'ALTER TABLE outbound_order ADD COLUMN customer_name_snapshot VARCHAR(100) NULL COMMENT ''客户名称快照'' AFTER customer_id',
    'SELECT 1'
);

PREPARE stmt_outbound_order_customer_name_snapshot FROM @outbound_order_customer_name_snapshot_sql;
EXECUTE stmt_outbound_order_customer_name_snapshot;
DEALLOCATE PREPARE stmt_outbound_order_customer_name_snapshot;
