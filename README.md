# ERP + WMS 一体化系统

面向中小型仓储与进销存场景的管理系统，从**轻量 WMS** 出发，向**轻 ERP + WMS 一体化**演进。

**技术栈**：Spring Boot 3 · Vue 3 · MyBatis · MySQL 8 · Python FastAPI · JavaFX

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│  Web 前端（Vue 3 + Element Plus，port 5173）              │
│  JavaFX 桌面端（Java 17，本地部署）                        │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / REST
                       ▼
┌─────────────────────────────────────────────────────────┐
│  Spring Boot 后端（port 8080）                            │
│  认证拦截 → Controller → Service → MyBatis → MySQL        │
│                              ↓                           │
│                   RestTemplate → Python AI（port 9000）   │
└─────────────────────────────────────────────────────────┘
```

**模块目录**：

| 目录 | 说明 | 端口 |
|------|------|------|
| `backend/` | Spring Boot 后端 | 8080 |
| `frontend/` | Vue 3 Web 前端 | 5173 |
| `desktop-client/` | JavaFX 桌面客户端 | — |
| `python-ai-service/` | FastAPI + PaddleOCR | 9000 |
| `sql/` | 数据库初始化脚本（演示数据） | — |
| `docs/` | 工程文档 | — |

---

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
docker compose up -d --build
```

验证：
- 后端：`http://localhost:8080/system/health`
- AI 服务：`http://localhost:9000/health`
- 前端：需单独启动（见下方）

### 方式二：本地手动启动

**前提**：MySQL 8 已启动，创建空库：

```sql
CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4;
```

按以下顺序启动：

```bash
# 1. 后端（Flyway 自动建表）
cd backend && mvn spring-boot:run

# 2. Python AI 服务（可选，仅 AI 识别功能需要）
cd python-ai-service
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 9000

# 3. Web 前端
cd frontend && npm install && npm run dev

# 4. 桌面端
cd desktop-client && mvn javafx:run
```

完整环境搭建说明见 [docs/guides/setup.md](./docs/guides/setup.md)。

### 默认演示账号

> 需先执行 `sql/demo_seed_mvp.sql` 导入演示数据。

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `123456` | 管理员（ADMIN） |
| `operator` | `123456` | 操作员（OPERATOR） |

---

## 核心功能

| 模块 | 能力 |
|------|------|
| 用户认证 | 登录/登出，UUID Token，ADMIN/OPERATOR 双角色 |
| 商品管理 | CRUD + 自定义字段 + 删除前置校验 |
| 客户/供应商 | CRUD + 停用拦截 + 删除关联校验 |
| 入库单 | 草稿→确认→作废，库存联动，数据快照，AI 来源 |
| 出库单 | 创建时即校验库存，双重校验，库存联动 |
| 库存管理 | `StockFlowService` 统一入口，流水审计不可篡改 |
| AI 识别 | PaddleOCR 图片识别→商品匹配→人工确认→生成单据 |
| 数据看板 | 近 7 天趋势折线图，聚合统计 |
| 导出打印 | Excel/CSV/PDF 导出，浏览器打印 |

---

## 文档

| 文档 | 说明 |
|------|------|
| [docs/README.md](./docs/README.md) | 文档导航总入口 |
| [docs/架构设计说明.md](./docs/架构设计说明.md) | 系统架构、技术栈、关键设计决策 |
| [docs/adr/](./docs/adr/README.md) | 架构决策记录（ADR） |
| [docs/features/](./docs/features/README.md) | 各业务模块功能规格 |
| [docs/guides/setup.md](./docs/guides/setup.md) | 本地开发环境搭建 |
| [docs/guides/testing.md](./docs/guides/testing.md) | 测试策略与运行方式 |
| [docs/BACKLOG.md](./docs/BACKLOG.md) | 开发待办与优先级 |
| [CHANGELOG.md](./CHANGELOG.md) | 版本变更记录 |
| [CONTRIBUTING.md](./CONTRIBUTING.md) | 贡献指南与开发规范 |

---

## 参与开发

请先阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)，了解分支规范、代码规范、测试要求和 PR 流程。

```bash
# 运行后端单元测试
cd backend && mvn test

# 查看 API 文档（后端启动后）
open http://localhost:8080/swagger-ui.html
```
