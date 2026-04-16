# 单据页面前端架构规格

> **定位**：本系统从轻量 WMS 出发，逐步演进为**轻 ERP + WMS 一体化**。单据页（入库单、出库单）是核心录单入口，其前端架构决定了整个单据体系能否支撑后续扩展（销售单、采购单、调拨单等）。
>
> 本文档描述：当前实现架构、核心设计规范、组件与 composable 接口、打印架构、待实现事项，以及 UI 设计参考。
>
> 后端业务规则见 [inbound-order.md](./inbound-order.md) / [outbound-order.md](./outbound-order.md)。

---

## 1. 当前实现状态（2026-04）

### 1.1 目录结构

```
frontend/src/
├── views/
│   ├── inbound/
│   │   ├── InboundCreateView.vue       # 路由入口包装（7行，仅转发给 InboundOrderCreate）
│   │   ├── InboundOrderCreate.vue      # 入库创建页实体（127行）
│   │   ├── InboundListView.vue         # 入库单列表
│   │   ├── InboundDetailView.vue       # 入库单详情（只读）
│   │   └── InboundPrintView.vue        # 打印视图（73行）
│   └── outbound/
│       ├── OutboundCreateView.vue      # 路由入口包装（7行）
│       ├── OutboundOrderCreate.vue     # 出库创建页实体（129行）
│       ├── OutboundListView.vue        # 出库单列表
│       ├── OutboundDetailView.vue      # 出库单详情（只读）
│       └── OutboundPrintView.vue       # 打印视图（63行）
├── composables/
│   ├── useOrderCalc.js                 # 金额计算（纯函数，42行）
│   ├── useOrderForm.js                 # 表单状态与页面模式（90行）
│   ├── useOrderItems.js                # 明细行增删插改（97行）
│   ├── useOrderValidation.js           # 三层校验（99行）
│   ├── useProductSearch.js             # 商品远程搜索（126行）
│   ├── useAiRecognition.js             # AI 识别草稿管理（166行）
│   ├── useQuickCreate.js               # 快速新建弹窗（44行）
│   ├── useOrderItemTableFocus.js       # 明细表焦点 / 键盘流 / 行高亮（143行）
│   ├── useOrderWorkbenchPage.js        # 工作台共享核心（420行）
│   ├── useInboundCreatePage.js         # 入库页薄适配层（84行）
│   └── useOutboundCreatePage.js        # 出库页薄适配层（159行）
├── components/
│   ├── order/
│   │   ├── OrderItemTable.vue          # 统一编辑/只读明细表格（428行）
│   │   ├── OrderProductSelectCell.vue  # 商品选择列子组件（61行）
│   │   ├── OrderDetailItemTable.vue    # 遗留只读详情表（35行，待清理）
│   │   ├── OrderSummary.vue            # 汇总区组件（46行）
│   │   ├── AiRecognitionDialog.vue     # 入库 AI 识别对话框（868行）
│   │   ├── AiOutboundRecognitionDialog.vue  # 出库 AI 识别对话框（750行）
│   │   └── QuickCreateDialog.vue       # 快速新建通用弹窗（50行）
│   ├── order-workbench/
│   │   ├── OrderHeaderForm.vue         # 工作台单据头（195行）
│   │   ├── OrderDetailTable.vue        # 工作台明细包装层（133行）
│   │   ├── OrderSummaryBar.vue         # 工作台底部汇总条（158行）
│   │   └── ProductSelectDialog.vue     # 商品弹窗组件（115行）
│   └── print/
│       └── PrintTemplate.vue           # 通用打印模板
└── utils/
    ├── orderHelper.js                  # 常量 + 工具函数（68行）
    ├── printUtils.js                   # 大写金额 + 打印行补齐（105行）
    ├── printAdapter.js                 # 打印数据适配（78行）
    └── printService.js                 # 触发打印（10行）
```

### 1.2 层次架构

