# 前端单据页面架构规格

> 本文档描述入库单、出库单创建页与打印视图的**前端架构设计**，包括：重构前历史现状、已完成重构内容、后续改进方向、关键代码设计。  
> 后端业务规则（状态机、库存联动、权限）见 [inbound-order.md](./inbound-order.md) / [outbound-order.md](./outbound-order.md)。  
> **当前状态（2026-04）**：composables 层、通用组件层、打印架构（阶段1-4）已完成重构，CreateView 已从千行级降至 ~160 行。

---

## 第一步：重构前历史现状（存档）

> 本节记录重构前的原始问题，作为历史对照。重构后的实际状态见下方"当前实现状态"。

### 1.1 重构前文件规模

| 文件 | 重构前行数 | 重构前职责（过载） |
|------|---------|------|
| `views/inbound/InboundCreateView.vue` | 1564 | 手工入库 + AI 识别对话框 + 快速新建供应商弹窗 + 快速新建商品弹窗 + 草稿加载 + 校验 + 提交 |
| `views/outbound/OutboundCreateView.vue` | 1231 | 与入库页结构几乎相同，差异仅为交易对手字段类型 |
| `views/inbound/InboundPrintView.vue` | 355 | 独立路由打印页，含 `toChineseAmount()`（~90行）+ `buildPrintItems()` 重复代码 |
| `views/outbound/OutboundPrintView.vue` | 315 | 与入库打印页逐字复制，差异仅为标题和字段名 |

### 1.2 重构前主要耦合问题

| 问题 | 严重度 |
|------|--------|
| CreateView 巨型化（1564/1231 行），无法独立测试 | 高 |
| 入库/出库 80%+ 代码重复，同一 Bug 需改两处 | 高 |
| AI 识别对话框嵌入 CreateView，共享组件状态 | 高 |
| 无 composables 层，所有逻辑内联在 .vue 文件 | 高 |
| 无页面状态模型（create/edit/view 未区分） | 中 |
| 无汇总区：合计数量/金额未展示 | 中 |
| 打印工具函数重复，无数据适配层 | 中 |
| Pinia 已安装但未用于共享状态 | 低 |

---

## 当前实现状态（2026-04）

> 阶段1–4 已完成重构，以下为实际代码现状。

### 文件规模对比

| 文件 | 重构前 | 重构后（实际） | 变化 |
|------|--------|-------------|------|
| `InboundCreateView.vue` | 1564 行 | **158 行** | ↓ 90% |
| `OutboundCreateView.vue` | 1231 行 | **160 行** | ↓ 87% |
| `InboundPrintView.vue` | 355 行 | **73 行** | ↓ 79% |
| `OutboundPrintView.vue` | 315 行 | **63 行** | ↓ 80% |

### 已建立的 Composables 层（`frontend/src/composables/`）

| 文件 | 行数 | 职责 |
|------|------|------|
| `useOrderCalc.js` | 41 | 行金额、合计数量、合计金额（纯函数） |
| `useOrderForm.js` | 82 | 表单状态、页面模式、API 映射 |
| `useOrderItems.js` | 96 | 明细行增/删/插入、商品联动 |
| `useOrderValidation.js` | 98 | 三层校验（字段→行→单据） |
| `useProductSearch.js` | 125 | 商品远程搜索 + 选项缓存 |
| `useAiRecognition.js` | 165 | AI 上传、识别、草稿状态、确认 |
| `useQuickCreate.js` | 43 | 快速新建供应商/客户/商品弹窗逻辑 |
| `useInboundCreatePage.js` | 329 | 入库创建页聚合 composable（组合以上各层） |
| `useOutboundCreatePage.js` | 349 | 出库创建页聚合 composable |

### 已建立的组件层

**`frontend/src/components/order/`**：
- `OrderItemTable.vue` — 可编辑明细表格（props/emit 解耦）
- `OrderSummary.vue` — 汇总区域组件
- `AiRecognitionDialog.vue` — 入库 AI 识别对话框
- `AiOutboundRecognitionDialog.vue` — 出库 AI 识别对话框（独立组件）
- `QuickCreateDialog.vue` — 通用快速新建弹窗
- `OrderDetailItemTable.vue` — 详情页只读表格

