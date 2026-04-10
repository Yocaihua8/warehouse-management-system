# 开发 Backlog

> **阅读约定**：本文件只记录**尚未完成**的事项。已完成的内容统一在底部"已完成"区。  
> 最后更新：2026-04

---

## P0 — 阻断性，优先处理

| # | 类型 | 描述 | 涉及位置 |
|---|------|------|---------|
| （暂无） | - | 当前无阻断性待办 | - |

---

## P1 — 功能缺口，影响演示或核心体验

| # | 类型 | 描述 | 涉及位置 |
|---|------|------|---------|
| （暂无） | - | 当前无 P1 级功能缺口待办 | - |

---

## P2 — 工程化改进

| # | 类型 | 描述 | 涉及位置 |
|---|------|------|---------|
| （暂无） | - | 当前无 P2 级代码质量待办 | - |

---

## P3 — 功能增强，MVP 后考虑

| # | 类型 | 描述 |
|---|------|------|
| 8 | 功能增强 | Token 无状态化改造：评估 JWT 方案，替代当前 DB/Redis 会话存储 |
| 9 | 功能增强 | AI 商品匹配增强：引入编辑距离或向量相似度，替代当前纯字符串匹配 |
| 10 | 功能增强 | 桌面端停止服务：设置页当前只能启动服务，不能停止 |
| 11 | 功能增强 | 用户重置密码独立流程（当前编辑用户可直接改密码，无单独入口） |

---

## 已知 Bug

| # | 描述 | 严重程度 |
|---|------|---------|
| （暂无） | - | - |

---

## 已完成

### 安全 / 认证
| # | 描述 | 涉及位置 |
|---|------|---------|
| D1 | Token 会话支持可切换持久化（DB 默认 / Redis TTL），通过 `AUTH_SESSION_STORE` 环境变量切换 | `common/TokenStore.java`、`interceptor/LoginInterceptor.java` |
| D23 | 用户管理接口权限收口：`/user/add`、`/user/update`、`/user/delete/{id}` 在 `LoginInterceptor` 中纳入 Admin-only 校验，阻断普通用户越权调用 | `interceptor/LoginInterceptor.java` |

### 后端架构
| # | 描述 | 涉及位置 |
|---|------|---------|
| D2 | AI 识别服务职责拆分：`OcrAdapterService`、`ProductMatchService`、`AiOrderAssemblerService`、`AiDraftPersistenceService` | `service/ai/`、`AiRecognitionServiceImpl.java` |
| D3 | 入库/出库服务抽取公共基类 `AbstractOrderServiceSupport`，统一权限校验、分页归一化、导出工具函数 | `service/impl/AbstractOrderServiceSupport.java` |
| D4 | 补充 `flyway-mysql` 依赖，解决 Flyway 10+ 不支持 MySQL 8.0 的启动报错 | `backend/pom.xml` |
| D28 | `AiRecognitionServiceImpl` 进一步拆分：将“记录校验/确认入参校验”下沉到 `AiRecognitionValidationService`，将“识别结果与编辑草稿 VO 组装”下沉到 `AiRecognitionVoAssemblerService`，主服务仅保留流程编排 | `service/impl/AiRecognitionServiceImpl.java`、`service/ai/AiRecognitionValidationService.java`、`service/ai/AiRecognitionVoAssemblerService.java` |