```
┌──────────────────────────────────────────────────────┐
│ View Layer                                           │
│  InboundOrderCreate / OutboundOrderCreate            │
│  InboundListView / OutboundListView                  │
│  InboundDetailView / OutboundDetailView              │
├──────────────────────────────────────────────────────┤
│ Page Composable Layer（共享核心 + 薄适配层）            │
│  useOrderWorkbenchPage                               │
│  ├── 组合所有基础 composable                          │
│  ├── 草稿保存 / 提交确认 / 离开保护                    │
│  └── 商品搜索 / 汇总计算 / 打印预览                    │
│  useInboundCreatePage / useOutboundCreatePage        │
│  ├── 注入 API / 路由 / 文案                           │
│  └── 补充供应商 / 客户 / 库存差异逻辑                  │
├──────────────────────────────────────────────────────┤
│ Base Composable Layer（业务逻辑，入库/出库共用）        │
│  useOrderForm   useOrderItems   useOrderCalc         │
│  useOrderValidation  useProductSearch                │
│  useAiRecognition    useQuickCreate                  │
├──────────────────────────────────────────────────────┤
│ Component Layer（UI 组件，无业务逻辑）                 │
│  OrderHeaderForm  OrderDetailTable  OrderSummaryBar  │
│  OrderItemTable   ProductSelectDialog                │
│  QuickCreateDialog                                   │
│  AiRecognitionDialog  AiOutboundRecognitionDialog    │
│  PrintTemplate                                       │
├──────────────────────────────────────────────────────┤
│ Utils Layer（纯函数）                                 │
│  orderHelper  printUtils  printAdapter  printService │
└──────────────────────────────────────────────────────┘
```

### 1.3 数据流

```
路由参数（?draftId=xxx）
    ↓
useInboundCreatePage / useOutboundCreatePage
    └─ useOrderWorkbenchPage
         ├─ useOrderForm       → form状态、pageMode、草稿ID、页面标题
         ├─ useOrderItems      → items 数组（增/删/插/改）
         ├─ useOrderCalc       → 行金额、总数量、总金额（computed）
         ├─ useOrderValidation → 三层校验
         ├─ useProductSearch   → productOptions、搜索防抖
         └─ API（保存草稿 / 提交确认 / 加载详情）
              ↓
    InboundOrderCreate / OutboundOrderCreate
         ├─ <OrderHeaderForm>      单据头
         ├─ <OrderDetailTable>     工作台明细包装层
         │     ├─ <OrderItemTable> 明细表格（editable）
         │     └─ <ProductSelectDialog> 商品弹窗
         ├─ <OrderSummaryBar>      底部汇总 + 主操作区
         └─ <AiRecognitionDialog>  AI 识别入口

AI 流程（独立）：
    AiRecognitionDialog / AiOutboundRecognitionDialog
         ├─ useAiRecognition    → AI草稿生命周期
         ├─ useProductSearch    → 商品匹配
         └─ useQuickCreate      → 快速新建产品/供应商/客户
         ↓ emit('confirmed', { form, items })
    页面接收，写入 form + items

打印流程：
    openPrintWindow(id, type) → window.open(/inbound/print/:id)
    PrintView → printAdapter.build*PrintData() → PrintTemplate
             → printService.triggerBrowserPrint()
```

补充：

- `InboundDetailView` / `OutboundDetailView` 已直接复用 `OrderItemTable`
- 详情页通过 `editable=false` 和显示开关关闭商品选择列、操作列、表格合计行，继续保留独立 `OrderSummary`

---

## 2. 核心设计规范

### 2.1 页面模式（PAGE_MODE）

页面模式控制 UI 可编辑性，与后端 `orderStatus` 解耦：

```js
// utils/orderHelper.js
export const PAGE_MODE = {
  CREATE:   'create',    // 新建，全部可编辑
  EDIT:     'edit',      // 编辑草稿，全部可编辑
  READONLY: 'readonly'   // 已确认 / 已作废，完全只读
}

// orderStatus → pageMode 映射
// DRAFT(1)     → EDIT
// COMPLETED(2) → READONLY
// VOID(3)      → READONLY
```

**使用原则**：所有可编辑控件的 `disabled` / `:editable` 条件统一读 `pageMode`，不直接判断 `orderStatus`。

### 2.2 单据状态（ORDER_STATUS）

```js
export const ORDER_STATUS = {
  DRAFT:     1,  // 草稿（可编辑、可作废）
  COMPLETED: 2,  // 已确认（入库/出库完成，只读）
  VOID:      3   // 已作废（只读）
}
```

### 2.3 主数据模型