**`frontend/src/components/print/`**：
- `PrintTemplate.vue` — 通用打印模板（纯渲染，不调 API）

### 已建立的工具层（`frontend/src/utils/`）

| 文件 | 行数 | 职责 |
|------|------|------|
| `orderHelper.js` | 61 | createEmptyOrderItem / PAGE_MODE / ORDER_STATUS / today() 等常量与工具函数 |
| `printUtils.js` | 104 | toChineseAmount / padPrintItems（已从两个 PrintView 合并为唯一来源） |
| `printAdapter.js` | 77 | buildInboundPrintData / buildOutboundPrintData → PrintData |
| `printService.js` | 9 | triggerBrowserPrint / openPrintPreview（PDF 导出预留接口） |

### Pinia Store（`frontend/src/stores/`）
- `auth.js` — 已接入，统一管理 token / username / nickname / role

### 待完善（阶段5）

| 项目 | 说明 |
|------|------|
| 预置空行交互 | 明细表格初始化预置 8 行空行，录入体验向 ERP 靠拢 |
| 合计行嵌入表格 | 合计行作为表格最后一行，与数据列对齐 |
| 操作区完整按钮 | 补齐"保存并新建"、"清空"、"预览"及快捷键标注 |
| 出库库存余量展示 | 明细行显示当前库存数，防止超量录入 |
| `ProductFormView.vue` / `CustomerFormView.vue` | 当前为空文件，待确认用途或删除 |

---

## 第二步：重构方案设计

### 2.1 页面结构：ERP 单据工作台

页面统一拆成四个区域，布局参考传统进销存软件"高效录单"风格（见 §UI参考分析）：

```
┌─────────────────────────────────────────────────────────────┐
│  A. 单据头区域                                                │
│  左：[供应商/客户]  [备注]        右：单据编号 XSD2024001001  │
│                                       单据日期 2024-01-01    │
├─────────────────────────────────────────────────────────────┤
│  B. 明细表格区域                                              │
│  ┌──┬──────────────┬──┬────┬──────┬──────┬──────┬───┬──┐   │
│  │行号│商品编码/名称  │规格│单位│ 数量 │ 单价 │ 金额 │备注│操作│   │
│  ├──┼──────────────┼──┼────┼──────┼──────┼──────┼───┼──┤   │
│  │ 1 │（预置空行）   │   │    │      │      │      │   │   │   │
│  │ 2 │              │   │    │      │      │      │   │   │   │
│  │...│  默认 8 行    │   │    │      │      │      │   │   │   │
│  │ 8 │              │   │    │      │      │      │   │   │   │
│  ├──┴──────────────┴──┴────┼──────┴──────┼──────┤───┴──┤   │
│  │    合 计                 │     0       │¥0.00 │      │   │
│  └─────────────────────────┴─────────────┴──────┴──────┘   │
├─────────────────────────────────────────────────────────────┤
│  C. 汇总区域（表格底部合计行 + 独立汇总条）                    │
│    合计数量：0    合计金额：¥0.00                              │
├─────────────────────────────────────────────────────────────┤
│  D. 操作区（左辅右主）                                        │
│  [查看历史] [智能识别导入]   [保存草稿(S)] [保存并新建] [清空] │
│                              [打印(P)]   [预览]              │
└─────────────────────────────────────────────────────────────┘
```

**单据头字段规划（与后端对齐）**：

| 字段 | 入库 | 出库 | 只读 | 位置 | 说明 |
|------|------|------|:----:|------|------|
| orderNo | ✓ | ✓ | ✓ | 右上角大字 | 系统生成，草稿阶段显示"待生成" |
| orderDate | ✓ | ✓ | - | 右上角 | 默认今日，可编辑 |
| supplierName | ✓ | - | - | 左侧主区 | 文本输入，支持快速新建 |
| customerId | - | ✓ | - | 左侧主区 | 下拉选择，支持快速新建 |
| remark | ✓ | ✓ | - | 左侧次区 | 备注/说明 |
| sourceType | ✓ | ✓ | ✓ | 自动带入 | MANUAL/AI，不需要用户手填 |

**操作区按钮规则**（左辅助 / 右主操作）：

