# 开发 Backlog

> 记录当前已知的待开发项、问题修复和技术改进。
> 已完成的功能不在此处记录，请看各 [功能规格文档](./features/)。
> 最后更新：2026-04

---

## P0 — 影响基本可用性，优先修复

| # | 类型 | 描述 | 涉及位置 |
|---|------|------|---------|
| 1 | 工程化 | 历史 `sql/V1_x` 增量已迁入 Flyway；后续数据库变更需只在 `db/migration` 新增版本脚本 | `backend/src/main/resources/db/migration` |
| 2 | 启动稳定性 | （已完成）Flyway 历史脚本 MySQL 兼容写法已完成一轮统一；后续仅补新增场景回归验证 | `backend/src/main/resources/db/migration` |
| 3 | 工程化 | （已完成）启动期业务服务直接改表逻辑已移除，结构变更收口到 Flyway | `service/impl/ProductServiceImpl.java` |
| 4 | 配置治理 | 收敛 `application.yaml`、`application-local.yml`、Docker Compose 的配置边界，避免 profile 和环境变量覆盖关系混乱 | `backend/src/main/resources/*.yml`、根目录 `docker-compose.yml` |
| 5 | 测试治理 | （阶段完成）已移除环境耦合的 contextLoads；后续再补独立 test profile 与分层测试策略 | `backend/src/test`、`backend/src/main/resources` |

---

## P1 — 功能不完整，影响演示或核心体验

| # | 类型 | 描述 | 涉及位置 |
|---|------|------|---------|
| 1 | 前后端契约 | （已完成）商品与客户编辑页 `status/code` 契约已对齐；后续补边界场景回归用例 | 前端 `views/product|customer/`、后端对应 Mapper XML |
| 2 | AI 能力口径 | （已完成）前端上传限制与 Python AI 能力已统一为图片；后续再补 PDF 路线评估 | 前端 `components/order/`、`python-ai-service/app.py`、AI 文档 |
| 3 | 列表性能 | AI 入库 / 出库历史当前未分页，需补分页接口与页面能力 | 后端 `AiRecognitionRecordMapper.xml`、前端 `views/ai/` |
| 4 | Web 能力闭环 | 供应商管理在后端和桌面端较完整，但 Web 端仍缺页面、菜单和完整 API 接入 | 前端 `router/`、`views/`、`api/supplier.js` |
| 5 | 功能缺失 | AI 出库识别历史列表页与详情页已补齐；后续可继续补筛选条件与分页参数 | `AiAssistController`、前端 `views/ai/` |
| 6 | 功能缺失 | 用户管理前端页面已补第一版（列表/新增/编辑/删除），后续可补重置密码独立流程与操作日志 | `frontend/views/user/` |
| 7 | 稳定性 | AI 服务调用已支持可配置超时；后续可按环境细化不同接口超时策略 | `config/RestTemplateConfig.java` |
| 8 | 业务规则 | 已限制停用客户/供应商被新订单引用；后续可补“历史草稿在确认前二次校验主数据状态” | `OutboundOrderServiceImpl`、`InboundOrderServiceImpl` |
| 9 | 业务规则 | 删除客户/供应商已增加关联订单校验（含历史名称快照兜底）；后续可补数据库 FK 约束统一防护 | `CustomerServiceImpl`、`SupplierServiceImpl` |

---

## P2 — 工程化改进，不影响功能

