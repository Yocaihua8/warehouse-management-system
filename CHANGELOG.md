# Changelog

所有版本的变更记录遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范，版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### Changed
- 补充 GitHub 协作文档：新建仓库时，需先让 `backend-test`、`frontend-test` 成功运行一次，Ruleset 才能选择 required checks
- 同步工程文档：收口当前 `main` 单分支协作方式，并确认 `protect-main` 与 required checks 已在新仓库恢复
- 启用 `docs/devlog/` 开发日志目录，并补充首份仓库重建与 CI / Ruleset 恢复记录
- 按文档规范补充 `docs/design/` 目录下的系统设计、架构摘要与数据库设计桥接文档，并接入 README 文档索引
- 补充前端创建页页面级联动测试，并将前端规格文档中的 AI 弹窗流程描述修正为当前实现
- 商品新增/编辑页的自定义字段从原始 JSON textarea 改为键值对编辑器，并补充列表摘要展示与字段序列化单测
- 客户/供应商新增 `custom_fields_json` 字段，前后端复用键值对编辑器与 JSON 对象校验，列表页增加自定义字段摘要展示

### 计划中
- 单据工作台第二批：收敛 `useOrderWorkbenchPage` 公共 composable，接入 `ProductSelectDialog`
- Tab 键行间跳转与预置空行键盘录入流
- 桌面端停止服务功能

---

## [1.6.0] - 2026-04

### Added
- ERP 单据工作台体验增强（阶段5）：入库/出库明细默认预置 8 行空行
- 合计行内嵌至明细表格末行，与数量/金额列对齐
- 操作区按钮重排为"左辅右主"，补充快捷键标注（S/P/C）
- 出库明细按商品自动回填库存余量列
- 商品列行内可编辑（选中商品后可继续人工修正规格/单位）
- 单据工作台骨架落地：新增 `order-workbench/` 组件目录（`OrderHeaderForm` / `OrderDetailTable` / `OrderSummaryBar` / `ProductSelectDialog`）
- 创建页拆分为"路由壳 + 工作台容器"（`InboundCreateView` → `InboundOrderCreate`）
- 提交链路统一串联：保存草稿 → 确认提交，编辑草稿场景先保存再确认
- 入/出库列表页双左栏收敛，释放主表格宽度

### Changed
- 单据底部操作区重排，解决统一右对齐导致的主次混乱

---

## [1.5.0] - 2026-03

### Added
- 前端单据页重构（阶段1–4）：建立 composables 层（9个）、通用组件层（7个）、工具层（4个）
- 打印三层架构：`printAdapter` / `PrintTemplate` / `printService` 分离
- AI 入库/出库历史接口与前端接入分页（消除全量加载风险）
- 供应商管理 Web 端补齐：列表/新增/编辑/删除，接入主导航与路由
- AI 服务拆分：`OcrAdapterService` / `ProductMatchService` / `AiOrderAssemblerService` / `AiDraftPersistenceService` / `AiRecognitionValidationService` / `AiRecognitionVoAssemblerService`
- 后端单测补齐：入库/出库确认/作废库存联动，AI 识别异常路径

### Changed
- `InboundCreateView` 1564行 → 158行，`OutboundCreateView` 1231行 → 160行
- `InboundPrintView` 355行 → 73行，`OutboundPrintView` 315行 → 63行
- ERP 单据工作台商品列从只读文本改为行内输入

---

## [1.4.0] - 2026-02

### Added
- Token 会话持久化（`user_session` 表，重启后会话可恢复）
- Token Store 支持 DB / Redis 可切换（`AUTH_SESSION_STORE` 环境变量）
- Flyway 迁移主流程接管，历史 `sql/V1_x` 收口为参考脚本
- RestTemplate 可配置超时（`connect-timeout-ms` / `read-timeout-ms`）
- 停用客户/供应商下单拦截 + 删除前关联订单校验（双重校验）
- 入库/出库服务公共基类 `AbstractOrderServiceSupport`
- 配置治理收敛：`application.yaml` 统一键与默认值，`application-local.yml` 仅保留本地覆盖

### Fixed
- `ProductServiceImpl` 启动期直接改表逻辑移除，结构变更回归 Flyway
- 商品/客户 `status/code` 前后端契约对齐

---

## [1.3.0] - 2025-12

### Added
- 用户管理 Web 端首版（列表/新增/编辑/删除，Admin-only）
- AI 出库识别历史列表页与详情页
- 操作日志（记录关键操作，Admin-only 查询）
- 低库存预警通知（定时扫描 + 手动触发 + 邮件/Webhook + 冷却去重）
- 首页近 7 天入库/出库趋势折线图
- Dashboard Caffeine 缓存（30s TTL）
- 用户管理接口权限收口（`/user/add|update|delete` 纳入 Admin-only 校验）

### Fixed
- 首页趋势统计 SQL 兼容 MySQL `only_full_group_by` 模式

---

## [1.2.0] - 2025-10

### Added
- BCrypt 密码加密 + `PasswordMigrationRunner`（启动时自动迁移明文密码）
- Swagger 文档（springdoc-openapi）
- Docker Compose：MySQL + Redis + Python AI + 后端一键启动
- 列表分页 `pageSize` 上限（`MAX_PAGE_SIZE=200`）
- Pinia auth store（统一管理 token / username / nickname / role）

### Security
- 双层权限控制：拦截器层 + Service 层二次校验

---

## [1.1.0] - 2025-08

### Added
- Vue 3 Web 前端完整闭环：登录/商品/客户/供应商/库存/入库单/出库单/AI 识别
- AI 辅助出库识别（入库/出库双链路）
- JavaFX 桌面端基础框架 + 主要业务模块
- AI 识别历史列表（入库侧）
- 数据导出（商品/库存/客户/供应商，Excel/CSV）

---

## [1.0.0] - 2025-06

### Added
- Spring Boot 后端核心业务闭环
- 用户认证（UUID Token，双角色 ADMIN/OPERATOR）
- 商品管理、客户管理、供应商管理（CRUD + 软删除）
- 入库单完整流程（草稿→完成→作废，库存联动，数据快照）
- 出库单完整流程（双重库存校验，创建时即校验库存余量）
- 库存管理（`StockFlowService` 统一入口，流水审计不可篡改）
- AI 辅助入库识别（上传→OCR→商品匹配→人工确认→生成单据）
- Python FastAPI + PaddleOCR AI 服务

---

[Unreleased]: https://github.com/Yocaihua8/warehouse-management-system/compare/v1.6.0...HEAD
[1.6.0]: https://github.com/Yocaihua8/warehouse-management-system/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/Yocaihua8/warehouse-management-system/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/Yocaihua8/warehouse-management-system/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/Yocaihua8/warehouse-management-system/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/Yocaihua8/warehouse-management-system/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/Yocaihua8/warehouse-management-system/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Yocaihua8/warehouse-management-system/releases/tag/v1.0.0