```js
// 表单头
form = {
  orderNo: '',          // 系统生成，草稿阶段显示"待生成"
  orderDate: today(),   // 默认今日
  supplierName: '',     // 入库专有
  customerId: null,     // 出库专有
  remark: '',
  sourceType: 'MANUAL'  // MANUAL | AI，自动带入
}

// 明细行（createEmptyOrderItem() 产出）
item = {
  productId: null,
  productCode: '',
  productName: '',
  specification: '',
  unit: '',
  quantity: 1,
  unitPrice: 0,
  remark: '',
  availableStock: null  // 出库专有，只读展示
}
```

### 2.4 明细表格：Excel 式行内编辑

**核心原则**：每个单元格直接可编辑，无需弹窗，无"进入编辑模式"的额外点击。控件常驻于格子内，点击即录入，Tab 在格子间跳转。

| 列 | 编辑控件 | 备注 |
|----|---------|------|
| 商品选择 | `el-input`（只读）+ "选择" 按钮 | 点击按钮或 Enter 键打开 `ProductSelectDialog`；选中后回填编码/名称/规格/单位 |
| 商品编码 | `el-input` | 自由文本，可在弹窗选中后手动覆盖 |
| 商品名称 | `el-input` | 自由文本，可在弹窗选中后手动覆盖 |
| 规格 | `el-input` | 自由文本，可覆盖 |
| 单位 | `el-input` | 自由文本，可覆盖 |
| 数量 | `el-input-number` | 最小值 1，变更后自动重算金额 |
| 单价 | `el-input-number` | 最小值 0，精度两位小数，变更后自动重算金额 |
| 金额 | **只读文本** | `quantity × unitPrice`，自动计算 |
| 库存余量 | **只读文本** | 出库专有，显示当前可用库存 |
| 备注 | `el-input` | 自由文本 |
| 操作 | 文字按钮 | 插入行 / 删除行 |

`READONLY` 模式下：所有列渲染为纯文本 `<span>`，不渲染控件。

### 2.5 页面布局规范

```
┌─────────────────────────────────────────────────────────────┐
│  A. 单据头区域                                                │
│  左：[供应商/客户]  [备注]        右：单据编号 XSD2024001001  │
│                                       单据日期 2024-01-01    │
├─────────────────────────────────────────────────────────────┤
│  B. 明细表格（Excel 式行内编辑，预置 8 行空行）                │
│  ┌──┬──────────────┬──┬────┬──────┬──────┬──────┬───┬──┐   │
│  │行号│商品编码/名称  │规格│单位│ 数量 │ 单价 │ 金额 │备注│操作│   │
│  ├──┼──────────────┼──┼────┼──────┼──────┼──────┼───┼──┤   │
│  │ 1 │[el-select]   │[  ]│[  ]│[  0] │[  0] │ 0.00 │[  ]│+✕│   │
│  │ 2 │              │    │    │      │      │      │   │  │   │
│  │...│  预置 8 行    │    │    │      │      │      │   │  │   │
│  ├──┴──────────────┴──┴────┼──────┴──────┼──────┤───┴──┤   │
│  │  合 计                   │     0       │¥0.00 │      │   │
│  └─────────────────────────┴─────────────┴──────┴──────┘   │
├─────────────────────────────────────────────────────────────┤
│  C. 汇总区                                                    │
│    行数：0    合计数量：0    合计金额：¥0.00                   │
├─────────────────────────────────────────────────────────────┤
│  D. 操作区（左辅右主）                                        │
│  [智能识别导入]   [保存草稿(S)] [保存并新建] [清空] [打印(P)] │
└─────────────────────────────────────────────────────────────┘
```

**操作区按钮规则**：

| 按钮 | CREATE | EDIT（草稿） | READONLY |
|------|:------:|:-----------:|:--------:|
| 智能识别导入 | ✓ | ✓ | ✗ |
| 保存草稿 (S) | ✓ | ✓ | ✗ |
| 保存并新建 | ✓ | ✓ | ✗ |
| 清空 | ✓ | ✓ | ✗ |
| 打印 (P) | ✗ | ✓ | ✓ |
| 提交确认（ADMIN） | ✓ | ✓ | ✗ |

---

## 3. 关键 Composable 接口

### 3.1 `useOrderForm`

```js
const {
  pageMode,          // Ref<'create'|'edit'|'readonly'>
  currentDraftId,    // Ref<number|null>
  submitting,        // Ref<boolean>
  pageTitle,         // Ref<string>
  resetForm,         // () => void
  applyDraftDetail,  // (detail, mapHeader) => void
  resolvePageModeByStatus, // (status) => void
  markCreateMode,    // () => void
} = useOrderForm()
```

