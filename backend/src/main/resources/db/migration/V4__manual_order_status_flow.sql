-- 历史出库单在旧逻辑下“保存即扣库存”，order_status=1 实际表示已出库。
-- 升级到“草稿 -> 确认出库”流程后，把历史记录回填为已出库，避免重复扣减库存。
UPDATE outbound_order
SET order_status = 2
WHERE order_status = 1;
