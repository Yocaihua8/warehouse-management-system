# 贡献指南

感谢参与本项目的开发。本文档说明开发流程、代码规范和提交要求。

---

## 目录

1. [前置条件](#1-前置条件)
2. [开发环境搭建](#2-开发环境搭建)
3. [分支与提交规范](#3-分支与提交规范)
4. [代码规范](#4-代码规范)
5. [测试要求](#5-测试要求)
6. [文档要求](#6-文档要求)
7. [Pull Request 流程](#7-pull-request-流程)
8. [架构决策记录（ADR）](#8-架构决策记录adr)

---

## 1. 前置条件

| 工具 | 版本 | 用途 |
|------|------|------|
| Java JDK | 17+ | 后端 + 桌面端 |
| Maven | 3.8+ | 构建 |
| Node.js | 18+ | Web 前端 |
| MySQL | 8.x | 数据库 |
| Python | 3.9+ | AI 服务（可选） |
| Git | — | 版本管理 |

---

## 2. 开发环境搭建

详见 [docs/guides/setup.md](./docs/guides/setup.md)。

关键步骤：

```bash
# 1. 数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4;"

# 2. 后端（Flyway 自动建表）
cd backend && mvn spring-boot:run

# 3. 前端
cd frontend && npm install && npm run dev

# 4. AI 服务（可选）
cd python-ai-service && .\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 9000
```

---

## 3. 分支与提交规范

完整规范见 [docs/guides/branch-conventions.md](./docs/guides/branch-conventions.md)。

### 分支命名

```
feature/<description>    新功能
fix/<description>        问题修复
refactor/<description>   重构
docs/<description>       文档更新
chore/<description>      维护、依赖、配置
```

### 提交信息格式

```
<type>: <简要描述>

[可选正文 - 说明 why，不是 what]

[可选 footer - 关联 Issue: #123]
```

| type | 场景 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（不影响功能） |
| `docs` | 文档变更 |
| `test` | 测试相关 |
| `chore` | 依赖、构建、配置 |
| `perf` | 性能优化 |

**禁止使用**：`update`、`修改`、`继续`、`bugfix`、`fix bug` 等模糊描述。

---

## 4. 代码规范

### 后端（Java）

- **分层原则**：Controller 不含业务逻辑，Service 不直接操作 Mapper（通过接口）
- **库存变更**：所有库存变更必须经过 `StockFlowService`，禁止直接 UPDATE `stock` 表
- **权限校验**：Admin-only 操作须同时在 `LoginInterceptor` 路径列表和 Service 层 `ensureAdminPermission()` 中双重保护
- **事务边界**：草稿编辑（delete-then-insert）必须在 `@Transactional` 内执行
- **异常处理**：业务异常使用 `BusinessException`，由 `GlobalExceptionHandler` 统一返回 `Result.error(msg)`
- **命名**：DTO 以 `DTO` 结尾，VO 以 `VO` 结尾，Service 实现类以 `Impl` 结尾

### 前端（Vue 3）

- **状态管理**：页面可编辑性统一读 `pageMode`（来自 `useOrderForm`），禁止直接判断 `orderStatus`
- **业务逻辑**：放在 composable 层（`composables/`），View 只负责组合和渲染
- **组件通信**：父子通过 props/emits，兄弟通过 composable 共享状态，禁止跨组件直接修改
- **API 调用**：统一在 composable 层调用 `api/` 目录下的接口函数，View 中禁止直接 import axios

### 数据库变更

- 所有结构变更（建表、加字段、加索引）必须新增 `backend/src/main/resources/db/migration/V{n}__description.sql`
- **禁止**修改已执行的历史 Flyway 脚本
- 脚本命名：`V10__add_supplier_status.sql`（下划线分隔，描述用英文）

---

## 5. 测试要求

完整测试策略见 [docs/guides/testing.md](./docs/guides/testing.md)。

### 提交前最低要求

| 场景 | 要求 |
|------|------|
| 修改 Service 层核心业务逻辑 | 必须包含对应单元测试（Mockito） |
| 新增 Admin-only 接口 | 必须同步更新 `auth.md` 接口清单 |
| 修改库存变更逻辑 | 必须包含库存增减和回滚的测试用例 |
| 新增 Flyway 迁移脚本 | 必须在本地全量迁移验证（空库 + 已有库） |

### 运行测试

```bash
# 后端单元测试（无需数据库）
cd backend && mvn test

# 前端单元测试
cd frontend && npm test

# 前端构建校验
cd frontend && npm run build

# 仅验证编译（跳过测试）
cd backend && mvn compile -DskipTests
```

---

## 6. 文档要求

| 变更类型 | 需同步更新的文档 |
|---------|---------------|
| 新增/修改业务逻辑 | 对应 `docs/features/*.md` |
| 新增 Admin-only 接口 | `docs/features/auth.md` 接口清单 |
| 重大架构决策 | `docs/adr/` 新增 ADR |
| 版本发布 | `CHANGELOG.md` |
| 新增已知问题或 Bug | `docs/BACKLOG.md` |

**文档原则**：文档只描述当前已实现状态，不包含规划内容。与代码不一致时以代码为准，并立即更新文档。

---

## 7. Pull Request 流程

### 提交 PR 前的自查清单

- [ ] 功能目标已完成，本地主流程走通
- [ ] 相关单元测试已通过（`mvn test`）
- [ ] 前端改动已执行 `npm test` / `npm run build`
- [ ] 无明显编译警告
- [ ] 不含调试代码（`console.log`、硬编码测试数据）
- [ ] 相关文档已同步更新
- [ ] `CHANGELOG.md` 已在 `[Unreleased]` 区记录本次变更

### GitHub CI 与合并保护

仓库已提供 GitHub Actions 工作流：

- `backend-test`
- `frontend-test`

PR 提交后，这两个 job 会自动执行。  
如果希望测试失败时禁止合并，还需要由仓库维护者在 GitHub `Settings > Branches / Rulesets` 中，将这两个 job 配置为 **required checks**。
对于新建仓库，GitHub 只有在这两个 job 至少成功运行过一次后，才会在 Ruleset 的检查项列表中显示它们。

### PR 描述模板

```markdown
## 变更说明
简述本次改动的目的和内容。

## 测试验证
- [ ] 本地主流程走通
- [ ] 单元测试通过
- [ ] 边界场景验证（如：库存不足、权限拦截等）

## 关联文档
- 涉及的功能规格文档：...
- 涉及的 ADR：...

## 已知限制
（如有，列出本次未处理的边界情况）
```

### 合并原则

- `feature/*` → `dev`：功能分支完成后合入开发集成分支
- `dev` → `main`：核心流程可完整跑通后合入，同步更新 `CHANGELOG.md` 和版本号

---

## 8. 架构决策记录（ADR）

重大架构决策必须记录在 `docs/adr/` 目录，格式见 [docs/adr/README.md](./docs/adr/README.md)。

触发条件：
- 技术选型（框架、库、存储方案）
- 影响多个模块的设计模式
- 有多个合理备选方案、需要记录"为什么选这个"的决策
- 推翻或替代之前的决策