### 3.2 `useOrderItems`

```js
const {
  addItem,           // () => void
  removeItem,        // (index) => void
  insertItem,        // (afterIndex) => void
  updateRowField,    // (index, field, value) => void
  selectProduct,     // (index, productId, options, onProductChange?) => void
  resetItems,        // () => void
} = useOrderItems(items, { minItems: 1 })
```

### 3.3 `useOrderCalc`

```js
const {
  calcAmount,        // (row) => string  行金额
  calcTotals,        // (items) => { totalQuantity, totalAmount }
  useComputedTotals, // (items) => { totalQuantity: ComputedRef, totalAmount: ComputedRef }
} = useOrderCalc()
```

### 3.4 `useOrderValidation`

```js
const {
  validateInboundForm,     // (form, items, emitMsg) => boolean
  validateOutboundForm,    // (form, items, emitMsg) => boolean
  validateAiImportedItems, // (items, emitMsg) => boolean
} = useOrderValidation()
```

**三层校验逻辑**：

```
字段级：supplierName / customerId 不能为空
行级：  数量 > 0，单价 ≥ 0
单据级：至少一条有效明细
AI专用：所有行 matchedProductId 必须已设置（未匹配项阻止提交）
```

### 3.5 `useProductSearch`

```js
const {
  productOptions,        // Ref<Array>
  productLoading,        // Ref<boolean>
  loadProducts,          // (params?) => Promise
  handleProductSearch,   // (keyword) => void  防抖搜索
  upsertProductOption,   // (product) => void  维持选项去重
  handleProductChange,   // (index, productId) => void  回填行字段
} = useProductSearch()
```

### 3.6 `useAiRecognition`

```js
const {
  normalizeAiDraft,       // (rawItem, normalize?) => AiDraftItem
  markAiDraftDirty,       // (item) => void
  getAiWarningText,       // (item) => string
  addAiItem,              // () => void
  removeAiItem,           // (index) => void
  hasUnmatchedAiItems,    // (items) => boolean
  hasInvalidAiItems,      // (items) => boolean
  buildAiConfirmPayload,  // (form, items, orderType) => Payload
} = useAiRecognition()
```

AI 识别解耦模式：

```
AiRecognitionDialog.vue
    emit('confirmed', { form, items })
        ↓
页面接收 → 写入 form + items
用户可继续编辑后再保存/提交
```

### 3.7 `useOrderWorkbenchPage`

共享页面级核心 composable，负责入库/出库共同流程：

```js
const {
  aiDialogRef,
  form,
  editable,
  pageTitle,
  submitting,
  currentDraftId,
  productOptions,
  productLoading,
  summary,
  calcAmount,
  addItem,
  insertItem,
  removeItem,
  updateRowField,
  handleProductSearch,
  onProductChange,
  openAiDialog,
  handleClear,
  handleSubmit,
  handleSaveAndNew,
  handleSubmitConfirm,
  openPrintPreview,
  canSaveDraft,
  canSaveAndNew,
  canClear,
  canAiImport,
  canSubmitConfirm,
  canPrintPreview
} = useOrderWorkbenchPage()
```

**配置注入职责**：

- `defaultValues`：表单头默认值
- `fetchDetail/saveDraftApi/updateDraftApi/confirmApi`：接口差异
- `buildPayload/buildSnapshot/mapDetailHeader`：数据映射差异
- `listRoute/detailRouteBuilder/printRouteBuilder`：路由差异
- `loadInitialData/afterDraftLoaded/afterProductsLoaded/onProductSelected`：客户列表、库存刷新等扩展钩子
- `messages/notify`：入库与出库的提示文案和消息级别

### 3.8 `useInboundCreatePage` / `useOutboundCreatePage`

现在这两个文件只保留业务差异，作为薄适配层直接暴露给 View 使用：

