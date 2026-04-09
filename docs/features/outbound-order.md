# 出库单功能规格

---

## 1. 业务概述

出库单用于记录商品从仓库发出给客户的过程。与入库单的核心区别：

- **出库单在创建草稿时即校验库存是否充足**，而不是等到确认时才报错
- 确认出库后扣减库存，作废已完成单据时回滚库存

---

## 2. 状态机

```
                  [新建]（创建时即检查库存）
                    │
                    ▼
              ┌─────────┐
              │  草稿    │ status=1
              │ (DRAFT)  │
              └────┬────┘
                   │                      ┌──────────────────────────────────┐
         ┌─────────┴──────────┐           │ 只有 ADMIN 可执行「确认」和「作废」  │
         │（Admin）确认出库     │           └──────────────────────────────────┘
         ▼                    │（Admin）作废
   ┌──────────────┐           ▼
   │  已出库       │     ┌─────────┐
   │ (COMPLETED)  │     │  作废    │ status=3（终态）
   │   status=2   │     │ (VOID)   │
   └──────┬───────┘     └─────────┘
          │（Admin）作废
          ▼
     ┌─────────┐
     │  作废    │ status=3（终态）+ 库存回滚（加回来）
     │ (VOID)   │
     └─────────┘
```

---

## 3. 与入库单的关键差异

| 对比项 | 入库单 | 出库单 |
|--------|--------|--------|
| 库存校验时机 | 确认时无需校验（是增加库存） | **创建草稿时**即检查库存充足性 |
| 确认效果 | 增加库存 | 扣减库存 |
| 作废回滚 | 减回库存（只对 COMPLETED） | 加回库存（只对 COMPLETED） |
| 关联主数据 | supplier_id（可空） | customer_id（必填） |
| 快照字段 | 明细有商品快照 | 主表有 customer_name_snapshot，明细有商品快照 |

---

## 4. 关键业务规则

### 4.1 创建草稿（库存预检验）
- 遍历每条明细，检查 `stock.quantity >= item.quantity`
- 任意一条商品库存不足，整个创建请求拒绝（抛 `BusinessException`）
- 注意：**创建时不锁定库存**，仅做一次快照式检验。如果创建后、确认前有其他出库单先被确认，可能导致确认时库存已不足（见 4.3）

### 4.2 编辑草稿
- 同入库单，仅 DRAFT 状态可编辑
- 编辑时**重新检查库存**（基于最新库存数量）
- 全量替换明细（delete-then-insert）

### 4.3 确认出库（ADMIN）
- 仅 DRAFT 可确认，使用乐观状态锁
- 确认时**再次检查库存充足性**（因草稿期间库存可能已变化）
- 确认成功后：
  1. 调用 `StockFlowService.decreaseByOutbound()`
  2. 每条商品：`stock.quantity -= item.quantity`
  3. 写入 `stock_adjust_log`（changeType=MANUAL_OUTBOUND）
  4. 更新 `order_status = 2`

### 4.4 作废（ADMIN）
- DRAFT 和 COMPLETED 都可以作废
- **DRAFT 作废**：直接更新状态，无库存影响
- **COMPLETED 作废**：
  1. 逐条回滚：`stock.quantity += item.quantity`（加回）
  2. 写入 `stock_adjust_log`
  3. 更新 `order_status = 3`
- 作废时须提供 `voidReason`（必填）

---

## 5. API 接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/outbound-order/add` | POST | 已登录 | 新建草稿（含库存校验） |
| `/outbound-order/{id}` | PUT | 已登录 | 编辑草稿（重新校验库存） |
| `/outbound-order/{id}/confirm` | POST | **Admin** | 确认出库（再次校验库存） |
| `/outbound-order/{id}/void` | POST | **Admin** | 作废（需传 voidReason） |
| `/outbound-order/list` | GET | 已登录 | 分页列表（支持 orderNo/orderStatus 筛选） |
| `/outbound-order/{id}` | GET | 已登录 | 详情（含明细） |
| `/outbound-order/{id}/export/excel` | GET | 已登录 | 导出 Excel |
| `/outbound-order/{id}/export/pdf` | GET | 已登录 | 导出 PDF |

---

## 6. 已知问题与注意事项

1. **库存预检验不锁定**：草稿创建到确认之间库存可能被其他操作消耗。库存不足只在确认时报错，前端应对此情况给出明确提示
2. **并发确认风险**：同一出库单并发确认，乐观锁保证只有一次成功，但多张单据同时确认同一商品时，可能出现超扣。当前场景为单机低并发，可接受
