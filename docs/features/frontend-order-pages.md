# 前端单据页面架构规格

---

## 1. 模块定位

本文档描述入库单创建页、出库单创建页、打印视图的**前端实现架构**，包括：

- 当前实现现状与耦合问题
- 目标架构设计（ERP 单据工作台模式）
- 目录结构与文件职责划分

**后端业务规则**见 [inbound-order.md](./inbound-order.md) / [outbound-order.md](./outbound-order.md)，本文档只涉及前端实现层面。

---

## 2. 当前实现现状

### 2.1 文件规模

| 文件 | 行数 | 职责 |
|------|------|------|
| `views/inbound/InboundCreateView.vue` | 1564 | 手工入库 + AI 识别 + 快速新建 + 草稿加载 |
| `views/outbound/OutboundCreateView.vue` | 1231 | 手工出库 + AI 识别 + 快速新建 + 草稿加载 |
| `views/inbound/InboundPrintView.vue` | 355 | 打印模板（独立路由） |
| `views/outbound/OutboundPrintView.vue` | 315 | 打印模板（独立路由） |

### 2.2 已知耦合问题

| 问题 | 影响 |
|------|------|
| 入库/出库两个 CreateView 结构性重复 > 80% | 同一 Bug 需改两处 |
| AI 识别对话框嵌入 CreateView 内，共享组件状态 | AI 流程无法独立演进 |
| `toChineseAmount()`（~90 行）在两个 PrintView 中逐字复制 | 改一处漏另一处 |
| `buildPrintItems()` 同样在两处复制 | 同上 |
| 无 composables 层：所有逻辑内联在 .vue 文件 | 逻辑不可复用、不可测试 |
| 无页面状态模型：通过 `currentDraftId` 隐式判断新建/编辑 | 状态切换逻辑散落 |
| 校验逻辑命令式 if-else 链，无法复用 | 出库/入库分别维护一份 |

---

## 3. 目标架构

### 3.1 页面结构（ERP 单据工作台）

```
┌─────────────────────────────────────────────┐
│ 页面标题区（动态标题 + 操作按钮组）            │
├─────────────────────────────────────────────┤
│ 单据头区域                                    │
│  ┌ 交易对手（入库=供应商文本/出库=客户下拉）    │
│  └ 备注                                      │
├─────────────────────────────────────────────┤
│ 明细表格区域（OrderItemTable 组件）           │
│  ┌ 工具栏（新增明细）                          │
│  └ 可编辑表格：商品选择 + 数量 + 单价 + 金额   │
├─────────────────────────────────────────────┤
│ 汇总区域（合计数量 + 合计金额）                │
├─────────────────────────────────────────────┤
│ 操作区域（返回 | 保存草稿 | 智能识别导入）      │
└─────────────────────────────────────────────┘
```

### 3.2 Composables 层（`composables/`）

| 文件 | 职责 | 入库/出库共用 |
|------|------|:---:|
| `useOrderForm.js` | 表单状态、草稿加载、提交、API 映射 | ✓ |
| `useOrderItems.js` | 明细行增删改、商品选择回填 | ✓ |
| `useOrderCalc.js` | 行金额计算、合计数量、合计金额 | ✓ |
| `useOrderValidation.js` | 三层校验（字段→行→单据） | ✓ |
| `useProductSearch.js` | 商品远程搜索 + 选项管理 | ✓ |
| `useAiRecognition.js` | AI 上传、识别、草稿状态、确认 | ✓ |
| `useQuickCreate.js` | 快速新建供应商/客户/商品的弹窗逻辑 | ✓ |

`useOrderForm` 接受 `orderType: 'inbound' | 'outbound'` 参数，内部区分：
- 入库：`form.supplierName`（文本输入）
- 出库：`form.customerId`（下拉选择）

### 3.3 通用组件层（`components/order/`）

| 组件 | 职责 |
|------|------|
| `OrderItemTable.vue` | 可编辑明细表格，通过 props/emit 与外部解耦 |
| `AiRecognitionDialog.vue` | AI 识别对话框，通过 `@confirmed` 事件向外传结果 |
| `QuickCreateDialog.vue` | 通用快速新建弹窗（商品/供应商/客户） |
| `OrderSummary.vue` | 汇总区域（合计数量/金额展示） |

