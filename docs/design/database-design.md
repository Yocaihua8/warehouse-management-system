# 数据库设计

> 状态：Active  
> Owner：Yocaihua8  
> Last Updated：2026-04-18  
> Scope：`wms` 业务数据库  
> Related：`../database.md`、`./system-design-overview.md`

## 1. 设计目标

当前数据库设计用于支撑：

- 主数据（用户、商品、客户、供应商）
- 单据主表与明细表
- 库存与审计日志
- AI 识别草稿与确认结果

设计重点是：

- 单据状态流转可追踪
- 库存变化可审计
- 历史快照稳定
- 结构变更统一经 Flyway 管理

## 2. 表清单

| 表名 | 用途 | 主键 | 说明 |
|------|------|------|------|
| `user` | 用户账号 | `id` | 支持 `ADMIN` / `OPERATOR` |
| `product` | 商品主数据 | `id` | 新增时自动创建库存记录 |
| `customer` | 客户主数据 | `id` | 出库业务关联 |
| `supplier` | 供应商主数据 | `id` | 入库业务关联 |
| `stock` | 实时库存 | `id` | 每个商品一条库存记录 |
| `stock_adjust_log` | 库存流水 | `id` | 不可删审计日志 |
| `operation_log` | 操作日志 | `id` | 关键操作留痕 |
| `inbound_order` / `inbound_order_item` | 入库单主表 / 明细 | `id` | 含商品快照 |
| `outbound_order` / `outbound_order_item` | 出库单主表 / 明细 | `id` | 含商品快照 |
| `ai_recognition_record` / `ai_recognition_item` | AI 识别主表 / 明细 | `id` | 识别草稿与确认记录 |

## 3. 结构摘要

### 3.1 主数据

- `product`、`customer`、`supplier` 均包含 `status`
- `product` 包含 `custom_fields_json` 扩展字段
- `stock` 与 `product` 为一对一关系

### 3.2 单据

- 入库 / 出库单采用主表 + 明细表
- 明细保存商品名称、规格、单位快照
- 订单状态采用 `1=草稿 / 2=已完成 / 3=作废`

### 3.3 AI 识别

- AI 识别结果先保存为草稿记录
- 用户确认后关联正式订单 ID

## 4. 实体关系

### 4.1 关系说明

- `product` 与 `stock`：一对一
- `inbound_order` 与 `inbound_order_item`：一对多
- `outbound_order` 与 `outbound_order_item`：一对多
- `ai_recognition_record` 与 `ai_recognition_item`：一对多

### 4.2 ER 图（文字版）

```text
[product] 1 --- 1 [stock]
[inbound_order] 1 --- n [inbound_order_item]
[outbound_order] 1 --- n [outbound_order_item]
[ai_recognition_record] 1 --- n [ai_recognition_item]
```

## 5. 关键约束

- 唯一约束：
  - 商品、客户、供应商编码唯一
  - 订单号、任务号唯一
- 外键 / 关联约束：
  - 明细表关联主表
  - 库存表按商品一对一
- 状态字段约束：
  - 单据状态流转固定
  - AI 识别状态固定
- 软删除策略：
  - 当前主数据以状态停用 / 删除前校验为主，不做统一软删除框架

## 6. 迁移规范

- 所有结构变更通过 Flyway 脚本完成
- 禁止直接修改已执行的历史迁移
- 迁移文件命名规则：`V{number}__description.sql`

## 7. 现有详细文档

当前字段级说明、枚举值和完整结构详情仍维护在：

- [../database.md](../database.md)

本文件的作用是对齐规范目录，并提供规范化入口；字段级权威说明仍以当前详细数据库文档和实际迁移脚本为准。