| 类别 | 暴露内容 |
|------|---------|
| 状态 | `form`, `summary`, `pageTitle`, `submitting`, `editable` |
| 产品 | `productOptions`, `productLoading`, `handleProductSearch`, `onProductChange` |
| 客户（出库） | `customerOptions` |
| 计算 | `calcAmount` |
| 行操作 | `addItem`, `removeItem`, `insertItem`, `updateRowField` |
| 操作 | `handleSubmit`, `handleSubmitConfirm`, `handleClear`, `handleSaveAndNew` |
| AI | `openAiDialog`（通过组件 ref 触发） |
| 打印 | `openPrintPreview()` |
| 库存（出库） | 通过适配层内部 `loadAvailableStock` / `refreshItemStocks` 钩子接入，不再暴露给 View |

### 3.9 已完成：第二批工作台拆分（v1.7）

分三个连续小步完成：

#### `v1.7-a`：公共页面级 composable（已完成）

- 已新增 `useOrderWorkbenchPage.js`
- 已抽取公共骨架：
  - 页面模式
  - 草稿保存
  - 提交确认主链路
  - 清空 / 保存并新建
  - 离开保护
  - 合计计算
  - 商品搜索基础能力
- 入库/出库差异已通过配置项注入：
  - 供应商 / 客户字段
  - 出库库存加载
  - 保存 / 确认 API
  - 路由与提示文案
- 已采用“共享核心 + 薄适配层”：
  - `useInboundCreatePage.js` / `useOutboundCreatePage.js` 内部调用共享核心
  - 页面容器继续消费原适配层结果，不需要重写模板结构

#### `v1.7-b`：商品弹窗选品（已完成）

- 已将 `OrderItemTable.vue` 中的商品列，从当前内联 `el-select` 切换为 `ProductSelectDialog.vue`
- 弹窗状态统一由 `OrderDetailTable.vue` 托管：
  - `dialogVisible`
  - `dialogKeyword`
  - `activeRowIndex`
  - `selectedProductId`
- 已实现：
  - 点击商品格或“选择”按钮打开弹窗
  - 按商品编码 / 名称搜索
  - 单击选中、双击确认、底部按钮确认
  - 回填商品编码 / 名称 / 规格 / 单位，并继续复用既有默认单价 / 库存逻辑
- 页面级 composable 保持不变，继续只暴露：
  - `productOptions`
  - `productLoading`
  - `handleProductSearch`
  - `onProductChange`
- 快速新建商品在这一批仅预留事件接口 `quick-create`，未接入完整创建链路，也不扩展到客户/供应商

#### `v1.7-c`：键盘录入流（第一版已完成）

- 已实现明细表主录入列的顺序 `Tab` 跳转
- 已补齐 `Shift+Tab` 反向链：
  - 当前格回退到上一格
  - 当前行第一格回退到上一行最后一格
  - 第一行第一格保持当前焦点，不再继续向外跳转
- 当前顺序：
  - 商品选择
  - 商品编码
  - 商品名称
  - 规格
  - 单位
  - 数量
  - 单价
  - 备注
- 已实现末格跳转规则：
  - 当前行 `备注` 按 `Tab`
  - 若存在下一行，聚焦下一行“商品选择”
  - 若当前已是最后一行，自动新增一行并聚焦新行“商品选择”
- 已与“保存并新建”动作衔接：
  - 保存成功后重置表单
  - 自动聚焦第一行“商品选择”
  - 后续继续按 `Tab` 进入既有主录入链
- 已补充当前录入行高亮：
  - 编辑态下，当前聚焦或点击的录入行高亮显示
  - `Tab` 跳到下一格 / 下一行时，高亮同步跟随
- 第一版明确未纳入焦点链：
  - 商品弹窗内部焦点流
  - 清空 / 插入 / 删除按钮
  - 合计行
  - 底部操作区按钮
- 实现位置：
  - `useOrderItemTableFocus.js` 负责主录入列焦点注册、`Tab/Shift+Tab` 跳转、行高亮与首格聚焦
  - `OrderItemTable.vue` 只负责表格渲染与事件桥接
  - 商品选择列已拆为 `OrderProductSelectCell.vue`，仅负责该列渲染与“选择/清空”按钮
  - 页面层与 composable 层不感知 DOM 焦点细节

---

## 4. 打印架构

四层分离，职责明确：

```
业务详情数据（API 返回 detail 对象）
    ↓
utils/printAdapter.js
    buildInboundPrintData(detail) → PrintData
    buildOutboundPrintData(detail) → PrintData
    ↓
components/print/PrintTemplate.vue
    纯渲染，不调 API，不含业务逻辑
    ↓
utils/printService.js
    triggerBrowserPrint()   → window.print()（延迟200ms等待DOM渲染）

utils/printUtils.js（工具函数）
    toChineseAmount(n)      → 中文大写金额
    padPrintItems(items, n) → 补齐打印行至最少行数
    openPrintWindow(id)     → 新窗口打开打印页
```