| 分组 | 按钮 | create | edit(草稿) | readonly(已确认/作废) |
|------|------|:------:|:----------:|:--------------------:|
| 左侧辅助 | 查看历史单据 | ✓ | ✓ | ✓ |
| 左侧辅助 | 智能识别导入 | ✓ | ✓ | ✗ |
| 右侧主操作 | 保存草稿 (S) | ✓ | ✓ | ✗ |
| 右侧主操作 | 保存并新建 | ✓ | ✓ | ✗ |
| 右侧主操作 | 打印 (P) | ✗ | ✓ | ✓ |
| 右侧主操作 | 预览 | ✗ | ✓ | ✓ |
| 右侧主操作 | 清空 (C) | ✓ | ✓ | ✗ |

### 2.2 单据逻辑层拆分方案

新建 `frontend/src/composables/` 目录，按职责拆分七个 composable：

| Composable | 职责 | 入库/出库共用 |
|-----------|------|:---:|
| `useOrderForm.js` | 页面状态机、单据头表单、草稿加载/保存/提交、API 映射 | ✓ |
| `useOrderItems.js` | 明细行增/删/插入、商品选择联动、行校验 | ✓ |
| `useOrderCalc.js` | 行金额、合计数量、合计金额（纯函数） | ✓ |
| `useOrderValidation.js` | 三层校验：字段→行→单据 | ✓ |
| `useProductSearch.js` | 商品远程搜索 + 本地选项缓存 | ✓ |
| `useAiRecognition.js` | AI 上传、识别、草稿状态、确认，与 CreateView 解耦 | ✓ |
| `useQuickCreate.js` | 快速新建供应商/客户/商品弹窗逻辑 | ✓ |

### 2.3 页面状态模型与单据状态模型

两者必须分开维护，不能混用：

```js
// 页面状态（决定 UI 可编辑性）
PAGE_MODE = {
  CREATE:   'create',   // 新建中，全部可编辑
  EDIT:     'edit',     // 编辑草稿，全部可编辑
  VIEW:     'view',     // 查看已保存单据，只读
  CONFIRM:  'confirm',  // 等待管理员确认，只读
  READONLY: 'readonly'  // 已确认/已作废，完全只读
}

// 单据状态（决定业务流转，与后端 orderStatus 对齐）
ORDER_STATUS = {
  DRAFT:     1,  // 草稿
  COMPLETED: 2,  // 已确认（入库/出库完成）
  VOID:      3   // 已作废
}
```

映射规则：`orderStatus=1` → `PAGE_MODE.EDIT`；`orderStatus=2|3` → `PAGE_MODE.READONLY`

### 2.4 主从数据模型

```js
// 统一数据模型（form/items/summary 三层）
const state = {
  form: {                    // 单据头
    orderNo: '',
    orderDate: today(),
    supplierName: '',        // 入库
    customerId: null,        // 出库
    remark: '',
    sourceType: 'MANUAL'
  },
  items: [OrderItem],        // 明细行数组
  summary: {                 // 汇总（computed）
    totalQuantity: 0,
    totalAmount: '0.00'
  }
}
```

### 2.5 行级交互事件抽象

| 事件 | 触发时机 | 处理逻辑 |
|------|---------|---------|
| `onHeaderFieldChange(field, value)` | 单据头字段变更 | 更新 form，标记脏状态 |
| `onRowFieldChange(index, field, value)` | 明细行字段变更 | 更新行，触发金额重算，刷新汇总 |
| `onProductSelected(index, productId)` | 商品选中 | 回填 productCode/productName/specification/unit/unitPrice，重算金额 |
| `onRowAdd()` | 点击新增行 | push 空行 |
| `onRowInsert(afterIndex)` | 在某行后插入 | splice 插入空行 |
| `onRowDelete(index)` | 删除某行 | splice 删除，至少保留 1 行 |
| `onSaveDraft()` | 保存草稿 | 校验后调 saveInboundOrder 或 updateInboundOrderDraft |
| `onSaveAndNew()` | 保存并新建 | 保存成功后 resetForm() |
| `onSubmitOrder()` | 提交确认 | 校验 + confirm API |
| `onLoadOrderDetail(id)` | 加载已有单据 | 拉取详情，判断 orderStatus 设置 pageMode |
| `onImportFromAI(aiDraft)` | AI 识别完成后导入 | 将 aiDraft 映射到 form + items |

