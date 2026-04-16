# ADR-003: OCR 识别作为独立 Python 服务

## 状态
已采纳（2025-08）

## 背景

系统需要对采购单/出库单图片进行 OCR 识别并提取结构化商品信息。需要决定 OCR 能力的部署方式：
1. 在 Java 后端内嵌 OCR 库
2. 作为独立 Python 服务，Java 通过 HTTP 调用

## 决策

OCR 识别作为**独立 Python FastAPI 服务**（port 9000）运行，Java 后端通过 `AiPythonOcrClient`（RestTemplate）调用。

Python 服务职责：
- PaddleOCR 文字识别
- 商品信息结构化提取（productName / specification / unit / quantity / unitPrice）
- 供应商/客户名称提取
- 返回标准 JSON 结构

Java 后端职责：
- 调用 Python 服务（`OcrAdapterService`）
- 商品匹配（`ProductMatchService`）
- AI 草稿持久化（`AiDraftPersistenceService`）
- 流程编排与确认（`AiRecognitionServiceImpl`）

## 后果

**正面影响：**
- Python 生态的 OCR 库（PaddleOCR）成熟度远高于 Java 同类库
- AI 能力可独立迭代升级，不影响后端发版
- 服务不可用时只影响 AI 识别链路，其余业务正常运行（降级友好）
- 可按需水平扩展 OCR 服务实例，不影响 Java 后端

**负面影响 / 权衡：**
- 增加一个运行时依赖，本地开发需额外启动 Python 服务
- 服务间网络调用增加延迟（通常 < 2s，可接受）
- 目前无降级机制，Python 服务不可用时直接返回错误
- 需维护两套语言环境（Java + Python）

## 备选方案

**Java 内嵌 Tesseract**：通过 `tess4j` 调用，无需独立服务，但中文识别效果显著差于 PaddleOCR，且模型文件需打包进 JAR 导致包体积大。

**调用云 OCR API**：效果好且无需本地资源，但依赖网络和付费，不适合离线/内网部署场景。
