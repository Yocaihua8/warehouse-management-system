ALTER TABLE product
    ADD COLUMN IF NOT EXISTS custom_fields_json TEXT NULL COMMENT '自定义字段JSON';
