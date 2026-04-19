# 系统设计总览

> 状态：Active  
> Owner：Yocaihua8  
> Last Updated：2026-04-18  
> Scope：ERP + WMS 一体化系统  
> Related：`../架构设计说明.md`、`./architecture-overview.md`、`./database-design.md`、`../features/README.md`

## 1. 设计目标

当前系统面向中小型仓储与进销存场景，目标是在现有 WMS 核心闭环基础上，稳定支撑：

- 商品、客户、供应商主数据管理
- 手工入库 / 出库单据流转
- 库存联动与审计
- AI 辅助单据识别与人工确认
- Web 前端与桌面端双入口协同

本文档侧重系统边界、逻辑组成和关键流程；三层架构边界与技术栈细节统一收口到 [architecture-overview.md](./architecture-overview.md)。

## 2. 系统边界

- 系统输入：
  - Web 前端请求
  - JavaFX 桌面端请求
  - AI 图片上传
- 系统输出：
  - 单据、库存、主数据查询结果
  - Excel / CSV / PDF 导出结果
  - AI 识别建议结果
- 外部依赖：
  - MySQL
  - Python FastAPI + PaddleOCR 服务
- 非目标范围：
  - 财务总账
  - 采购单 / 销售单正式模块
  - 复杂工作流引擎

## 3. 关联架构

| 项目 | 说明 |
|------|------|
| 架构模式 | 前后端分离 + Spring Boot 单体三层架构 + 独立 Python AI 服务 |
| 关联文档 | [architecture-overview.md](./architecture-overview.md) |
| 关键层 / 关键端口 | Web / JavaFX 入口、Spring Boot API、MyBatis 数据层、Python OCR 服务 |
| 架构约束摘要 | Controller 不承载业务逻辑；库存变更统一经 `StockFlowService`；AI 识别结果不直接生成正式单据 |

## 4. 逻辑组成

| 组成部分 | 职责 | 对应层 / 角色 | 输入 | 输出 |
|----------|------|----------------|------|------|
| Web 前端 | 页面渲染、录单工作台、权限路由控制 | 表现层 | 用户操作 | API 请求、页面状态 |
| JavaFX 桌面端 | 桌面入口与本地化操作界面 | 表现层 | 用户操作 | API 请求、桌面视图 |
| Spring Boot 后端 | 鉴权、业务编排、事务控制、导出 | 业务核心 | HTTP 请求 | 统一 `Result` 响应 |
| MyBatis Mapper + MySQL | 持久化、查询、历史快照保存 | 数据层 | 领域对象、查询条件 | 数据记录 |
| Python AI 服务 | OCR 识别、结构化提取 | 外部能力 | 图片文件 | JSON 识别结果 |

## 5. 核心流程

### 5.1 手工入库 / 出库流程

1. 用户在前端或桌面端录入单据草稿
2. 后端校验主数据、库存与状态流转约束
3. Admin 确认单据后，经 `StockFlowService` 完成库存增减并写审计日志

### 5.2 AI 识别确认流程

1. 用户上传单据图片
2. Spring Boot 调用 Python OCR 服务生成识别建议
3. 识别结果保存为 AI 草稿记录
4. 用户人工确认后再生成正式入库 / 出库单

## 6. 非功能设计

### 6.1 性能

- 列表接口统一分页，默认限制页大小
- Dashboard 与部分聚合统计已加入缓存

### 6.2 安全

- Token 鉴权 + 双角色权限模型
- Admin-only 操作采用拦截器 + Service 双重校验

### 6.3 可维护性

- 前端录单逻辑已收敛到 composables 与工作台组件
- 后端核心服务已形成较高单测覆盖率与 JaCoCo 包级门槛

### 6.4 可扩展性

- AI 服务独立部署，便于单独演进识别能力
- 单据工作台已为后续采购单 / 销售单扩展预留模式

## 7. 待补充设计

- 采购单 / 销售单正式模块落地后的系统边界扩展
- 如未来引入更多对外 API，再补充接口破坏性变更说明