**PrintData 结构**：

```js
{
  title: string,
  headerFields: [{ label, value, span }],
  items: [{
    index, productName, specification,
    unit, quantity, unitPrice, amount, remark, isEmpty
  }],
  totalAmount: string,
  totalAmountChinese: string
}
```

---

## 5. 组件接口规格

### 5.1 `OrderItemTable.vue`

现已同时用于：

- 创建页工作台明细表（编辑态）
- 入库 / 出库详情页只读明细表（只读态）

**Props**：

| Prop | 类型 | 说明 |
|------|------|------|
| `items` | Array | 明细行数组（响应式） |
| `editable` | Boolean | `false` → 所有列渲染为纯文本 |
| `orderType` | String | `'inbound'`\|`'outbound'`，控制是否显示库存余量列 |
| `productOptions` | Array | 商品下拉选项 |
| `productLoading` | Boolean | 搜索加载状态 |
| `calcAmount` | Function | `(row) => string` 行金额计算 |
| `showToolbar` | Boolean | 是否显示顶部标题工具条 |
| `showAddButton` | Boolean | 是否显示“新增明细”按钮 |
| `showIndexColumn` | Boolean | 是否显示行号列 |
| `showProductSelectColumn` | Boolean | 是否显示商品选择列 |
| `showActionColumn` | Boolean | 是否显示操作列 |
| `showSummary` | Boolean | 是否显示表格底部合计行 |
| `showStockColumn` | Boolean | 是否显示出库库存余量列 |
| `stripe` | Boolean | 是否启用条纹行，只读详情页可开启 |

**Events**：

| Event | 参数 | 说明 |
|-------|------|------|
| `row-add` | — | 添加行 |
| `row-insert` | `afterIndex` | 在指定行后插入 |
| `row-delete` | `index` | 删除行 |
| `row-field-change` | `(index, field, value)` | 行字段变更（触发金额重算） |
| `open-product-dialog` | `index` | 打开当前行的商品弹窗（`v1.7-b` 目标协议） |
| `product-selected` | `(index, productId)` | 商品选中，触发字段回填 |

**键盘流（`v1.7-c` 第一版）**：

- 主录入列顺序固定为：`商品选择 → 商品编码 → 商品名称 → 规格 → 单位 → 数量 → 单价 → 备注`
- `Tab` 到当前行最后一格时，默认跳到下一行第一格
- 若当前已是最后一行，则自动补一行并跳转
- `Shift+Tab` 按相反顺序回退；当前行第一格可回退到上一行最后一格
- 不纳入第一版焦点链：清空按钮、操作列按钮、合计行、底部操作区
- 编辑态下高亮当前录入行，便于连续录单时定位焦点所在行

**组件公开方法**：

- `focusFirstEditableCell()`：聚焦第一行“商品选择”，供“保存并新建”成功后继续录单使用

补充：

- 当前轮次瘦身已停在两步：
  - 焦点 / 键盘流抽到 `useOrderItemTableFocus.js`
  - 商品选择列抽到 `OrderProductSelectCell.vue`
- 商品编码 / 商品名称 / 规格 / 单位 这 4 列虽然模板相似，但目前仍保留在 `OrderItemTable.vue` 内联渲染
- 原因：
  - 这 4 列主要是简单输入模板重复，而不是独立职责块
  - 若现在抽成通用文本列组件，会明显增加参数化复杂度，降低直接可读性
- 数量 / 单价列当前也不建议继续抽离：
  - `el-input-number` 焦点节点比普通 `el-input` 更敏感
  - 继续拆分会放大 `Tab / Shift+Tab`、行高亮和自动补行回归风险
- 后续仅在新增列级交互或模板差异继续扩大时，再按需求驱动评估是否继续拆分

### 5.2 `ProductSelectDialog.vue`

**Props**：

| Prop | 类型 | 说明 |
|------|------|------|
| `visible` | Boolean | 弹窗开关 |
| `keyword` | String | 当前搜索关键字 |
| `products` | Array | 候选商品列表 |
| `loading` | Boolean | 商品列表加载状态 |
| `selectedProductId` | Number\|String\|null | 当前选中商品 |

