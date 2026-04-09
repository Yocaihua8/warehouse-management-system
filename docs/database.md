# 数据库设计文档

> 描述当前 `wms` 数据库的所有表结构、字段含义和枚举值定义。
> 对应建表脚本：`sql/wms.sql`

---

## 表清单

| 表名 | 中文名 | 说明 |
|------|--------|------|
| `user` | 用户 | 系统账号，支持 ADMIN / OPERATOR 两种角色 |
| `product` | 商品 | 商品主数据，创建时自动关联一条 `stock` 记录 |
| `customer` | 客户 | 出库单关联的客户主数据 |
| `supplier` | 供应商 | 入库单关联的供应商主数据 |
| `stock` | 库存 | 每个商品一条记录，记录实时库存数量 |
| `stock_adjust_log` | 库存流水 | 所有库存变更的不可删审计日志 |
| `operation_log` | 操作日志 | 用户关键操作日志（登录、确认、作废、AI确认等） |
| `inbound_order` | 入库单 | 入库单据主表 |
| `inbound_order_item` | 入库单明细 | 入库单商品明细，含快照字段 |
| `outbound_order` | 出库单 | 出库单据主表 |
| `outbound_order_item` | 出库单明细 | 出库单商品明细，含快照字段 |
| `ai_recognition_record` | AI 识别主表 | OCR 识别任务记录 |
| `ai_recognition_item` | AI 识别明细 | OCR 识别结果的商品行 |

---

## 枚举值定义

### 用户角色（user.role）

| 值 | 含义 |
|----|------|
| `ADMIN` | 管理员，可执行全部操作 |
| `OPERATOR` | 操作员，只读 + 新建草稿，不可确认/作废 |

### 订单状态（inbound_order.order_status / outbound_order.order_status）

| 值 | 含义 | 可转换到 |
|----|------|---------|
| `1` | 草稿（DRAFT） | 2（已完成）、3（作废） |
| `2` | 已完成（COMPLETED） | 3（作废，同时回滚库存） |
| `3` | 作废（VOID） | 终态，不可再变更 |

### AI 识别状态（ai_recognition_record.recognition_status）

| 值 | 含义 |
|----|------|
| `pending` | 识别中 |
| `success` | 识别成功，等待用户确认 |
| `failed` | 识别失败（OCR 出错或 Python 服务不可用） |
| `confirmed` | 用户已确认，正式单据已生成 |

### AI 识别单据类型（ai_recognition_record.doc_type）

| 值 | 含义 |
|----|------|
| `inbound` | 入库单 |
| `outbound` | 出库单 |

### 库存变更类型（stock_adjust_log.change_type）

| 值 | 触发场景 |
|----|---------|
| `MANUAL_ADJUST` | 管理员手动调整库存 |
| `MANUAL_INBOUND` | 手动创建的入库单确认 |
| `MANUAL_OUTBOUND` | 手动创建的出库单确认 |
| `AI_CONFIRM_INBOUND` | AI 识别确认生成入库单 |
| `AI_CONFIRM_OUTBOUND` | AI 识别确认生成出库单 |

### 操作日志结果（operation_log.result_status）

| 值 | 含义 |
|----|------|
| `SUCCESS` | 操作成功 |
| `FAILED` | 操作失败（预留） |

---

## 表结构详情

### user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `username` | VARCHAR(50) UNIQUE | 用户名，登录凭证 |
| `password` | VARCHAR(100) | BCrypt 哈希密码（启动时自动迁移明文） |
| `nickname` | VARCHAR(50) | 显示名称 |
| `status` | INT | 1=启用，0=禁用 |
| `role` | VARCHAR(20) | ADMIN / OPERATOR |

---

### product（商品表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `product_code` | VARCHAR(50) UNIQUE | 商品编码，业务唯一标识 |
| `product_name` | VARCHAR(100) | 商品名称 |
| `specification` | VARCHAR(100) | 规格 |
| `unit` | VARCHAR(20) | 单位 |
| `category` | VARCHAR(50) | 分类 |
| `sale_price` | DECIMAL(10,2) | 销售单价 |
| `custom_fields_json` | TEXT | 自定义扩展字段（JSON 格式） |
| `remark` | VARCHAR(255) | 备注 |
| `status` | TINYINT | 1=启用，0=停用 |
| `created_time` | DATETIME | 创建时间 |
| `updated_time` | DATETIME | 更新时间 |

> 注：新增商品时自动创建对应 `stock` 记录（初始 quantity=0）。

---

### customer / supplier（客户/供应商表）

两张表结构相同：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `customer_code` / `supplier_code` | VARCHAR(50) UNIQUE | 编码 |
| `customer_name` / `supplier_name` | VARCHAR(100) | 名称 |
| `contact_person` | VARCHAR(50) | 联系人 |
| `phone` | VARCHAR(30) | 电话 |
| `address` | VARCHAR(255) | 地址 |
| `remark` | VARCHAR(255) | 备注 |
| `status` | TINYINT | 1=启用，0=停用 |
| `created_time` / `updated_time` | DATETIME | 时间戳 |

---

### stock（库存表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `product_id` | BIGINT UNIQUE FK | 关联商品，一对一 |
| `quantity` | INT | 当前库存数量 |
| `warning_quantity` | INT DEFAULT 10 | 预警阈值，低于此值标红 |
| `created_time` / `updated_time` | DATETIME | 时间戳 |

> 约束：`product_id` 上有唯一索引，每个商品只能有一条库存记录。

---

