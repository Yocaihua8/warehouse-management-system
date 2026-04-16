# ADR-002: 使用 UUID Token + 数据库会话而非 JWT

## 状态
已采纳（2025-06）；JWT 迁移列为 P3 待评估

## 背景

系统需要用户认证机制。主要候选方案为：
1. UUID Token + 服务端会话存储（DB 或 Redis）
2. JWT（无状态，客户端自包含 token）

项目初期为单实例部署，无水平扩展需求，快速验证业务正确性是首要目标。

## 决策

使用 **UUID Token + 数据库持久化会话**（`user_session` 表），通过 `TokenStore` 抽象层支持 DB / Redis 两种存储后端可切换（`AUTH_SESSION_STORE` 环境变量）。

Token 验证在 `LoginInterceptor` 中完成，通过 `CurrentUserContext`（ThreadLocal）向下游传递用户身份。

## 后果

**正面影响：**
- 服务端可主动使 token 失效（强制登出、密码重置），JWT 无法做到
- 会话重启可恢复（数据库持久化），不需要用户重新登录
- 实现简单，无需引入 JWT 签名/验证库
- 通过 `TokenStore` 接口，Redis 迁移路径已预留

**负面影响 / 权衡：**
- 每次请求需查数据库验证 token（DB 模式），高并发场景有性能瓶颈
- 不支持水平扩展的集中会话治理（多实例场景需切 Redis）
- Token payload 中无法携带声明（claims），角色信息需每次从 DB 读取

## 备选方案

**JWT**：无状态、可水平扩展，但主动失效（踢人下线）需要额外的 Deny List 机制，当前业务规模无此需求。P3 阶段可评估迁移。