**Emits**：

| Event | 参数 | 说明 |
|-------|------|------|
| `update:visible` | `visible` | 控制弹窗显隐 |
| `search` | `keyword` | 远程搜索 |
| `select` | `product` | 单击或双击选中商品 |
| `confirm` | `product` | 点击确认后回填到当前行 |
| `quick-create` | `keyword` | 预留商品快速新建入口（`v1.7-b` 先预留，不要求完整实现） |

### 5.3 `QuickCreateDialog.vue`

**Props**：`visible`, `title`, `width`, `loading`, `confirmText`  
**Emits**：`update:visible`, `confirm`  
通过默认 slot 承载表单，可复用于快速新建供应商 / 客户 / 商品。

### 5.4 `OrderSummary.vue`

**Props**：`itemList`（Array）  
自动计算并展示：行数（`lineCount`）、合计数量（`totalQuantity`）、合计金额（`totalAmount`）。

---

## 6. ERP 扩展路线

当前系统是 WMS（入库单、出库单）。向轻 ERP 演进时，单据体系需扩展：

| 单据类型 | 状态 | 复用基础 |
|---------|------|---------|
| 入库单（采购入库） | ✅ 已实现 | — |
| 出库单（销售出库） | ✅ 已实现 | — |
| 采购单 | 待规划 | `useOrderForm` + `useOrderItems` + `useOrderCalc` |
| 销售单 | 待规划 | 同上 |
| 调拨单 | 待规划 | 同上，双仓库字段 |
| 盘点单 | 待规划 | 独立流程 |

**扩展原则**：新单据类型只需新增页面级 `useXxxCreatePage.js`，复用全部基础 composable，不修改通用层。

---

## 7. 待实现事项（阶段 6+）

| 项目 | 优先级 | 说明 |
|------|:------:|------|
| `OrderItemTable` 瘦身 | 中 | 当前轮次已收口：焦点/键盘流已抽到 `useOrderItemTableFocus.js`，商品选择列已拆到 `OrderProductSelectCell.vue`；商品编码/名称/规格/单位与数量/单价列暂不继续抽象，后续仅在新增需求驱动下再评估 |
| 键盘录入流增强 | 中 | 剩余弹窗内部焦点管理、Enter/F3 等快捷键后续再做 |
| `ProductFormView` / `CustomerFormView` | 低 | 当前为空文件，确认用途或删除 |

---

## 8. UI 设计参考

### 8.1 两款进销存软件共同验证的录单模式

| 设计原则 | 智慧记（2023） | 辛巴商贸通+（2024） | 我们的目标 |
|---------|:----------:|:-------------:|:--------:|
| 预置空行 | 11 行 | 26+ 行 | 8 行 |
| 单元格常驻控件（Excel 式） | ✓ | ✓ | ✓（已实现） |
| 合计行嵌入表格末行 | ✓ | ✓ | ✓（已实现） |
| 操作区快捷键标注 | 部分 | ✓（F3等） | S/P/C |
| 单据编号右上角 | ✓ | ✓ | ✓（已实现） |

### 8.2 辛巴商贸通+ 界面结构（参考）

```
┌──────────────────────────────────────────────────────────────┐
│ 供货单位 | 经手人 | 部门 | 付款期限       票单日期  单据编号   │
│ 仓库 | 折扣 | 税率 | 预设售价           应付款/预付款/本单付款 │
│ 摘要 | 附加说明 | 送货员 | 备注                               │
├──────────────────────────────────────────────────────────────┤
│ 明细表（26+ 行预置，全格直接可编辑）                           │
│ 商品编号│名称│条码│规格│型号│品牌│单位│数量│单价│金额│折扣│税额 │
│ ─────────────────────── 合 计 ──────────────────────────    │
├──────────────────────────────────────────────────────────────┤
│ 付款账户 | 发票信息 | 运输费用 | 优惠金额   ← 不引入          │
├──────────────────────────────────────────────────────────────┤
│ 打开 | 条码(F3) | 草稿                    [✓确认] [✗取消]    │
└──────────────────────────────────────────────────────────────┘
```

**我们不引入**：折扣/税率列、批号/辅助数量、付款账户、发票信息、多仓库/部门/经手人（单仓库单角色场景）。
