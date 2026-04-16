# 基础资料功能规格

> 覆盖：商品管理、客户管理、供应商管理

---

## 1. 通用规则

- 三个模块的 CRUD 操作均为 Admin 专属（查询和导出无限制）
- 所有主数据使用唯一业务编码（`product_code` / `customer_code` / `supplier_code`），不允许重复
- 软删除逻辑：`status=0`（停用）而非物理删除，但目前部分删除接口为硬删除（见各模块说明）
- 所有列表接口支持按编码、名称模糊筛选 + 分页

---

## 2. 商品管理

### 特有规则
- **新增商品时自动创建 `stock` 记录**（初始 `quantity=0`，`warning_quantity=10`）
- **删除商品的前置校验**：
  - `stock.quantity > 0` → 拒绝删除（有库存不允许删）
  - 商品存在于任意入库/出库单明细中 → 拒绝删除
- `custom_fields_json` 支持自定义扩展字段，存储 JSON 字符串，系统在动态检测列不存在时可自动 DDL（`ProductServiceImpl`）

### 自定义字段（custom_fields_json）

- 存储格式：合法 JSON 字符串，例如 `{"颜色":"红色","产地":"广州"}`
- 写入时校验 JSON 合法性（必须为 JSON 对象，不超过 4000 字符）
- 当前前端实现：原始 JSON textarea，用户需手动输入合法 JSON——仅开发者友好，待改造为键值对 UI（见 BACKLOG P2 #10）

### API
| 接口 | 说明 |
|------|------|
| `GET /product/list` | 分页列表，支持 productCode/productName 筛选 |
| `GET /product/{id}` | 查询详情 |
| `POST /product/add` | 新增（Admin） |
| `PUT /product/update` | 修改（Admin） |
| `DELETE /product/delete/{id}` | 删除（Admin，含前置校验） |
| `GET /product/export` | 导出 Excel |

---

## 3. 客户管理

### 特有规则
- 客户与出库单关联（`outbound_order.customer_id`）
- 删除时做双重关联校验：`countByCustomerId` + `countByCustomerNameWhenCustomerIdMissing`，命中即拒绝删除
- `status=0`（停用）由后端强制拦截，停用客户不能用于新建/编辑出库单

### API
| 接口 | 说明 |
|------|------|
| `GET /customer/list` | 分页列表，支持 customerCode/customerName 筛选 |
| `GET /customer/{id}` | 查询详情 |
| `POST /customer/add` | 新增（Admin） |
| `PUT /customer/update` | 修改（Admin） |
| `DELETE /customer/delete/{id}` | 删除（Admin） |
| `GET /customer/export` | 导出 Excel |

---

## 4. 供应商管理

### 特有规则
- 供应商与入库单关联（`inbound_order.supplier_id`，可空）
- 创建入库单时 `supplier_id` 可为空（支持临时供应商场景），`supplier_name` 直接存储
- 删除时做双重关联校验：`countBySupplierId` + `countBySupplierNameWhenSupplierIdMissing`，命中即拒绝删除
- `status=0`（停用）由后端强制拦截，停用供应商不能用于新建/编辑入库单

### API
| 接口 | 说明 |
|------|------|
| `GET /supplier/list` | 分页列表，支持 supplierCode/supplierName 筛选 |
| `GET /supplier/{id}` | 查询详情 |
| `POST /supplier/add` | 新增（Admin） |
| `PUT /supplier/update` | 修改（Admin） |
| `DELETE /supplier/delete/{id}` | 删除（Admin） |
| `GET /supplier/export` | 导出 Excel |

---

## 5. 已知问题

| 问题 | 影响 |
|------|------|
| 商品列表无 status 筛选参数 | 前端无法按启用/停用过滤商品（需确认是否已实现） |
