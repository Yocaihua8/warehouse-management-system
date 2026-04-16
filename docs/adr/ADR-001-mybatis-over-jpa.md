# ADR-001: 使用 MyBatis 而非 JPA/Hibernate

## 状态
已采纳（2025-06）

## 背景

后端需要选择 ORM 框架。主要候选方案为 MyBatis 和 Spring Data JPA（Hibernate）。

本系统的 SQL 查询包含多表 JOIN、条件聚合（Dashboard 趋势统计）、按商品名称模糊搜索等，这些场景在 JPQL 或 Criteria API 中书写和维护成本较高。

## 决策

使用 **MyBatis**（`mybatis-spring-boot-starter`）作为数据访问层框架，注解与 XML 混合模式：

- 简单 CRUD 使用注解（`@Select` / `@Insert` / `@Update` / `@Delete`）
- 复杂查询（动态 WHERE、JOIN、分页）使用 XML Mapper

## 后果

**正面影响：**
- SQL 完全可见、可调试，便于排查慢查询
- 复杂多表查询（如 Dashboard 统计、订单列表带商品信息）代码直观
- Mapper XML 便于 DBA 或后续维护者直接审阅 SQL

**负面影响 / 权衡：**
- 无实体关系自动管理，关联查询须手写 SQL
- 不支持数据库无关的 JPQL，迁移数据库方言时需手动调整 SQL
- 相比 Spring Data JPA，CRUD 样板代码略多

## 备选方案

**Spring Data JPA（Hibernate）**：自动生成 CRUD、关系映射便利，但对复杂查询的 JPQL 可读性差、N+1 问题需额外处理，与当前偏 SQL-centric 的查询需求不符。