| # | 类型 | 描述 | 涉及位置 |
|---|------|------|---------|
| 9 | 代码质量 | `AiRecognitionServiceImpl` 已完成第一阶段拆分（OCR/匹配/单据组装/草稿持久化）；后续继续拆分记录校验与VO组装 | `service/impl/AiRecognitionServiceImpl.java`、`service/ai/*` |
| 10 | 代码质量 | `InboundOrderServiceImpl` 与 `OutboundOrderServiceImpl` 已完成公共基类抽取第一步（权限/分页/导出工具函数）；后续可继续抽”草稿保存与更新模板流程” | `service/impl/AbstractOrderServiceSupport.java`、两个 ServiceImpl |
| 11 | 可观测性 | Dashboard 已接入 `@Cacheable` + Caffeine（30s TTL）；后续可按场景补 `@CacheEvict` 精细失效策略 | `DashboardServiceImpl`、`application.yaml` |
| 12 | 工程化 | 已补首批核心 Service Mockito 测试（入库/出库确认与作废库存联动）；后续继续补草稿编辑、异常分支与库存不足场景 | `backend/src/test/java/com/yocaihua/wms/service/impl/*Test.java` |
| 13 | 工程化 | 已补 Docker Compose 首版（MySQL + 后端 + Python AI 一键启动）；后续可补前端服务、镜像分层优化与生产参数模板 | 根目录 `docker-compose.yml`、`backend/Dockerfile`、`python-ai-service/Dockerfile` |
| 14 | 前端 | 已统一用户会话状态到 Pinia `auth` store（token/username/nickname/role）；后续可继续将权限判断从 `utils/auth` 迁移为组件内直接消费 store | `frontend/src/stores/auth.js`、`frontend/src/utils/auth.js` |
| 15 | 前端重构 | **[阶段1]** 抽取前端逻辑层：从 InboundCreateView/OutboundCreateView 提取 composables（useOrderForm/useOrderItems/useOrderCalc/useOrderValidation/useProductSearch）和工具函数（orderHelper.js/printUtils.js），功能不变 | `frontend/src/composables/`（新建） |
| 16 | 前端重构 | **[阶段2]** 重构入库单创建页：InboundCreateView.vue 从 1564 行降至 ~180 行，拆出 OrderItemTable、AiRecognitionDialog、QuickCreateDialog 组件 | `frontend/src/views/inbound/InboundCreateView.vue`、`frontend/src/components/order/` |
| 17 | 前端重构 | **[阶段3]** 重构出库单创建页：OutboundCreateView.vue 复用阶段2全部 composables + 组件，降至 ~150 行 | `frontend/src/views/outbound/OutboundCreateView.vue` |
| 18 | 前端重构 | **[阶段4]** 独立打印模板：提取 printAdapter.js + PrintTemplate.vue，InboundPrintView/OutboundPrintView 各降至 ~60 行，消除 toChineseAmount/buildPrintItems 重复代码 | `frontend/src/utils/printAdapter.js`、`frontend/src/components/print/` |
| 19 | 前端重构 | **[阶段5]** 补充增强：DetailView 共享组件、OrderSummary 汇总区、草稿离开提示（onBeforeRouteLeave） | `frontend/src/views/inbound/InboundDetailView.vue` 等 |

---

## P3 — 功能增强，MVP 后考虑

| # | 类型 | 描述 |
|---|------|------|
| 15 | 功能增强 | Token 可切换 Redis 持久化会话（`auth.session-store=redis`）；后续可继续评估 JWT 无状态化改造 |
| 16 | 功能增强 | 已补低库存预警通知首版（定时检查 + 手动触发 + 邮件/Webhook 渠道）；后续可补企业微信/钉钉签名协议与通知记录持久化 |
| 17 | 功能增强 | 已补首页近7天入库/出库趋势折线图（后端 `/dashboard/trend` + 前端首页 SVG 渲染）；后续可补金额趋势与多时间窗口切换 |
| 18 | 功能增强 | AI 识别商品匹配增强：引入编辑距离 / 向量相似度替代纯字符串匹配 |
| 19 | 功能增强 | 操作日志（记录用户的关键操作，如登录、确认、作废） |
| 20 | 功能增强 | 桌面端停止服务功能（设置页当前只能启动，不能停止） |

---

## 已完成（近期）

| # | 类型 | 描述 | 涉及位置 |
|---|------|------|---------|
| D1 | 安全 | Token 会话已支持可切换持久化（DB / Redis）；默认 DB，设置 `auth.session-store=redis` 后改为 Redis TTL 会话 | `common/TokenStore.java`、`interceptor/LoginInterceptor.java` |
| D2 | 性能 | 列表分页已增加 `pageSize` 上限（`MAX_PAGE_SIZE=200`），避免超大分页全表查询 | `service/impl/*ServiceImpl.java` |
| D3 | 架构 | AI 识别服务已完成首轮职责拆分：`OcrAdapterService`、`ProductMatchService`、`AiOrderAssemblerService`、`AiDraftPersistenceService` | `service/ai/*`、`service/impl/AiRecognitionServiceImpl.java` |
| D4 | 架构 | 入库/出库单服务已抽取公共支撑基类 `AbstractOrderServiceSupport`，统一权限校验、分页归一化、导出工具函数 | `service/impl/AbstractOrderServiceSupport.java`、`InboundOrderServiceImpl`、`OutboundOrderServiceImpl` |
| D5 | 功能 | 低库存预警通知首版已落地：支持定时扫描、管理员手动触发、邮件/Webhook 渠道与冷却去重 | `service/impl/LowStockAlertServiceImpl.java`、`StockController.java` |

---

## 已知 Bug / 边界问题

| # | 描述 | 严重程度 | 状态 |
|---|------|---------|------|
| B1 | 创建出库草稿后、确认前，若其他单据先消耗了同一商品库存，确认时会报库存不足（属预期行为）；提示已优化为“单号+商品+需出库+可用库存”可读文案 | 低 | 已优化 |
| B2 | CORS 默认来源已改为 `localhost/127.0.0.1` 任意端口，并支持 `APP_CORS_ALLOWED_ORIGIN_PATTERNS` 环境变量覆盖 | 低 | 已优化 |
| B3 | 已确认 `sourceType` 通过 `ai_recognition_record` 反查推导（`InboundOrderMapper` 的 `EXISTS/子查询`）；并补充组合索引 `idx_doc_type_confirmed_order_id` 优化该链路 | 低 | 已确认并优化 |
