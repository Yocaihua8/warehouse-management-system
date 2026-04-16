# 功能规格文档索引

> 本目录收录各业务模块的功能规格文档，描述**当前已实现**的功能边界、业务规则、状态机和 API 接口。  
> 开发新功能或修复 Bug 时，请先阅读对应规格文档，以确保实现与设计一致。

---

## 模块列表

| 文档 | 覆盖模块 | 关键内容 |
|------|---------|---------|
| [auth.md](./auth.md) | 用户认证与权限 | 登录流程、Token 机制、ADMIN/OPERATOR 权限矩阵、Admin-only 接口清单 |
| [master-data.md](./master-data.md) | 商品 / 客户 / 供应商管理 | CRUD 规则、删除前置校验、停用拦截、自定义字段 |
| [inbound-order.md](./inbound-order.md) | 入库单管理 | 状态机（草稿→完成→作废）、库存增减、source_type、数据快照 |
| [outbound-order.md](./outbound-order.md) | 出库单管理 | 状态机、双重库存校验、与入库单的差异对比 |
| [inventory.md](./inventory.md) | 库存管理 | StockFlowService 必经原则、所有变更场景、流水不可篡改 |
| [ai-recognition.md](./ai-recognition.md) | AI 辅助识别 | 识别状态机、入库/出库识别流程、商品匹配逻辑、确认规则 |
| [desktop-client.md](./desktop-client.md) | 桌面端客户端 | 启动流程、已实现功能、目录结构、依赖接口 |
| [frontend-order-pages.md](./frontend-order-pages.md) | 单据页前端架构 | composables 层接口、组件规格、PAGE_MODE 状态模型、打印架构、ERP 扩展路线 |

---

## 阅读建议

- **首次了解系统**：先读 [auth.md](./auth.md)（权限体系是所有功能的前提），再按业务流程读入库 → 出库 → 库存
- **修改基础资料逻辑**：读 [master-data.md](./master-data.md)，特别注意删除前置校验和停用状态的拦截规则
- **修改订单逻辑**：读对应的 inbound/outbound 规格，重点关注状态机和库存联动规则
- **修改库存逻辑**：必读 [inventory.md](./inventory.md)，所有库存变更必须经过 `StockFlowService`
- **修改 AI 功能**：读 [ai-recognition.md](./ai-recognition.md)，注意 AI 确认为 Admin 专属操作
- **修改单据前端**：读 [frontend-order-pages.md](./frontend-order-pages.md)，了解 composables 层、组件接口与 PAGE_MODE 规范

---

## 文档约定

- 文档描述**已实现**的逻辑，不包含规划中的功能
- 未实现、待修复的问题记录在 [BACKLOG.md](../BACKLOG.md)
- 如发现文档与代码不一致，以代码为准，并更新文档
