# 开发 Backlog

> **阅读约定**：
> - 本文件只记录**尚未完成**的事项，已完成内容在底部"已完成"区
> - 规模估算：`XS`<半天 · `S`<1天 · `M`<3天 · `L`<1周 · `XL`>1周
> - 最后更新：2026-04-16

---

## P0 — 阻断性，立即处理

| # | 描述 | 验收标准 | 规模 |
|---|------|---------|:----:|
| （暂无） | 当前无阻断性问题 | — | — |

---

## P1 — 功能缺口，影响核心体验

| # | 描述 | 验收标准 | 规模 | 里程碑 |
|---|------|---------|:----:|--------|
| （暂无） | 当前无 P1 功能缺口 | — | — | — |

---

## P2 — 工程化改进

| # | 描述 | 验收标准 | 规模 | 里程碑 |
|---|------|---------|:----:|--------|
| 6 | **`OrderItemTable` 瘦身（按需求驱动再评估）**：当前轮次已收口在”焦点/键盘流 composable 抽离 + 商品选择列子组件拆分”；商品编码/名称/规格/单位与数量/单价列暂不继续抽象，避免为简单模板引入过度参数化，以及触发 `el-input-number` 焦点复杂度回升 | 仅在新增交互或列级需求出现时，再评估是否继续拆分剩余列 | — | 按需 |

---

## P3 — 功能增强，v2.0 后评估

| # | 描述 | 规模 |
|---|------|:----:|
| 6 | **Token JWT 化**：评估 JWT 方案，替代当前 DB/Redis 会话存储；需处理主动失效（Deny List）机制 | L |
| 7 | **AI 商品匹配增强**：引入编辑距离或向量相似度，替代当前纯字符串匹配；增加置信度分级展示 | L |
| 8 | **桌面端停止服务**：系统设置页当前只能启动服务，增加停止服务、进程状态可见性 | S |
| 9 | **用户密码独立重置流程**：当前编辑用户可直接改密码，无独立重置入口 | S |
| 10 | **Testcontainers 集成测试**：使用 Testcontainers 启动真实 MySQL，测试 Flyway 迁移链 + Mapper 层 | M |
| 11 | **自定义字段扩展到客户/供应商**：当前 `customFieldsJson` 仅商品支持；向客户、供应商实体扩展，需 Flyway 新增字段 + CRUD 接口调整 + 前端键值编辑器复用 | M |
| 12 | **字段定义管理（完整自定义字段方案）**：Admin 可在后台定义字段模板（字段名、类型：文本/数字/日期/下拉、是否必填），商品/客户/供应商表单自动渲染对应字段；需新增 `custom_field_definition` 表；是 ERP 方向"用户可扩展数据模型"的核心能力 | XL |

---

## 技术债务

| # | 描述 | 影响 | 规模 |
|---|------|------|:----:|
| T1 | 配置来源不够收口：`application.yaml` / `application-local.yml` / `docker-compose.yml` 三套来源并存，本地手工启动与 Compose 启动默认值不同（会话存储、数据库密码）；新人上手需额外对照 | 新人上手困难，本地与 Docker 行为差异 | S |
| T3 | 前端 E2E / 浏览器级联调仍缺自动化保障：composable、关键组件与创建页保存链 happy/error path 已覆盖；当前主要空白已收敛为浏览器级保存链、AI 识别弹窗确认成单链与跨页面回归 | 前端回归覆盖面仍不完整 | S |
| T4 | Web 与桌面端能力仍有差异，桌面端供应商管理缺编辑/删除 | 功能覆盖不一致 | M |

---

## 已知 Bug

| # | 描述 | 严重程度 |
|---|------|:-------:|
| （暂无） | 当前无已知 Bug | — |

---

## 已完成

