CREATE TABLE ai_recognition_item (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                     record_id BIGINT NOT NULL COMMENT '识别主表ID',
                                     line_no INT NOT NULL COMMENT '行号',
                                     product_name VARCHAR(255) DEFAULT NULL COMMENT '识别出的商品名称',
                                     specification VARCHAR(255) DEFAULT NULL COMMENT '规格',
                                     unit VARCHAR(64) DEFAULT NULL COMMENT '单位',
                                     quantity INT DEFAULT NULL COMMENT '数量',
                                     unit_price DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
                                     amount DECIMAL(12,2) DEFAULT NULL COMMENT '金额',
                                     matched_product_id BIGINT DEFAULT NULL COMMENT '匹配到的系统商品ID',
                                     match_status VARCHAR(32) DEFAULT NULL COMMENT '匹配状态：matched/unmatched/manual_fixed',
                                     remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
                                     created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     KEY idx_record_id (record_id),
                                     KEY idx_matched_product_id (matched_product_id)
) COMMENT='AI识别任务明细表';