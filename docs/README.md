# ERP + WMS 一体化系统 — 工程文档体系

> **文档约定**：本目录下所有文档描述**当前已实现**的系统现状，不包含规划性内容。  
> 规划中的功能和已知问题统一记录在 [BACKLOG.md](./BACKLOG.md)。

---

## 按场景导航

### 我是新人，想快速了解系统

1. [架构设计说明](./架构设计说明.md) — 系统全貌、技术栈、模块关系
2. [数据库设计](./database.md) — 12 张表结构、枚举值定义
3. [本地环境搭建](./guides/setup.md) — 从零启动系统的完整步骤
4. [系统现状与风险清单](./系统现状与风险清单.md) — 当前运行边界、已收敛问题、高风险项

### 我要开发某个功能模块

1. 阅读对应的 [功能规格文档](./features/README.md)（业务规则、状态机、API 接口）
2. 查看 [分支与提交规范](./guides/branch-conventions.md)
3. 如使用 AI 辅助开发，阅读 [Codex Guide](./guides/codex-guide.md)

### 我要修复 Bug 或做改进

1. 先看 [系统现状与风险清单](./系统现状与风险清单.md) 确认当前真实运行边界
2. 再看 [BACKLOG](./BACKLOG.md) 确认优先级和上下文
3. 阅读对应模块的功能规格文档
4. 查看 [项目路线图](./项目路线图.md) 了解当前版本阶段
5. 如需了解最近一轮推进过程，查看 [开发日志](./devlog/README.md)

### 我要准备项目演示或求职材料

→ 以 [README](../README.md)、[架构设计说明](./架构设计说明.md) 和各模块功能规格文档为准，自行整理演示材料。

---

## 文档全量索引

### 系统设计

| 文档 | 说明 | 目标读者 |
|------|------|---------|
| [架构设计说明](./架构设计说明.md) | 系统定位、模块总览、技术栈、分层职责、关键设计决策 | 所有开发者 |
| [数据库设计](./database.md) | 12 张表结构、字段说明、枚举值、关系图 | 后端开发者 |
| [系统现状与风险清单](./系统现状与风险清单.md) | 当前运行前提、已收敛问题、高风险点、待验证项 | 联调 / 排障 / 接手开发者 |

### 功能规格（Feature Specifications）

> 每个功能模块的业务规则、状态机、约束条件、API 接口。  
> 索引与阅读指南：[features/README.md](./features/README.md)

| 文档 | 覆盖模块 |
|------|---------|
| [features/auth.md](./features/auth.md) | 认证流程、Token 机制、角色定义、权限矩阵、Admin-only 接口清单 |
| [features/master-data.md](./features/master-data.md) | 商品、客户、供应商的 CRUD 规则与约束 |
| [features/inbound-order.md](./features/inbound-order.md) | 入库单状态机（草稿→完成→作废）、库存联动、数据快照 |
| [features/outbound-order.md](./features/outbound-order.md) | 出库单状态机、双重库存校验、与入库单差异对比 |
| [features/inventory.md](./features/inventory.md) | StockFlowService 统一入口、库存变更场景、流水审计不可篡改 |
| [features/ai-recognition.md](./features/ai-recognition.md) | AI 识别状态机、OCR 流程、商品匹配逻辑、确认规则 |
| [features/desktop-client.md](./features/desktop-client.md) | 桌面端启动流程、已实现功能、目录结构、依赖接口 |
| [features/frontend-order-pages.md](./features/frontend-order-pages.md) | 单据页前端架构：composables 层、组件接口、ERP 扩展路线 |

### 架构决策记录（ADR）

> 记录重大架构决策的背景、决策内容、权衡与备选方案。

| 文档 | 说明 |
|------|------|
| [adr/README.md](./adr/README.md) | ADR 索引与模板 |
| [ADR-001](./adr/ADR-001-mybatis-over-jpa.md) | 使用 MyBatis 而非 JPA/Hibernate |
| [ADR-002](./adr/ADR-002-uuid-token-sessions.md) | 使用 UUID Token + 数据库会话而非 JWT |
| [ADR-003](./adr/ADR-003-python-ocr-service.md) | OCR 识别作为独立 Python 服务 |
| [ADR-004](./adr/ADR-004-stock-flow-single-entry.md) | StockFlowService 作为库存变更唯一入口 |
| [ADR-005](./adr/ADR-005-snapshot-pattern.md) | 订单明细使用数据快照模式 |
| [ADR-006](./adr/ADR-006-ai-draft-not-direct-order.md) | AI 识别结果不直接生成正式单据 |

### 开发指南

| 文档 | 说明 |
|------|------|
| [guides/setup.md](./guides/setup.md) | 本地环境搭建：数据库初始化、后端/前端/AI/桌面端启动 |
| [guides/testing.md](./guides/testing.md) | 测试策略：单元测试、API 测试、回归清单 |
| [guides/branch-conventions.md](./guides/branch-conventions.md) | 分支命名、提交信息格式、合并原则、版本标签 |
| [guides/codex-guide.md](./guides/codex-guide.md) | AI 辅助开发工具的行为约定 |

### 项目管理

| 文档 | 说明 |
|------|------|
| [项目路线图](./项目路线图.md) | 系统定位演进、已完成功能清单、版本节点 |
| [devlog/README.md](./devlog/README.md) | 开发过程日志索引（日报 / 阶段记录） |
| [BACKLOG](./BACKLOG.md) | P0-P3 待办、技术债务、验收标准、规模估算 |
| [../CHANGELOG.md](../CHANGELOG.md) | 版本变更记录（Keep a Changelog 格式） |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | 贡献指南：代码规范、测试要求、PR 流程 |

---

## 项目模块目录

| 目录 | 说明 | 端口 |
|------|------|------|
| `backend/` | Spring Boot 后端主工程 | 8080 |
| `frontend/` | Vue 3 Web 前端 | 5173 |
| `desktop-client/` | JavaFX 桌面客户端 | — |
| `python-ai-service/` | Python FastAPI + PaddleOCR AI 服务 | 9000 |
| `sql/` | 数据库初始化与演示数据脚本（历史迁移参考） | — |
| `docs/` | 工程文档（本目录） | — |

---

## Swagger API 文档

后端运行后访问：`http://localhost:8080/swagger-ui.html`  
登录后在 Authorize 中填入 token，即可调试全部接口。

---

## 文档维护约定

| 约定 | 说明 |
|------|------|
| **描述现状** | 所有活文档只描述已实现的功能，不包含规划内容 |
| **代码为准** | 文档与代码不一致时以代码为准，并及时更新文档 |
| **功能修改同步更新** | 修改业务逻辑后必须同步更新对应的 `features/*.md` |
| **权限变更同步更新** | 新增 Admin-only 接口时必须同步更新 `features/auth.md` 接口清单 |