### 安全 / 认证
| # | 描述 | 涉及位置 |
|---|------|---------|
| D1 | Token 会话支持 DB/Redis 可切换持久化（`AUTH_SESSION_STORE` 环境变量） | `TokenStore.java`、`LoginInterceptor.java` |
| D23 | 用户管理接口权限收口：`/user/add|update|delete` 纳入 Admin-only 校验 | `LoginInterceptor.java` |

### 后端架构
| # | 描述 | 涉及位置 |
|---|------|---------|
| D2 | AI 识别服务职责拆分：OcrAdapter / ProductMatch / Assembler / Persistence | `service/ai/` |
| D3 | 入库/出库公共基类 `AbstractOrderServiceSupport`（权限校验/分页/导出） | `service/impl/AbstractOrderServiceSupport.java` |
| D4 | 补充 `flyway-mysql` 依赖，解决 Flyway 10+ 不支持 MySQL 8.0 | `backend/pom.xml` |
| D28 | `AiRecognitionServiceImpl` 进一步拆分 Validation / VoAssembler 子服务 | `service/ai/` |

### 功能
| # | 描述 | 涉及位置 |
|---|------|---------|
| D5 | 低库存预警：定时扫描 + 手动触发 + 邮件/Webhook + 冷却去重 | `LowStockAlertServiceImpl.java` |
| D6 | 首页近 7 天趋势折线图（`/dashboard/trend` + 前端 SVG） | `DashboardServiceImpl.java`、`HomeView.vue` |
| D7 | 操作日志（记录关键操作，Admin-only 查询） | `OperationLogController.java` |
| D8 | 用户管理 Web 端首版（列表/新增/编辑/删除） | `UserController.java`、`UserListView.vue` |
| D9 | AI 出库识别历史列表页与详情页 | `AiOutboundRecordListView.vue` |
| D10 | 停用客户/供应商下单拦截 + 删除关联订单双重校验 | `CustomerServiceImpl.java`、`SupplierServiceImpl.java` |
| D24 | AI 入库/出库历史分页补齐（消除全量加载风险） | `AiAssistController.java`、`views/ai/` |
| D25 | 供应商管理 Web 端补齐（列表/新增/编辑/删除，接入导航） | `frontend/src/views/supplier/` |
| D26 | ERP 工作台体验增强（阶段5）：预置空行/合计行内嵌/操作区/库存余量 | `OrderItemTable.vue`、`use*CreatePage.js` |

### 工程化
| # | 描述 | 涉及位置 |
|---|------|---------|
| D11 | 列表分页 `pageSize` 上限（`MAX_PAGE_SIZE=200`） | `service/impl/*ServiceImpl.java` |
| D12 | Docker Compose 首版（MySQL + Redis + Python AI + 后端） | `docker-compose.yml` |
| D13 | 后端核心 Service Mockito 测试首批（入库/出库确认/作废库存联动） | `backend/src/test/java/` |
| D14 | Flyway 历史脚本 MySQL 兼容写法统一 | `db/migration/` |
| D15 | Dashboard Caffeine 缓存（30s TTL） | `DashboardServiceImpl.java` |
| D21 | 配置治理收敛：`application.yaml` 统一键，`application-local.yml` 仅本地覆盖 | `backend/src/main/resources/` |
| D27 | 后端单测补齐（草稿编辑、库存不足异常、AI 识别异常路径） | `backend/src/test/java/` |
| D29 | 文档同步：`frontend-order-pages.md`、`master-data.md` 与代码对齐 | `docs/features/` |

