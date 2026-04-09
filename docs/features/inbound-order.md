# 入库单功能规格

---

## 1. 业务概述

入库单用于记录商品从供应商入库到仓库的过程。支持手动录入和 AI 识别两种来源。

所有入库单必须经过「草稿 → 确认」流程，**只有确认后才会更新库存**。

---

## 2. 状态机

```
                  [新建]
                    │
                    ▼
              ┌─────────┐
              │  草稿    │ status=1
              │ (DRAFT)  │
              └────┬────┘
                   │                      ┌──────────────────────────────────┐
         ┌─────────┴──────────┐           │ 只有 ADMIN 可执行「确认」和「作废」  │
         │（Admin）确认入库     │           └──────────────────────────────────┘
         ▼                    │（Admin）作废
   ┌──────────────┐           ▼
   │  已入库       │     ┌─────────┐
   │ (COMPLETED)  │     │  作废    │ status=3（终态）
   │   status=2   │     │ (VOID)   │
   └──────┬───────┘     └─────────┘
          │（Admin）作废
          ▼
     ┌─────────┐
     │  作废    │ status=3（终态）+ 库存回滚
     │ (VOID)   │
     └─────────┘
```

### 状态转换规则

| 当前状态 | 目标状态 | 操作 | 权限 | 库存影响 |
|---------|---------|------|------|---------|
| DRAFT(1) | COMPLETED(2) | 确认入库 | Admin | **增加库存** + 写审计日志 |
| DRAFT(1) | VOID(3) | 作废 | Admin | 无 |
| COMPLETED(2) | VOID(3) | 作废 | Admin | **回滚库存**（减回来）+ 写审计日志 |
| VOID(3) | — | 任何操作 | — | 终态，不可变更 |

---

## 3. 关键业务规则

### 3.1 创建草稿
- 商品必须存在（会校验 `product_id`）
- 商品的 `stock` 记录必须存在
- 同一张单据内不允许重复的商品（`product_id` 不能重复）
- `totalAmount` 在 Java 端计算（`sum(quantity × unitPrice)`），不从前端接收
- `orderNo` 由系统自动生成（推测：按规则拼接时间戳/序号）
- 创建成功返回 `{ orderId, orderNo }`

### 3.2 编辑草稿
- **只有 DRAFT 状态**可以编辑，COMPLETED 和 VOID 不可编辑
- 编辑采用「删除原有明细 → 重新插入」策略（非 diff 更新），有事务保护
- 编辑后重新计算 `totalAmount`

### 3.3 确认入库（ADMIN）
- **只有 DRAFT 状态**可以确认
- 使用乐观状态锁：`UPDATE ... WHERE status = 1`，并发确认时只有第一次成功
- 确认成功后：
  1. 逐条调用 `StockFlowService.increaseByInbound()`
  2. 每条商品：`stock.quantity += item.quantity`
  3. 写入 `stock_adjust_log`（changeType=MANUAL_INBOUND）
  4. 更新 `order_status = 2`

### 3.4 作废（ADMIN）
- DRAFT 和 COMPLETED 都可以作废
- **DRAFT 作废**：直接更新状态为 VOID，无库存影响
- **COMPLETED 作废**：
  1. 逐条反向调整库存：`stock.quantity -= item.quantity`
  2. 写入 `stock_adjust_log`（changeType=MANUAL_INBOUND，change_quantity 为负值）
  3. 更新 `order_status = 3`
- 作废时须提供 `voidReason`（必填）

### 3.5 数据快照
明细中的商品名称、规格、单位在**创建时**写入快照字段，后续即使商品主数据修改，历史单据的展示数据不变。

---

## 4. 来源类型（source_type）

| 值 | 说明 |
|----|------|
| `MANUAL` | 手动录入 |
| `AI` | AI 识别后确认生成 |

> 注意：AI 来源的入库单在 `ai_recognition_record.confirmed_order_id` 字段有关联。

---

## 5. API 接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/inbound-order/add` | POST | 已登录 | 新建草稿 |
| `/inbound-order/{id}` | PUT | 已登录 | 编辑草稿 |
| `/inbound-order/{id}/confirm` | POST | **Admin** | 确认入库 |
| `/inbound-order/{id}/void` | POST | **Admin** | 作废（需传 voidReason） |
| `/inbound-order/list` | GET | 已登录 | 分页列表（支持 orderNo/sourceType/orderStatus 筛选） |
| `/inbound-order/detail/{id}` | GET | 已登录 | 详情（含明细） |
| `/inbound-order/{id}` | GET | 已登录 | 简要详情（打印/预览用） |
| `/inbound-order/{id}/export/excel` | GET | 已登录 | 导出 Excel |
| `/inbound-order/{id}/export/pdf` | GET | 已登录 | 导出 PDF |

详细请求/响应结构见 Swagger：`http://localhost:8080/swagger-ui.html`

---

## 6. 关联 DTO / VO

| 类名 | 用途 |
|------|------|
| `InboundOrderAddDTO` | 新建/编辑入库单的请求体，含 `List<InboundOrderItemAddDTO>` |
| `InboundOrderVO` | 列表展示 VO |
| `InboundOrderDetailVO` | 详情 VO（含明细列表） |
| `InboundDetailVO` | 简要详情 VO（打印场景） |
| `OrderCreatedVO` | 创建成功返回 `{ orderId, orderNo }` |

---

## 7. 已知问题与注意事项

1. **库存更新非原子批量操作**：确认时逐条 UPDATE stock，高并发场景可能有超卖风险（当前场景为单机低并发，可接受）
2. **编辑是全量替换**：草稿编辑采用 delete-then-insert，事务内操作，如果中途异常会回滚，但要确认事务配置正确
3. **source_type 字段存储位置**：AI 来源的订单通过 `ai_recognition_record.doc_type` 区分，但 `inbound_order` 表本身可能无 source_type 字段（需核实），查询时需要 JOIN 确认