### 功能
| # | 描述 | 涉及位置 |
|---|------|---------|
| D5 | 低库存预警通知：定时扫描 + 手动触发 + 邮件/Webhook 渠道 + 冷却去重 | `LowStockAlertServiceImpl.java`、`StockController.java` |
| D6 | 首页近 7 天入库/出库趋势折线图（后端 `/dashboard/trend` + 前端 SVG 渲染） | `DashboardServiceImpl.java`、`HomeView.vue` |
| D7 | 操作日志：记录用户关键操作（登录、确认、作废等），Admin-only 查询 | `OperationLogController.java`、`OperationLogView.vue` |
| D8 | 用户管理 Web 端首版：列表/新增/编辑/删除 | `UserController.java`、`views/user/UserListView.vue` |
| D9 | AI 出库识别历史列表页与详情页 | `AiAssistController.java`、`views/ai/AiOutboundRecordListView.vue` |
| D10 | 停用客户/供应商后阻止新订单引用；删除时校验关联订单（含历史名称快照兜底） | `CustomerServiceImpl.java`、`SupplierServiceImpl.java` |
| D24 | AI 入库/出库历史分页补齐：后端 `/ai/inbound/list`、`/ai/outbound/list` 改为 `PageResult` 分页返回，前端 `views/ai` 接入 `pageNum/pageSize/total` 与分页器，消除全量加载风险 | `AiAssistController.java`、`AiRecognitionService*.java`、`AiRecognitionRecordMapper*.xml`、`views/ai/*RecordListView.vue` |
| D25 | 供应商管理 Web 端补齐：新增供应商列表/新增/编辑页面，打通分页查询、详情加载、修改与删除链路，并接入主导航与路由 | `frontend/src/views/supplier/*`、`frontend/src/api/supplier.js`、`router/index.js`、`layouts/MainLayout.vue` |
| D26 | ERP 单据工作台体验增强（阶段5）：入库/出库明细默认预置 8 行空行；合计行内嵌至明细表末行并与数量/金额列对齐；操作区按钮补充快捷键标注；出库明细按商品回填库存余量 | `components/order/OrderItemTable.vue`、`views/inbound/InboundCreateView.vue`、`views/outbound/OutboundCreateView.vue`、`composables/useOrderForm.js`、`useOutboundCreatePage.js` |

### 工程化
| # | 描述 | 涉及位置 |
|---|------|---------|
| D11 | 列表分页增加 `pageSize` 上限（`MAX_PAGE_SIZE=200`），防止超大分页全表查询 | `service/impl/*ServiceImpl.java` |
| D12 | Docker Compose 首版：MySQL + Redis + Python AI + 后端一键启动，含健康检查 | `docker-compose.yml`、`backend/Dockerfile`、`python-ai-service/Dockerfile` |
| D13 | 后端核心 Service Mockito 测试首批：入库/出库确认与作废的库存联动 | `backend/src/test/java/com/yocaihua/wms/service/impl/` |
| D14 | Flyway 历史脚本 MySQL 兼容写法统一；启动期直接改表逻辑移除，结构变更收口到 Flyway | `db/migration/`、`ProductServiceImpl.java` |
| D15 | Dashboard 接入 Caffeine 缓存（30s TTL），减少高频查询压力 | `DashboardServiceImpl.java`、`application.yaml` |
| D21 | 配置治理收敛：`application.yaml` 作为统一配置键与默认值入口；`application-local.yml` 仅保留 local profile 覆盖；`docker-compose.yml` 显式 `SPRING_PROFILES_ACTIVE=local` | `backend/src/main/resources/application*.yml`、`docker-compose.yml` |
| D27 | 后端单测补齐：新增“入库/出库草稿编辑成功”用例、“确认出库库存不足异常”用例，以及 AI 入库/出库识别异常路径（`markFailed` + 异常透传）用例 | `backend/src/test/java/com/yocaihua/wms/service/impl/*Test.java` |

### 前端
| # | 描述 | 涉及位置 |
|---|------|---------|
| D16 | Pinia auth store 上线，统一管理 token/username/nickname/role | `stores/auth.js` |
| D17 | 商品/客户编辑页前后端 `status`/`code` 契约对齐 | `views/product|customer/`、Mapper XML |
| D18 | 前端上传限制与 Python AI 能力统一为图片格式 | `components/order/`、`python-ai-service/app.py` |
| D19 | AI 服务调用支持可配置超时（`connect-timeout-ms`、`read-timeout-ms`） | `config/RestTemplateConfig.java`、`application.yaml` |
| D20 | **前端单据页重构（阶段1–4）**：建立 composables 层（9个）、通用组件层（7个）、工具层（4个）、打印三层架构；CreateView 1564/1231 行→145/149 行，PrintView 355/315 行→66/56 行 | `composables/`、`components/order/`、`components/print/`、`utils/` |
| D22 | **前端单据页重构（阶段2收口）**：入库/出库创建页对齐 ERP 工作台四区（单据头/明细/汇总/操作区），补齐 `保存并新建/清空/提交确认/打印预览` 按钮状态规则，并统一明细表事件命名（`row-add/row-insert/row-delete/product-selected`） | `views/inbound/InboundCreateView.vue`、`views/outbound/OutboundCreateView.vue`、`components/order/OrderItemTable.vue`、`composables/use*CreatePage.js` |