### 3.4 打印架构（三层）

```
业务数据（API 返回 detail 对象）
    ↓
打印数据适配器（utils/printAdapter.js）
    ↓  buildInboundPrintData / buildOutboundPrintData → PrintData
打印模板组件（components/print/PrintTemplate.vue）
    ↓  纯渲染，不调 API，不含业务判断
window.print()
```

**工具函数**（`utils/printUtils.js`）：
- `toChineseAmount(amount)` — 数字金额转中文大写（原 PrintView 内重复代码合并为唯一来源）
- `padPrintItems(items, minRows = 12)` — 补齐空行至指定最小行数

**打印数据模型（PrintData）**：
```
{
  title: string,
  headerFields: [{ label, value, span }],
  items: [{ index, productName, specification, unit, quantity, unitPrice, amount, remark, isEmpty }],
  totalAmount: string,
  totalAmountChinese: string
}
```

---

## 4. 目录结构（目标态）

```
frontend/src/
  composables/                 ← 本次新增
    useOrderForm.js
    useOrderItems.js
    useOrderCalc.js
    useOrderValidation.js
    useProductSearch.js
    useAiRecognition.js
    useQuickCreate.js
  components/
    order/                     ← 本次新增
      OrderItemTable.vue
      AiRecognitionDialog.vue
      QuickCreateDialog.vue
      OrderSummary.vue
    print/                     ← 本次新增
      PrintTemplate.vue
  utils/
    printUtils.js              ← 本次新增（合并两个 PrintView 的重复函数）
    orderHelper.js             ← 本次新增（createEmptyItem, parsePageData, displayText 等）
    printAdapter.js            ← 本次新增（PrintData 适配器）
    request.js                 ← 不变
    auth.js                    ← 不变
  views/
    inbound/
      InboundCreateView.vue    ← 重构为 ~180 行（使用 composables + 组件）
      InboundPrintView.vue     ← 重构为 ~60 行（使用 printAdapter + PrintTemplate）
      InboundDetailView.vue    ← 暂不动
      InboundListView.vue      ← 不动
    outbound/
      OutboundCreateView.vue   ← 重构为 ~150 行
      OutboundPrintView.vue    ← 重构为 ~50 行
      OutboundDetailView.vue   ← 暂不动
      OutboundListView.vue     ← 不动
```

---

## 5. 关键数据模型

### 5.1 明细行（`OrderItem`）

```js
{
  productId: null | number,
  productCode: string,
  productName: string,
  specification: string,
  unit: string,
  quantity: number,      // 默认 1，最小 1
  unitPrice: number,     // 默认 0，最小 0
  remark: string
}
```

### 5.2 页面常量

```js
ORDER_TYPE = { INBOUND: 'inbound', OUTBOUND: 'outbound' }
PAGE_MODE  = { CREATE: 'create', EDIT: 'edit' }
ORDER_STATUS = { DRAFT: 1, COMPLETED: 2, VOID: 3 }
```

---

## 6. 改造阶段计划

| 阶段 | 目标 | 产出 | 风险 |
|------|------|------|------|
| 1 | 抽逻辑层（不改页面） | composables + utils，替换 InboundCreateView 内联调用，功能不变 | 极低 |
| 2 | 重构入库页 | InboundCreateView 从 1564 行降至 ~180 行 | 中 |
| 3 | 重构出库页 | OutboundCreateView 复用阶段 2 产出，降至 ~150 行 | 低 |
| 4 | 独立打印模板 | printUtils + printAdapter + PrintTemplate，两个 PrintView 瘦身 | 低 |
| 5 | 补充增强 | DetailView 共享、汇总组件、草稿离开提示 | 低 |

改造全程保持**路由路径不变**，不修改后端 API，不影响已有功能。

---

## 7. 历史演进摘要

- 早期 CreateView 将手工流程与 AI 识别流程混合在同一组件，无逻辑层抽象。
- 当前版本（v1.5 前）：功能完整，但单文件超 1200 行，入库/出库重复率 > 80%。
- 本文档记录的架构为**重构目标态**，对应 BACKLOG P2 前端重构系列任务。
