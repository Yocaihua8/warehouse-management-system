# ADR-004: StockFlowService 作为库存变更唯一入口

## 状态
已采纳（2025-06）

## 背景

库存变更场景较多：入库确认、出库确认、入库作废回滚、出库作废回滚、手动调整。早期代码中各场景直接操作 `stock` 表，导致审计日志写入逻辑散落各处，审计一致性难以保证。

## 决策

所有库存变更必须经过 **`StockFlowService`**，禁止在其他 Service 或 Mapper 中直接 UPDATE `stock` 表。

`StockFlowService` 职责：
1. 更新 `stock.quantity`（增加或减少）
2. 写入 `stock_adjust_log`（变更前/后数量、变更量、变更类型、操作人、关联业务单号）

变更类型枚举（`change_type`）：
- `MANUAL_INBOUND`：手工入库确认
- `MANUAL_OUTBOUND`：手工出库确认
- `AI_CONFIRM_INBOUND`：AI 入库确认
- `AI_CONFIRM_OUTBOUND`：AI 出库确认
- `MANUAL_ADJUST`：管理员手动调整

## 后果

**正面影响：**
- 库存变更有统一的审计入口，`stock_adjust_log` 记录完整不遗漏
- 新增库存变更场景时，只需调用 `StockFlowService`，不需要在新场景中重复写审计逻辑
- 代码审查时，凡涉及库存变更的 PR，只需重点关注 `StockFlowService` 调用链
- `stock_adjust_log` 流水记录不可修改、不可删除，为审计提供不可篡改的历史

**负面影响 / 权衡：**
- 所有涉及库存的操作都依赖 `StockFlowService`，该类成为热点，需保证其稳定性
- 团队成员需了解并遵守"禁止直接操作 `stock` 表"的约定

## 备选方案

**直接在各 Service 中操作**：灵活但审计散乱，已验证不可行（早期遗留代码即如此）。

**数据库触发器写审计日志**：不依赖应用代码，但调试困难，且无法获取应用层上下文（如操作人、业务单号），不采用。
