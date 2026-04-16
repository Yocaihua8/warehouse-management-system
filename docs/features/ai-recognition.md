# AI 辅助单据识别功能规格

---

## 1. 功能概述

AI 识别功能允许用户上传采购单/销售单图片，系统调用 Python OCR 服务识别商品明细，经过人工复核确认后，生成正式入库单或出库单。

说明：

- Web 前端与 Python AI 服务当前已统一为仅支持图片识别（`jpg/jpeg/png`）
- PDF 识别当前尚未接入，上传 PDF 会被服务端拒绝

**核心原则：AI 识别结果必须经过人工确认，不直接生成正式单据。**

### 历史演进摘要

- 早期方案仅规划“AI 入库识别”，当前已落地为“入库 + 出库”双链路。
- 早期匹配策略以字符串精确/LIKE 为主，当前已升级为“精确优先 + 语义相似匹配兜底”（编辑距离 + 字符 n-gram 向量相似度）。
- 早期文档中独立的 AI 需求与前期方案已并入本规格，不再单独维护。

---

## 2. 系统组件

| 组件 | 职责 |
|------|------|
| `AiAssistController` | 接收前端请求，调用 Service |
| `AiRecognitionServiceImpl` | 编排识别流程、商品匹配、订单创建 |
| `AiPythonOcrClient` | 用 RestTemplate 调用 Python OCR 服务 |
| Python FastAPI 服务（port 9000） | PaddleOCR 识别、文本清洗、字段提取 |

---

## 3. 识别状态机

```
  上传文件
      │
      ▼
  ┌─────────┐    Python 服务失败
  │ pending  │ ─────────────────────► ┌────────┐
  └────┬────┘                         │ failed │（终态）
       │ Python 识别成功               └────────┘
       ▼
  ┌─────────┐
  │ success  │  等待用户确认
  └────┬────┘
       │ 用户确认
       ▼
  ┌───────────┐
  │ confirmed │  正式单据已生成（终态）
  └───────────┘
```

---

## 4. 入库识别流程（inbound）

### Step 1：识别

```
POST /ai/inbound/recognize (multipart/form-data, file=...)
  → 创建 ai_recognition_record（status=pending, doc_type=inbound）
  → 调用 Python /ocr/inbound/recognize，上传文件
  → Python 返回 { supplierName, items: [{productName, spec, unit, qty, price}], rawText, warnings }
  → 遍历 items：
      按 productName 在 product 表模糊查询，尝试自动匹配
      匹配到 → match_status=matched，填充 matched_product_id
      未匹配 → match_status=unmatched
  → 尝试自动匹配供应商（按 supplierName 模糊查询 supplier 表）
  → 保存 ai_recognition_item 列表
  → 更新 record status=success
  → 返回 AiInboundRecognizeVO（含识别项列表 + 匹配状态 + 供应商匹配结果）
```

### Step 2：用户确认（ADMIN）

```
POST /ai/inbound/confirm (AiInboundConfirmDTO)
  → 用户在前端核对/修改识别结果后提交
  → 调用 InboundOrderService 创建入库单（source_type=AI）
  → 调用 StockFlowService.increaseByInbound()（更新库存）
  → 更新 ai_recognition_record.confirmed_order_id = 新入库单 ID
  → 更新 record status=confirmed
  → 返回 orderId
```

**注意**：AI 确认入库是 Admin 专属操作，和手动确认入库一样需要 Admin 权限。

---

## 5. 出库识别流程（outbound）

与入库识别流程对称，差异：
- 匹配的是**客户**（customer 表），而非供应商
- 确认后调用 `StockFlowService.decreaseByOutbound()`（扣减库存）
- 确认时同样校验库存充足性

---

## 6. 商品匹配逻辑

1. 按 `商品名 + 规格 + 单位` 精确匹配
2. 精确不中则按 `商品名 + 规格`、`商品名` 依次匹配
3. 仍未命中时进入语义评分兜底：
- 编辑距离相似度（Levenshtein）
- 字符 trigram 向量余弦相似度
- 规格/单位一致性加分
4. 最高分低于阈值或前两名分差过小时，返回 `unmatched`，要求人工确认

---

## 7. API 接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| `/ai/inbound/recognize` | POST | 已登录 | 上传文件识别入库单 |
| `/ai/inbound/confirm` | POST | **Admin** | 确认入库识别结果，生成正式入库单 |
| `/ai/outbound/recognize` | POST | 已登录 | 上传文件识别出库单 |
| `/ai/outbound/confirm` | POST | **Admin** | 确认出库识别结果，生成正式出库单 |
| `/ai/inbound/list` | GET | 已登录 | AI 入库识别历史列表 |
| `/ai/outbound/list` | GET | 已登录 | AI 出库识别历史列表 |
| `/ai/inbound/detail/{id}` | GET | 已登录 | AI 识别记录详情 |
| `/ai/outbound/detail/{id}` | GET | 已登录 | AI 出库识别记录详情 |
| `/ai/ping` | GET | 已登录 | AI 服务连通性测试 |

---

## 8. 已知限制与注意事项

1. **Python 服务不可用时无降级**：调用失败时仍直接返回错误，无 fallback 机制
2. **文件大小限制**：当前通过 `spring.servlet.multipart.max-file-size=20MB` 控制，Python 侧无独立限制
3. **当前仅支持图片识别**：PDF 识别尚未接入
4. **历史列表筛选能力待增强**：当前出库历史已具备列表/详情，但筛选维度仍可继续增强
5. **超时策略可继续细化**：已支持可配置超时，后续可按 OCR/匹配/确认等接口场景细分策略
6. **匹配策略仍需持续迭代**：复杂商品名、规格别名、OCR 噪声场景下仍可能出现低置信匹配