### 2.6 校验逻辑三层设计

```
字段级校验（validateField）
  └─ 商品不能为空
  └─ 数量必须 > 0
  └─ 单价不能 < 0

行级校验（validateRow）
  └─ 同一商品不允许重复录入（警告，不阻断）
  └─ 出库：quantity <= 库存余量

单据级校验（validateOrder）
  └─ 至少有一条有效明细
  └─ 单据头必填字段不为空
  └─ AI 导入：所有行 matchedProductId 必须已设置（未匹配项不允许提交）
```

### 2.7 明细表交互抽象方案（`OrderItemTable.vue`）

**Props**：
- `items` — 明细行数组
- `editable` — 是否可编辑
- `orderType` — 入库/出库（控制是否显示库存余量列）
- `productOptions` — 商品选项
- `productLoading` — 搜索加载状态

**Events**：
- `@row-add`
- `@row-insert(afterIndex)`
- `@row-delete(index)`
- `@row-field-change(index, field, value)`
- `@product-selected(index, productId)`
- `@product-search(keyword)`

### 2.8 打印架构拆分方案（四层）

```
业务数据（API 返回 detail 对象）
    ↓
打印数据适配器（utils/printAdapter.js）
    buildInboundPrintData(detail) → PrintData
    buildOutboundPrintData(detail) → PrintData
    ↓
打印模板组件（components/print/PrintTemplate.vue）
    纯渲染，不调 API，不含业务逻辑
    ↓
打印服务层（utils/printService.js）
    openPrintPreview(id, type)  → 新窗口预览
    triggerPrint(id, type)      → 直接打印
    exportPdf(id, type)         → 预留 PDF 导出接口
```

**PrintData 统一数据结构**：
```js
{
  title: string,                              // 单据标题
  headerFields: [{ label, value, span }],     // 单据头信息行
  items: [{                                   // 明细行（含空行）
    index, productName, specification,
    unit, quantity, unitPrice, amount,
    remark, isEmpty
  }],
  totalAmount: string,
  totalAmountChinese: string
}
```

### 2.9 AI 识别确认单兼容方案

**核心原则**：AI 识别产生的是"待确认草稿"，不是直接单据。

**解耦方案**：

```
AiRecognitionDialog.vue（独立组件）
    ↓ 用户确认草稿后
    emit('confirmed', { form, items })
    ↓
InboundCreateView 接收
    onImportFromAI(aiDraft) → 映射到 form + items
    用户可继续编辑
    ↓
手动点击"保存草稿" / "提交确认"
```

**AI 草稿 → 标准 items 映射规则**：
- `aiItem.matchedProductId` → `item.productId`（已匹配时）
- `aiItem.productName` → `item.productName`（未匹配时保留原始名称，标记警告）
- `aiItem.quantity / unitPrice / amount` → 直接映射
- 未匹配商品：`item.productId = null`，行高亮警告，阻止提交直到处理完

**AI 对话框中支持**：
- 逐行修改识别结果
- 行内搜索匹配商品（`handleAiMatchedProductChange`）
- 快速新建商品（`openQuickCreateProduct`）
- 快速新建供应商/客户（`openQuickCreateSupplier`）

---

## 第三步：分阶段改造计划

| 阶段 | 目标 | 主要产出 | 风险 |
|------|------|---------|------|
| **1** | 抽逻辑层，不改页面 | `utils/orderHelper.js`、`utils/printUtils.js`、`composables/useOrderCalc.js`、`useOrderValidation.js`、`useProductSearch.js` | 极低 |
| **2** | 重构入库页 | `composables/useOrderForm.js`、`useOrderItems.js`、`useAiRecognition.js`、`useQuickCreate.js`；`components/order/OrderItemTable.vue`、`AiRecognitionDialog.vue`、`QuickCreateDialog.vue`；重写 `InboundCreateView.vue` (~200行) | 中 |
| **3** | 重构出库页 | 复用阶段2全部产出，重写 `OutboundCreateView.vue` (~150行) | 低 |
| **4** | 独立打印模板 | `utils/printAdapter.js`、`utils/printService.js`、`components/print/PrintTemplate.vue`；重构 `InboundPrintView.vue` + `OutboundPrintView.vue` | 低 |
| **5** | 补充增强 | `OrderSummary.vue` 汇总组件；操作区完整按钮（保存并新建/预览/清空）；草稿离开提示；出库库存余量展示 | 低 |

