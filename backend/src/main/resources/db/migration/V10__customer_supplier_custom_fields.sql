SET @customer_custom_fields_json_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'customer'
      AND COLUMN_NAME = 'custom_fields_json'
);

SET @customer_custom_fields_json_sql = IF(
    @customer_custom_fields_json_exists = 0,
    'ALTER TABLE customer ADD COLUMN custom_fields_json TEXT NULL COMMENT ''自定义字段JSON''',
    'SELECT 1'
);

PREPARE stmt_customer_custom_fields_json FROM @customer_custom_fields_json_sql;
EXECUTE stmt_customer_custom_fields_json;
DEALLOCATE PREPARE stmt_customer_custom_fields_json;

SET @supplier_custom_fields_json_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'supplier'
      AND COLUMN_NAME = 'custom_fields_json'
);

SET @supplier_custom_fields_json_sql = IF(
    @supplier_custom_fields_json_exists = 0,
    'ALTER TABLE supplier ADD COLUMN custom_fields_json TEXT NULL COMMENT ''自定义字段JSON''',
    'SELECT 1'
);

PREPARE stmt_supplier_custom_fields_json FROM @supplier_custom_fields_json_sql;
EXECUTE stmt_supplier_custom_fields_json;
DEALLOCATE PREPARE stmt_supplier_custom_fields_json;
