SET @product_custom_fields_json_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'product'
      AND COLUMN_NAME = 'custom_fields_json'
);

SET @product_custom_fields_json_sql = IF(
    @product_custom_fields_json_exists = 0,
    'ALTER TABLE product ADD COLUMN custom_fields_json TEXT NULL COMMENT ''自定义字段JSON''',
    'SELECT 1'
);

PREPARE stmt_product_custom_fields_json FROM @product_custom_fields_json_sql;
EXECUTE stmt_product_custom_fields_json;
DEALLOCATE PREPARE stmt_product_custom_fields_json;