全程保持**路由路径不变**，不修改后端 API，不影响已有功能。

---

## 第四步：文件落地修改点

### 新增文件

```
frontend/src/
  composables/
    useOrderForm.js          — 页面状态机 + 单据头表单 + 草稿加载/保存/提交
    useOrderItems.js         — 明细行增/删/插入 + 商品联动
    useOrderCalc.js          — 纯函数：行金额 + 合计数量 + 合计金额
    useOrderValidation.js    — 三层校验
    useProductSearch.js      — 商品远程搜索
    useAiRecognition.js      — AI 流程（上传/识别/草稿/确认）
    useQuickCreate.js        — 快速新建弹窗逻辑
  components/
    order/
      OrderItemTable.vue     — 可编辑明细表格（props/emit 解耦）
      AiRecognitionDialog.vue — AI 识别对话框（emit confirmed 事件）
      QuickCreateDialog.vue  — 通用快速新建弹窗
      OrderSummary.vue       — 汇总区域组件
    print/
      PrintTemplate.vue      — 通用打印模板（纯渲染）
  utils/
    orderHelper.js           — createEmptyItem / parsePageData / displayText / PAGE_MODE / ORDER_STATUS 等常量
    printUtils.js            — toChineseAmount / padPrintItems（从两个 PrintView 合并）
    printAdapter.js          — buildInboundPrintData / buildOutboundPrintData
    printService.js          — openPrintPreview / triggerPrint / exportPdf（预留）
```

### 修改文件

| 文件 | 改动方向 | 改造阶段 |
|------|---------|---------|
| `views/inbound/InboundCreateView.vue` | 从 1564 行重构为 ~200 行，使用 composables + 组件 | 2 |
| `views/outbound/OutboundCreateView.vue` | 从 1231 行重构为 ~150 行 | 3 |
| `views/inbound/InboundPrintView.vue` | 移除重复函数，使用 printAdapter + PrintTemplate | 4 |
| `views/outbound/OutboundPrintView.vue` | 同上 | 4 |

### 不修改的文件

- `api/inbound.js` / `api/outbound.js` / `api/ai.js` — API 层已足够清晰
- `utils/request.js` / `utils/auth.js` — 基础设施
- `views/inbound/InboundDetailView.vue` 等详情页 — 阶段 5 以后再考虑
- 所有路由配置 / 后端代码

---

## 第五步：关键代码设计

### 5.1 页面常量与数据模型（`utils/orderHelper.js`）

```js
export const ORDER_TYPE   = { INBOUND: 'inbound', OUTBOUND: 'outbound' }
export const PAGE_MODE    = { CREATE: 'create', EDIT: 'edit', VIEW: 'view', CONFIRM: 'confirm', READONLY: 'readonly' }
export const ORDER_STATUS = { DRAFT: 1, COMPLETED: 2, VOID: 3 }

export function createEmptyItem() {
  return {
    productId: null, productCode: '', productName: '',
    specification: '', unit: '',
    quantity: 1, unitPrice: 0, remark: ''
  }
}

export function today() {
  return new Date().toISOString().slice(0, 10)
}

export function parsePageData(payload) {
  if (Array.isArray(payload)) return { list: payload, total: payload.length }
  return {
    list: Array.isArray(payload?.list) ? payload.list : [],
    total: typeof payload?.total === 'number' ? payload.total : 0
  }
}

export function displayText(value) {
  if (value === null || value === undefined) return '-'
  return String(value).trim() || '-'
}
```

### 5.2 单据逻辑层核心接口（`composables/useOrderForm.js`）

