# 架构决策记录（ADR）

> Architecture Decision Records —— 记录系统中每一个重大架构决策：做了什么、为什么、放弃了什么。
>
> 新决策请按 [ADR 模板](#模板) 新增文件，文件名格式：`ADR-NNN-简短描述.md`。

---

## 决策列表

| ADR | 标题 | 状态 | 日期 |
|-----|------|------|------|
| [ADR-001](./ADR-001-mybatis-over-jpa.md) | 使用 MyBatis 而非 JPA/Hibernate | 已采纳 | 2025-06 |
| [ADR-002](./ADR-002-uuid-token-sessions.md) | 使用 UUID Token + 数据库会话而非 JWT | 已采纳 | 2025-06 |
| [ADR-003](./ADR-003-python-ocr-service.md) | OCR 识别作为独立 Python 服务 | 已采纳 | 2025-08 |
| [ADR-004](./ADR-004-stock-flow-single-entry.md) | StockFlowService 作为库存变更唯一入口 | 已采纳 | 2025-06 |
| [ADR-005](./ADR-005-snapshot-pattern.md) | 订单明细使用数据快照模式 | 已采纳 | 2025-06 |
| [ADR-006](./ADR-006-ai-draft-not-direct-order.md) | AI 识别结果不直接生成正式单据 | 已采纳 | 2025-08 |

---

## 状态说明

| 状态 | 含义 |
|------|------|
| 已采纳 | 当前有效，代码以此为准 |
| 已废弃 | 不再适用，但保留历史记录 |
| 已取代 | 被另一个 ADR 替代，见说明 |
| 草稿 | 尚在讨论，未最终确定 |

---

## 模板

```markdown
# ADR-NNN: 标题

## 状态
已采纳 / 已废弃 / 已取代（by ADR-XXX）/ 草稿

## 背景
[描述导致需要做这个决策的背景和问题]

## 决策
[我们做了什么决定]

## 后果
**正面影响：**
- ...

**负面影响 / 权衡：**
- ...

## 备选方案
[描述考虑过但未采用的方案，以及放弃原因]
```
