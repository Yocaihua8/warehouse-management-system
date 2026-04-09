CREATE TABLE IF NOT EXISTS operation_log
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    action_type   VARCHAR(64)  NOT NULL COMMENT '操作类型',
    module_name   VARCHAR(64)  NOT NULL COMMENT '模块名称',
    biz_type      VARCHAR(64)  NULL COMMENT '业务类型',
    biz_id        BIGINT       NULL COMMENT '业务ID',
    biz_no        VARCHAR(64)  NULL COMMENT '业务单号',
    operator_name VARCHAR(64)  NULL COMMENT '操作人',
    result_status VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT '结果状态：SUCCESS/FAILED',
    message       VARCHAR(255) NULL COMMENT '描述信息',
    created_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_action_type (action_type),
    KEY idx_operator_name (operator_name),
    KEY idx_created_time (created_time),
    KEY idx_biz (biz_type, biz_id)
) COMMENT='用户关键操作日志';