### 前端
| # | 描述 | 涉及位置 |
|---|------|---------|
| D16 | Pinia auth store（统一管理 token/username/nickname/role） | `stores/auth.js` |
| D17 | 商品/客户编辑页 `status/code` 前后端契约对齐 | `views/product|customer/` |
| D18 | 前端上传限制与 Python AI 统一为图片格式 | `components/order/` |
| D19 | AI 服务调用支持可配置超时 | `config/RestTemplateConfig.java` |
| D20 | **前端单据页重构（阶段1-4）**：composables 层（9个）/ 通用组件层（7个）/ 工具层（4个）/ 打印三层架构；CreateView 1564/1231 行→158/160 行 | `composables/`、`components/`、`utils/` |
| D22 | **前端单据页重构（阶段2收口）**：四区布局对齐，事件命名统一 | `views/`、`components/order/OrderItemTable.vue` |
| D30 | 商品列行内可编辑修复（选商品后可继续修正规格/单位） | `OrderItemTable.vue` |
| D31 | 列表页双左栏收敛，释放主表格宽度 | `InboundListView.vue`、`OutboundListView.vue` |
| D32 | 单据底部操作区重排（左辅右主） | `InboundCreateView.vue`、`OutboundCreateView.vue` |
| D33 | 工作台骨架落地：`order-workbench/` 目录 + 创建页拆分为路由壳 + 容器 | `components/order-workbench/`、`InboundOrderCreate.vue` |
| D34 | 提交链路统一：保存草稿→确认提交串联 | `useInboundCreatePage.js`、`useOutboundCreatePage.js` |
| D35 | `useOrderWorkbenchPage` 共享核心落地：入库/出库创建页收敛为“共享核心 + 薄适配层” | `useOrderWorkbenchPage.js`、`useInboundCreatePage.js`、`useOutboundCreatePage.js` |
| D36 | 商品弹窗选品首版：`OrderDetailTable` 托管 `ProductSelectDialog`，替换明细表内联 `el-select` | `OrderDetailTable.vue`、`OrderItemTable.vue`、`ProductSelectDialog.vue` |
| D37 | 键盘录入流首版：主录入列 `Tab` 顺序跳转，末格自动补新行并跳到下一行第一格 | `OrderItemTable.vue` |
| D38 | 保存并新建自动聚焦：保存成功后聚焦第一行“商品选择”，继续进入既有 `Tab` 主录入链 | `useOrderWorkbenchPage.js`、`OrderItemTable.vue`、`OrderDetailTable.vue`、`InboundOrderCreate.vue`、`OutboundOrderCreate.vue` |
| D39 | 行焦点高亮：编辑态明细表高亮当前录入行，并与既有 `Tab` 主录入链联动 | `OrderItemTable.vue` |
| D40 | 键盘录入流补齐 `Shift+Tab`：支持同一行反向回退，第一格可回退到上一行最后一格 | `OrderItemTable.vue` |
| D41 | 前端 Vitest 首批单测落地：`npm test` 可执行，覆盖 `useOrderCalc`、`useOrderValidation`、`useOrderItems` | `package.json`、`vite.config.js`、`src/composables/__tests__/` |
| D42 | 统一编辑/只读明细表实现：详情页切换到 `OrderItemTable`，通过显示开关复用同一组件；旧 `OrderDetailItemTable.vue` 暂保留待清理 | `OrderItemTable.vue`、`InboundDetailView.vue`、`OutboundDetailView.vue` |
| D43 | GitHub Actions CI 首版：新增 `.github/workflows/ci.yml`，PR 自动执行 `backend-test` 与 `frontend-test`；可配合 Required status checks 阻止失败测试合并 | `.github/workflows/ci.yml` |
| D44 | 后端 JaCoCo 覆盖率报告首版：`mvn verify` 生成 HTML 报告，当前先提供可视化覆盖率，不直接启用 80% fail threshold | `backend/pom.xml`、`backend/target/site/jacoco/` |
| D45 | `CustomerServiceImpl` Mockito 单测首版：补齐分页、详情、新增、修改、删除核心分支，单类行覆盖率提升到约 `57.85%` | `CustomerServiceImplTest.java` |
| D46 | `SupplierServiceImpl` Mockito 单测首版：补齐分页、详情、新增、修改、删除核心分支，单类行覆盖率提升到约 `52.81%` | `SupplierServiceImplTest.java` |
| D47 | `ProductServiceImpl` Mockito 单测首版：补齐分页、详情、新增、修改、删除与 `customFieldsJson`/库存初始化核心分支，单类行覆盖率提升到约 `65.89%` | `ProductServiceImplTest.java` |
| D48 | `StockServiceImpl` Mockito 单测首版：补齐分页、手工库存调整、默认操作人与默认原因等核心分支，单类行覆盖率提升到约 `37.36%` | `StockServiceImplTest.java` |
| D49 | `StockFlowServiceImpl` Mockito 单测首版：补齐入库/出库库存变更、作废回滚、手工调整日志与库存不足提示核心分支，单类行覆盖率提升到约 `93.01%` | `StockFlowServiceImplTest.java` |
| D50 | `LowStockAlertServiceImpl` Mockito 单测首版：补齐管理员手动触发、冷却去重、邮件/Webhook 渠道与定时检查核心分支，单类行覆盖率提升到约 `87.74%` | `LowStockAlertServiceImplTest.java` |
| D51 | `DashboardServiceImpl` Mockito 单测首版：补齐首页汇总计数、趋势参数校验、缺口日期补零与空行/空值处理，单类行覆盖率提升到 `100%` | `DashboardServiceImplTest.java` |
| D52 | `UserServiceImpl` Mockito 单测首版：补齐登录、当前用户、Admin-only 用户管理、默认管理员保护与当前用户自保护等核心分支，单类行覆盖率提升到约 `97.67%` | `UserServiceImplTest.java` |
| D53 | `OperationLogServiceImpl` Mockito 单测首版：补齐写日志归一化、写入失败容错与分页筛选参数归一化，单类行覆盖率提升到 `100%` | `OperationLogServiceImplTest.java` |
| D54 | `SystemServiceImpl` Mockito 单测首版：补齐系统健康检查、数据库/AI 状态汇总、AI 消息回退与 bootstrap 配置映射，单类行覆盖率提升到 `100%` | `SystemServiceImplTest.java` |
| D55 | `StockAdjustLogServiceImpl` Mockito 单测首版：补齐分页默认值、页大小上限、offset 计算与 `productName` 原样透传行为，单类行覆盖率提升到 `100%` | `StockAdjustLogServiceImplTest.java` |
| D56 | `AiRecognitionServiceImpl` Mockito 单测补齐成功识别路径：覆盖入库/出库 OCR 成功后草稿落库、商品匹配、识别成功状态更新与结果 VO 返回，单类行覆盖率提升到约 `26.07%` | `AiRecognitionServiceImplTest.java` |
| D57 | `AiRecognitionServiceImpl` Mockito 单测补齐确认成单成功路径：覆盖入库/出库 DTO 校验、草稿保存、正式单据与明细持久化、库存联动、确认状态更新和操作日志，单类行覆盖率提升到约 `72.99%` | `AiRecognitionServiceImplTest.java` |
| D58 | `AbstractOrderServiceSupport` Mockito 单测首版：补齐管理员权限、作废原因/备注、分页归一化、文本/金额/时间格式化、Excel 汇总行写入与 Jasper 模板加载，单类行覆盖率提升到 `100%` | `AbstractOrderServiceSupportTest.java` |
| D59 | `AiRecognitionServiceImpl` Mockito 单测补齐重复确认异常路径：覆盖入库/出库 `markConfirmedToOrder<=0` 时回查记录并抛“已确认”异常，单类行覆盖率提升到约 `73.93%` | `AiRecognitionServiceImplTest.java` |
| D60 | `AiRecognitionServiceImpl` Mockito 单测补齐出库确认异常路径：覆盖 `confirmOutbound(...)` 的“未匹配客户”与“保存出库单明细失败”分支，单类行覆盖率提升到约 `78.20%` | `AiRecognitionServiceImplTest.java` |
| D61 | `AiRecognitionServiceImpl` Mockito 单测补齐入库确认前置解析异常：覆盖 `confirmInbound(...)` 的“供应商不存在”与“供应商名称不能为空”分支，单类行覆盖率提升到约 `82.46%` | `AiRecognitionServiceImplTest.java` |
| D62 | `AiRecognitionServiceImpl` Mockito 单测补齐出库确认前置解析异常：覆盖 `confirmOutbound(...)` 的“客户不存在”与“客户名称不能为空”分支，单类行覆盖率提升到约 `83.89%` | `AiRecognitionServiceImplTest.java` |
| D63 | `AiRecognitionServiceImpl` Mockito 单测补齐出库客户解析剩余异常：覆盖 `confirmOutbound(...)` 的“匹配后客户不存在”与“客户显示名不能为空”分支，单类行覆盖率提升到约 `86.26%` | `AiRecognitionServiceImplTest.java` |
| D64 | `OrderItemTable` 第一阶段瘦身：抽取 `useOrderItemTableFocus`，将焦点注册、Tab/Shift+Tab、行高亮与首格聚焦逻辑移出组件主体，`OrderItemTable.vue` 行数降至约 `406` 行 | `useOrderItemTableFocus.js`、`OrderItemTable.vue` |
| D65 | `OrderItemTable` 第二阶段瘦身：拆分商品选择列为 `OrderProductSelectCell`，保持焦点/弹窗协议不变，`OrderItemTable.vue` 行数进一步降至约 `394` 行 | `OrderProductSelectCell.vue`、`OrderItemTable.vue` |
| D66 | `OrderItemTable` 第三阶段评估结论：当前轮次先停在焦点逻辑与商品列拆分，不继续抽象商品编码/名称/规格/单位与数量/单价列，避免为简单模板引入通用参数膨胀和 `el-input-number` 焦点风险 | `BACKLOG.md`、`frontend-order-pages.md` |
| D67 | `InboundOrderServiceImpl` Mockito 单测补齐 `saveInboundOrder(...)` 首批核心分支：覆盖保存成功主链与“同一商品重复出现在入库单中”异常，单类行覆盖率提升到约 `50.87%`，`service.impl` 包级行覆盖率提升到约 `68.10%` | `InboundOrderServiceImplTest.java`、`testing.md` |
| D68 | `InboundOrderServiceImpl` Mockito 单测继续补齐 `saveInboundOrder(...)` 异常分支：覆盖“供应商已停用”与“保存入库单明细失败”，单类行覆盖率提升到约 `51.56%`，`service.impl` 包级行覆盖率提升到约 `68.22%` | `InboundOrderServiceImplTest.java`、`testing.md` |
| D69 | `InboundOrderServiceImpl` Mockito 单测继续补齐 `saveInboundOrder(...)` 异常分支：覆盖“商品不存在”与“商品库存记录不存在”，单类行覆盖率提升到约 `52.25%`，`service.impl` 包级行覆盖率提升到约 `68.33%` | `InboundOrderServiceImplTest.java`、`testing.md` |
| D70 | `InboundOrderServiceImpl` Mockito 单测继续补齐 `saveInboundOrder(...)` 收尾异常分支：覆盖“保存入库单失败”与“入库单明细不能为空”，单类行覆盖率提升到约 `52.94%`，`service.impl` 包级行覆盖率提升到约 `68.45%` | `InboundOrderServiceImplTest.java`、`testing.md` |
| D71 | `InboundOrderServiceImpl` Mockito 单测集中补齐确认/作废/草稿编辑/分页/详情/Excel/PDF 导出主链：单类行覆盖率提升到约 `92.04%`，`service.impl` 包级行覆盖率提升到约 `74.93%`，阶段 A 当前优先级正式切到 `OutboundOrderServiceImpl` 与 `StockServiceImpl` | `InboundOrderServiceImplTest.java`、`testing.md` |
| D72 | `OutboundOrderServiceImpl` Mockito 单测集中补齐保存/草稿编辑/确认/作废/分页/详情/Excel/PDF 导出主链与高价值异常：单类行覆盖率提升到约 `95.04%`，`service.impl` 包级行覆盖率明显抬升 | `OutboundOrderServiceImplTest.java`、`testing.md` |
| D73 | `StockServiceImpl` Mockito 单测补齐分页 offset、Excel 导出与 CSV 导出：单类行覆盖率提升到约 `96.70%`，库存服务主链覆盖从“仅查询/调整”扩展到“查询/调整/导出” | `StockServiceImplTest.java`、`testing.md` |
| D74 | 后端覆盖率门槛落地：`service.impl` 包级行覆盖率提升到约 `86.75%` 后，在 `backend/pom.xml` 启用 JaCoCo `80%` 行覆盖率检查，并将 GitHub Actions 后端 job 切换为 `./mvnw verify`，同时补 `chmod +x mvnw` 消除 Linux runner 权限风险（原 `T5` 已收口） | `backend/pom.xml`、`.github/workflows/ci.yml`、`testing.md` |
| D75 | Maven 版本号与语义化版本收口：将 `backend/pom.xml`、`desktop-client/pom.xml`、`app.version` 默认值统一到 `1.7.0-SNAPSHOT`，消除 `0.0.1-SNAPSHOT` 与 `CHANGELOG.md` 最新发布序列不一致的问题 | `backend/pom.xml`、`desktop-client/pom.xml`、`application.yaml`、`SystemServiceImpl.java` |
| D76 | 前端 Vitest 组件测试首批落地：补齐 `OrderItemTable` 与 `ProductSelectDialog` 的渲染 / 关键交互测试，前端测试范围从“仅 composable 纯逻辑”扩展到“composable + 关键组件” | `frontend/src/components/**/__tests__/`、`testing.md` |
| D77 | 左侧导航栏折叠/展开落地：`MainLayout` 支持 220px/64px 侧栏切换、Header 折叠按钮、菜单图标、折叠态原生 tooltip 与 `localStorage` 跨刷新保持，并补 `MainLayout` 组件测试 | `MainLayout.vue`、`MainLayout.spec.js`、`testing.md` |
| D78 | `CustomerServiceImpl` / `SupplierServiceImpl` 覆盖率补测完成：补齐导出 Excel、成功查询与剩余高价值失败分支后，两者行覆盖率提升到约 `95.87%` / `97.75%`，`service.impl` 包级行覆盖率提升到约 `91.68%`，为后续评估 `85%` 或 BRANCH 门槛留出余量 | `CustomerServiceImplTest.java`、`SupplierServiceImplTest.java`、`testing.md` |
| D79 | GitHub 仓库保护链恢复：新仓库已重新配置 `protect-main` ruleset，并将 `backend-test`、`frontend-test` 设为 required checks；PR 需通过两项检查后才能合并 `main` | GitHub 仓库 `Settings / Rulesets`、`.github/workflows/ci.yml`、协作文档 |
| D80 | 前端页面级联动测试首版：补齐 `useInboundCreatePage` / `useOutboundCreatePage` 保存草稿 happy/error path，以及 `InboundOrderCreate` / `OutboundOrderCreate` 的“保存草稿 / 保存并新建 / 智能识别导入”页面联动测试；同时修正文档中“AI 弹窗 confirm 写回 form”的过时描述 | `frontend/src/composables/__tests__/`、`frontend/src/views/**/__tests__/`、`testing.md`、`frontend-order-pages.md` |
| D81 | 商品自定义字段 UI 改造：商品新增/编辑页将原始 JSON textarea 替换为键值对编辑器，自动序列化回 `customFieldsJson` 写入后端；商品列表中的自定义字段展示改为可读的键值列表 / 摘要，并补充字段序列化与编辑器交互单测 | `frontend/src/views/product/`、`frontend/src/components/product/`、`frontend/src/utils/productCustomFields.js`、`master-data.md` |