```js
export function useOrderForm(orderType) {
  // 返回：
  // form, items, summary         — 三层数据模型
  // pageMode, currentDraftId     — 状态
  // pageTitle                    — 动态标题
  // loadDraft(draftId)           — 草稿加载
  // handleSaveDraft()            — 保存草稿
  // handleSaveAndNew()           — 保存并新建
  // handleSubmit(validateFn)     — 提交
  // resetForm()                  — 清空
  // onImportFromAI(aiDraft)      — AI 导入
}
```

### 5.3 行交互处理（`composables/useOrderItems.js`）

```js
export function useOrderItems(items) {
  const addRow = () => items.push(createEmptyItem())

  const insertRow = (afterIndex) =>
    items.splice(afterIndex + 1, 0, createEmptyItem())

  const removeRow = (index) => {
    if (items.length <= 1) { ElMessage.warning('至少保留一条明细'); return }
    items.splice(index, 1)
  }

  const onProductSelected = (index, productId, productOptions) => {
    const p = productOptions.find(p => Number(p.id) === Number(productId))
    const row = items[index]
    if (p) {
      row.productCode = p.productCode || ''
      row.productName = p.productName || ''
      row.specification = p.specification || ''
      row.unit = p.unit || ''
      row.unitPrice = Number(p.salePrice || 0)
    } else {
      Object.assign(row, { productCode: '', productName: '', specification: '', unit: '' })
    }
  }

  return { addRow, insertRow, removeRow, onProductSelected }
}
```

### 5.4 计算函数（`composables/useOrderCalc.js`）

```js
export function useOrderCalc(items) {
  const calcRowAmount = (row) =>
    (Number(row.quantity || 0) * Number(row.unitPrice || 0)).toFixed(2)

  const totalQuantity = computed(() =>
    items.reduce((s, r) => s + Number(r.quantity || 0), 0))

  const totalAmount = computed(() =>
    items.reduce((s, r) =>
      s + Number(r.quantity || 0) * Number(r.unitPrice || 0), 0
    ).toFixed(2))

  return { calcRowAmount, totalQuantity, totalAmount }
}
```

### 5.5 打印数据适配器（`utils/printAdapter.js`）

```js
import { padPrintItems, toChineseAmount } from './printUtils'

export function buildInboundPrintData(detail) {
  return {
    title: '采购入库单',
    headerFields: [
      { label: '入库仓库', value: '总仓库', span: 6 },
      { label: '单据编号', value: detail.orderNo || '-', span: 5 },
      { label: '供货单位', value: detail.supplierName || '-', span: 6 },
      { label: '录单日期', value: detail.createdTime || '-', span: 5 },
      { label: '来源类型', value: detail.sourceType === 'AI' ? 'AI识别' : '手工录入', span: 11 },
      { label: '备注', value: detail.remark || '-', span: 11 }
    ],
    items: padPrintItems(detail.itemList),
    totalAmount: detail.totalAmount ?? '-',
    totalAmountChinese: toChineseAmount(detail.totalAmount)
  }
}

export function buildOutboundPrintData(detail) {
  return {
    title: '销售出库单',
    headerFields: [
      { label: '出库仓库', value: '总仓库', span: 6 },
      { label: '单据编号', value: detail.orderNo || '-', span: 5 },
      { label: '客户名称', value: detail.customerName || '-', span: 6 },
      { label: '录单日期', value: detail.createdTime || '-', span: 5 },
      { label: '备注', value: detail.remark || '-', span: 11 }
    ],
    items: padPrintItems(detail.itemList),
    totalAmount: detail.totalAmount ?? '-',
    totalAmountChinese: toChineseAmount(detail.totalAmount)
  }
}
```

### 5.6 打印服务层（`utils/printService.js`）

```js
export const PrintType = { INBOUND: 'inbound', OUTBOUND: 'outbound' }

export function openPrintPreview(id, type) {
  window.open(`/${type}/print/${id}`, '_blank')
}

export function triggerPrint() {
  window.print()
}

// 预留 PDF 导出扩展点（阶段5实现）
export function exportPdf(id, type) {
  // TODO: 集成 html2pdf 或后端 Jasper 导出
  console.warn('PDF 导出尚未实现', id, type)
}
```

### 5.7 重构后 InboundCreateView.vue 骨架

