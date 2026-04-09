# 库存管理功能规格

---

## 1. 业务概述

库存模块负责维护每个商品的实时库存数量，并记录每一次变更的完整历史（库存流水）。

**核心原则：所有库存变更必须经过 `StockFlowService`，不允许直接 UPDATE stock 表。**

---

## 2. 库存记录（stock 表）

- 每个商品对应**唯一一条** stock 记录（`product_id` 唯一约束）
- 商品创建时**自动创建** stock 记录（初始 quantity=0）
- 不允许手动新增或删除 stock 记录
- 商品删除时：若 `quantity > 0`，拒绝删除

### 预警库存（warning_quantity）
- 默认值：10
- 当 `quantity <= warning_quantity` 时，前端列表标红显示
- 管理员可通过手动调整接口修改 `warning_quantity`

---

## 3. 库存变更场景与规则

### 3.1 入库确认（quantity 增加）
- 触发：`InboundOrderServiceImpl.confirmInboundOrder()`
- 流向：`StockFlowService.increaseByInbound()`
- change_type：`MANUAL_INBOUND` 或 `AI_CONFIRM_INBOUND`

### 3.2 出库确认（quantity 减少）
- 触发：`OutboundOrderServiceImpl.confirmOutboundOrder()`
- 流向：`StockFlowService.decreaseByOutbound()`
- change_type：`MANUAL_OUTBOUND` 或 `AI_CONFIRM_OUTBOUND`
- 前置校验：`quantity >= 出库数量`，不足则报错

### 3.3 订单作废回滚
- 入库单作废（已完成）→ 逐条减少库存（change_quantity 为负）
- 出库单作废（已完成）→ 逐条增加库存（change_quantity 为正）
- 回滚操作与正向操作使用相同的 StockFlowService，保持审计完整性

### 3.4 手动调整（Admin）
- 接口：`PUT /stock/update`
- 管理员可直接设置商品的库存数量
- 每次调整写入 `stock_adjust_log`（changeType=MANUAL_ADJUST）
- 同时可修改 `warning_quantity` 预警阈值

---

## 4. 库存流水（stock_adjust_log）

所有库存变更都产生一条流水记录，字段包含：
- 变更前/后数量（`before_quantity` / `after_quantity`）
- 变动数量（`change_quantity`，正=增加，负=减少）
- 变更类型（`change_type`）
- 关联业务单号（`biz_order_id` / `biz_order_no`）
- 操作人（`operator_name`）

**流水记录不可修改、不可删除，是不可篡改的审计日志。**

---

## 5. API 接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/stock/list` | GET | 已登录 | 库存分页列表（支持商品编码/名称筛选） |
| `/stock/export` | GET | 已登录 | 导出库存（format=excel/csv） |
| `/stock/update` | PUT | **Admin** | 手动调整库存数量及预警值 |
| `/stock/log/list` | GET | 已登录 | 库存流水分页列表 |

---

## 6. 导出格式

`/stock/export?format=excel`（默认）→ 返回 `.xlsx`

`/stock/export?format=csv` → 返回 `.csv`（UTF-8 BOM 编码）
