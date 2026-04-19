# 架构设计说明

> 状态：Active  
> Owner：Yocaihua8  
> Last Updated：2026-04-18  
> Scope：ERP + WMS 一体化系统  
> Related：`../架构设计说明.md`、`./system-design-overview.md`、`../adr/README.md`

## 1. 架构结论

当前系统采用：

- Web / 桌面双入口
- Spring Boot 单体后端三层架构
- MyBatis + MySQL 持久化
- 独立 Python OCR 服务

| 字段 | 内容 |
|------|------|
| 架构模式 | 前后端分离 + 单体三层架构 + 外部 AI 服务 |
| 核心边界 | 主数据、单据状态流转、库存审计、AI 识别确认 |
| 主要入口 | Web 前端、JavaFX 桌面端、Spring Boot HTTP API |
| 主要外部依赖 | MySQL、Python FastAPI + PaddleOCR |

## 2. 架构图（文字版）

### 2.1 当前三层架构示意

```text
[表现层：Vue 页面 / JavaFX 页面 / Spring Controller]
                    ↓
[业务层：Service / ServiceImpl / 事务与权限校验]
                    ↓
[数据层：MyBatis Mapper / MySQL / RestTemplate 到 Python AI]
```

## 3. 技术栈

| 层级 | 技术 | 用途 | 选择原因 |
|------|------|------|----------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia | Web 页面与状态管理 | 生态成熟，适合中后台业务页面 |
| 后端 | Spring Boot 3 + MyBatis | 业务编排与数据访问 | 单体交付效率高，SQL 可控 |
| 数据库 / 基础设施 | MySQL 8 + Flyway | 数据持久化与迁移管理 | 关系模型稳定，迁移过程可控 |
| AI 服务 | FastAPI + PaddleOCR | OCR 识别与结构化提取 | 与 Java 主业务解耦，便于单独演进 |

## 4. 结构职责

### 4.1 表现层

- Vue 页面、JavaFX 页面负责交互与渲染
- Controller 负责接收请求、参数校验、响应封装
- 不承载核心库存、状态流转和权限业务规则

### 4.2 业务层

- Service 层负责：
  - 单据状态流转
  - 库存联动
  - 权限二次校验
  - 事务边界
  - AI 识别流程编排

### 4.3 数据层

- MyBatis Mapper 负责数据库读写
- Python OCR 服务通过 `RestTemplate` 以外部依赖形式接入
- 数据快照与审计日志由后端统一落库

## 5. 外部依赖

| 依赖项 | 类型 | 用途 | 风险 |
|--------|------|------|------|
| MySQL | 外部基础设施 | 存储主数据、单据、库存、日志 | 本地配置与 Docker 配置仍需进一步收敛 |
| Python AI 服务 | 外部服务 | OCR 识别与结构化建议 | 不可用时只影响 AI 识别链路 |

## 6. 关键设计约束

- 表现层不直接承载核心业务规则
- 库存变更统一走 `StockFlowService`
- Admin-only 操作必须在拦截器和 Service 层双重校验
- AI 识别结果不直接生成正式单据，必须经过人工确认
- 历史单据使用快照字段，避免主数据变更破坏历史展示

## 7. 备选方案与取舍

| 方案 | 是否采用 | 原因 |
|------|----------|------|
| 单体三层架构 | 是 | 当前模块规模可控，交付效率更高 |
| 微服务拆分 | 否 | 当前复杂度不足以支撑额外运维与分布式成本 |
| AI 服务独立部署 | 是 | OCR 依赖与 Java 主业务技术栈不同，拆出更稳 |

## 8. 现有详细文档

当前更完整的架构细节仍维护在：

- [../架构设计说明.md](../架构设计说明.md)

本文件的作用是对齐规范目录，并提供规范化入口；若两者内容出现差异，以当前代码和详细架构文档同步修正。