### stock_adjust_log（库存流水表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `product_id` | BIGINT | 商品 ID |
| `product_name_snapshot` | VARCHAR(100) | 商品名称快照 |
| `before_quantity` | INT | 变更前库存 |
| `after_quantity` | INT | 变更后库存 |
| `change_quantity` | INT | 变动数量（正=增，负=减） |
| `change_type` | VARCHAR(32) | 见枚举值定义 |
| `biz_order_id` | BIGINT | 关联单据 ID（可空） |
| `biz_order_no` | VARCHAR(50) | 关联单号（可空） |
| `operator_name` | VARCHAR(64) | 操作人 |
| `reason` | VARCHAR(255) | 调整原因 |
| `remark` | VARCHAR(255) | 备注 |
| `created_time` | DATETIME | 创建时间（只有创建，不可修改） |

> 该表只允许插入，不允许修改或删除，作为不可篡改的审计日志。

---

### operation_log（操作日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `action_type` | VARCHAR(64) | 操作类型（如 `LOGIN_SUCCESS` / `INBOUND_CONFIRM`） |
| `module_name` | VARCHAR(64) | 模块名称 |
| `biz_type` | VARCHAR(64) | 业务类型（可空） |
| `biz_id` | BIGINT | 业务主键（可空） |
| `biz_no` | VARCHAR(64) | 业务单号（可空） |
| `operator_name` | VARCHAR(64) | 操作人 |
| `result_status` | VARCHAR(16) | 结果状态（默认 `SUCCESS`） |
| `message` | VARCHAR(255) | 描述信息 |
| `created_time` | DATETIME | 操作时间 |

---

### inbound_order（入库单主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `order_no` | VARCHAR(50) UNIQUE | 单号，系统自动生成 |
| `supplier_id` | BIGINT | 供应商 ID（可空，允许快捷录入时无对应供应商） |
| `supplier_name` | VARCHAR(100) | 供应商名称（非快照，冗余存储） |
| `total_amount` | DECIMAL(12,2) | 总金额（Java 端聚合计算） |
| `order_status` | TINYINT | 1=草稿，2=已入库，3=作废 |
| `remark` | VARCHAR(255) | 备注 |
| `created_time` / `updated_time` | DATETIME | 时间戳 |

---

### inbound_order_item（入库单明细）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `inbound_order_id` | BIGINT FK | 关联入库单 |
| `product_id` | BIGINT | 商品 ID |
| `product_name_snapshot` | VARCHAR(100) | **商品名称快照** |
| `specification_snapshot` | VARCHAR(100) | **规格快照** |
| `unit_snapshot` | VARCHAR(20) | **单位快照** |
| `quantity` | INT | 入库数量 |
| `unit_price` | DECIMAL(10,2) | 单价 |
| `amount` | DECIMAL(12,2) | 金额（quantity × unit_price） |
| `remark` | VARCHAR(255) | 备注 |
| `created_time` / `updated_time` | DATETIME | 时间戳 |

> 快照字段（Snapshot）在创建时写入，**不随商品主数据变更而更新**，保证历史订单数据稳定。

---

### outbound_order / outbound_order_item（出库单）

与入库单结构基本相同，差异：
- 主表关联 `customer_id` / `customer_name_snapshot`（注意出库单主表使用快照字段）
- 明细结构与入库单明细完全一致

---

### ai_recognition_record（AI 识别主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `task_no` | VARCHAR(64) UNIQUE | 识别任务编号，系统生成 |
| `doc_type` | VARCHAR(32) | `inbound` / `outbound` |
| `source_file_name` | VARCHAR(255) | 上传文件名 |
| `recognition_status` | VARCHAR(32) | `pending` / `success` / `failed` / `confirmed` |
| `supplier_name` | VARCHAR(255) | 识别出的往来方名称 |
| `raw_text` | TEXT | OCR 原始文本（调试用） |
| `warnings_json` | TEXT | 识别警告信息 JSON |
| `result_json` | LONGTEXT | 完整识别结果 JSON 备份 |
| `error_message` | VARCHAR(500) | 失败原因（仅 failed 状态有值） |
| `confirmed_order_id` | BIGINT | 用户确认后生成的正式订单 ID |
| `created_by` | VARCHAR(64) | 操作人 |
| `created_time` / `updated_time` | DATETIME | 时间戳 |

---

### ai_recognition_item（AI 识别明细）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 主键 |
| `record_id` | BIGINT FK | 关联识别主表 |
| `line_no` | INT | 行号 |
| `product_name` | VARCHAR(255) | OCR 识别出的商品名称 |
| `specification` | VARCHAR(255) | 规格 |
| `unit` | VARCHAR(64) | 单位 |
| `quantity` | INT | 数量 |
| `unit_price` | DECIMAL(10,2) | 单价 |
| `amount` | DECIMAL(12,2) | 金额 |
| `matched_product_id` | BIGINT | 自动匹配到的系统商品 ID（可空） |
| `match_status` | VARCHAR(32) | 匹配状态（matched / partial / unmatched） |
| `remark` | VARCHAR(255) | 备注 |

---

## 关键关系图（文字）

```
product ──1:1──► stock
product ──1:N──► inbound_order_item (通过 product_id)
product ──1:N──► outbound_order_item (通过 product_id)
product ──1:N──► stock_adjust_log (通过 product_id)

inbound_order ──1:N──► inbound_order_item
outbound_order ──1:N──► outbound_order_item

ai_recognition_record ──1:N──► ai_recognition_item
ai_recognition_record ──0:1──► inbound_order / outbound_order (通过 confirmed_order_id)

supplier ──引用── inbound_order (supplier_id，可空)
customer ──引用── outbound_order (customer_id)
user ──无直接 FK── 所有表（通过 operator_name / created_by 字符串关联）
```