```vue
<template>
  <div class="order-workbench">
    <el-card shadow="never">
      <!-- A. 单据头 -->
      <template #header>
        <span class="order-title">{{ pageTitle }}</span>
      </template>
      <OrderHeader
        :form="form"
        :editable="pageMode !== PAGE_MODE.READONLY"
        :order-type="ORDER_TYPE.INBOUND"
        @field-change="onHeaderFieldChange"
      />

      <!-- B. 明细表 -->
      <OrderItemTable
        :items="items"
        :editable="pageMode !== PAGE_MODE.READONLY"
        :product-options="productOptions"
        :product-loading="productLoading"
        :order-type="ORDER_TYPE.INBOUND"
        @row-add="addRow"
        @row-insert="insertRow"
        @row-delete="removeRow"
        @row-field-change="onRowFieldChange"
        @product-selected="onProductSelected"
        @product-search="handleProductSearch"
      />

      <!-- C. 汇总区 -->
      <OrderSummary :total-quantity="totalQuantity" :total-amount="totalAmount" />

      <!-- D. 操作区 -->
      <div class="order-actions">
        <el-button @click="resetForm" :disabled="pageMode === PAGE_MODE.READONLY">清空</el-button>
        <el-button type="success" :loading="submitting" @click="handleSaveDraft"
          :disabled="pageMode === PAGE_MODE.READONLY">
          {{ currentDraftId ? '更新草稿' : '保存草稿' }}
        </el-button>
        <el-button @click="handleSaveAndNew" :disabled="pageMode === PAGE_MODE.READONLY">
          保存并新建
        </el-button>
        <el-button type="primary" plain @click="aiVisible = true"
          :disabled="pageMode === PAGE_MODE.READONLY">
          智能识别导入
        </el-button>
        <el-button type="primary" @click="onPrint" :disabled="!currentDraftId">打印</el-button>
      </div>
    </el-card>

    <AiRecognitionDialog
      v-model:visible="aiVisible"
      order-type="inbound"
      :product-options="productOptions"
      @confirmed="onImportFromAI"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import OrderItemTable from '../../components/order/OrderItemTable.vue'
import OrderSummary from '../../components/order/OrderSummary.vue'
import AiRecognitionDialog from '../../components/order/AiRecognitionDialog.vue'
import { useOrderForm } from '../../composables/useOrderForm'
import { useOrderItems } from '../../composables/useOrderItems'
import { useOrderCalc } from '../../composables/useOrderCalc'
import { useOrderValidation } from '../../composables/useOrderValidation'
import { useProductSearch } from '../../composables/useProductSearch'
import { openPrintPreview } from '../../utils/printService'
import { ORDER_TYPE, PAGE_MODE } from '../../utils/orderHelper'

const route = useRoute()
const aiVisible = ref(false)

const { form, items, pageMode, pageTitle, currentDraftId, submitting,
        loadDraft, handleSaveDraft, handleSaveAndNew, resetForm, onImportFromAI
      } = useOrderForm(ORDER_TYPE.INBOUND)

const { addRow, insertRow, removeRow, onProductSelected, onRowFieldChange } = useOrderItems(items)
const { totalQuantity, totalAmount } = useOrderCalc(items)
const { validateForm } = useOrderValidation(form, items, ORDER_TYPE.INBOUND)
const { productOptions, productLoading, handleProductSearch } = useProductSearch()

const onHeaderFieldChange = (field, value) => { form[field] = value }
const onPrint = () => openPrintPreview(currentDraftId.value, 'inbound')

onMounted(() => {
  const draftId = route.query.draftId
  if (draftId) loadDraft(draftId)
})
</script>
```

---

## 界面风格约定

**整体风格**：
- 浅灰底（`#f5f5f5`）+ 白色卡片，区块边框使用 `1px solid #e8e8e8`
- 表格优先，不用堆叠卡片式表单，"更像业务软件，不像互联网后台"
- 数字列（数量/单价/金额）右对齐
- 状态标签用 `el-tag`（草稿=info，已确认=success，已作废=danger）

**三个核心交互规则**（参考传统进销存软件分析结论）：

1. **预置空行**：明细表格初始化时默认 8 行空行，不是"点按钮才出现行"。空行不参与计算，视觉上像一张等待填写的空白单据。用户直接在第一行开始录入，录单节奏不被打断。

2. **合计行内嵌表格**：合计数量和合计金额作为表格最后一行展示，与明细行使用相同列结构，数字在对应列下方对齐。不在表格外单独搭一个汇总区。

3. **操作区左辅右主**：左侧放辅助操作（查看历史、智能识别导入），右侧放主操作（保存/打印/清空）。主操作按钮可标注快捷键（如"保存草稿(S)"），减少鼠标依赖。

**其他约定**：
- 单据编号和日期在单据头**右上角**醒目展示，字号稍大，只读
- 商品列宽应充分利用，不用在表格里放笨重的全宽 select；后续可改为单元格内嵌 `···` 触发搜索弹窗
- 操作区按钮不做成卡片顶部，固定在卡片底部或页面底部，不随滚动消失

---

## UI 参考分析

> 本节记录重构界面设计时参考的传统进销存软件截图分析，供实现时对照。

参考截图：智慧记进销存「销售单」录单界面（2023年截图）

### 参考截图的关键设计点

| 设计点 | 参考界面的做法 | 我们的借鉴方向 | 优先级 |
|--------|-------------|-------------|--------|
| 表格空行 | 默认显示 11 行空行 | 初始化预置 8 行，空行不计入汇总 | 高 |
| 合计行 | 嵌入表格末行，数量/金额列对齐 | `el-table` summary-method 或追加固定行 | 高 |
| 单据编号/日期 | 右上角大字，与客户字段分左右两栏 | 单据头采用左右两栏布局 | 中 |
| 操作区分组 | 左：查看历史/导入；右：保存/打印/清空 | 同，按钮标注快捷键 | 高 |
| 品名"···"弹出选择 | 单元格内小按钮触发搜索弹窗 | 阶段 2 先用 el-select，后续可改 | 低（阶段5） |
| 折扣率 | 底部单独字段，联动折后金额 | 当前无折扣需求，暂不引入 | 不引入 |
| 营业员/结算账户 | 底部辅助字段 | 当前无此字段，暂不引入 | 不引入 |

### 与参考界面的主要差异

我们**不照搬**的内容：
- 蓝色皮肤和旧式视觉风格（保持 Element Plus 浅色风格）
- 折扣率、营业员、结算账户字段（当前业务不需要）
- 销售出货/销售退货单选按钮（入库/出库是独立页面）
- Tab 多开单据（路由架构不同）

我们**重点借鉴**的核心逻辑：
- 预置空行 → 像 Excel 一样直接录入
- 合计行内嵌 → 数字对齐，一目了然
- 操作区左辅右主 → 效率优先
- 单据编号右上角 → 单据身份感

---

## 改造效果（实际结果）

| 指标 | 重构前 | 重构后（实际） |
|------|--------|-------------|
| InboundCreateView 行数 | 1564 | **158** ✓ |
| OutboundCreateView 行数 | 1231 | **160** ✓ |
| InboundPrintView 行数 | 355 | **73** ✓ |
| OutboundPrintView 行数 | 315 | **63** ✓ |
| 代码重复率 | >80% | <5% ✓ |
| 可独立测试的逻辑单元 | 0 | 7 个基础 composables + 2 个页面级 composable + 4 个工具模块 ✓ |
| `toChineseAmount` 副本数 | 2 | **1**（utils/printUtils.js）✓ |
| Pinia 使用情况 | 安装未使用 | **auth store 已接入** ✓ |

---

## 历史演进摘要

- v1.0–v1.3：CreateView 将手工流程、AI 识别流程、快速新建弹窗全部内联，无逻辑层抽象，单文件超 1200 行。
- v1.4：功能完整，打印页已独立路由，但逻辑重复率 > 80%，无 composables 层，无汇总区，操作区不完整。
- v1.5（当前）：完成阶段1–4重构——建立 composables 层（9个文件）、通用组件层（7个组件）、工具层（4个文件）、打印架构三层分离；CreateView 降至 ~160 行，PrintView 降至 ~70 行。
- 下一步（阶段5）：预置空行、合计行内嵌表格、操作区完整按钮，向 ERP 单据工作台交互靠拢。
