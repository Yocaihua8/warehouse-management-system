# 认证与权限

---

## 1. 认证机制

### 登录流程

```
POST /user/login
  → 校验用户名/密码（BCrypt matches）
  → 校验用户状态（status=1）
  → 生成 UUID token
  → TokenStore.saveToken(token, username, role)
  → 返回 { token, username, nickname, role }

前端：
  → localStorage 存入 token / username / nickname / role
  → 后续请求自动在 Header 中附加 token 字段
```

### Token 验证（每次请求）

```
LoginInterceptor.preHandle()
  → 读取 Header["token"]
  → TokenStore.getUsername(token) / getRole(token)
  → 如未找到 → 返回 401
  → 设置 CurrentUserContext（ThreadLocal）
  → 检查 isAdminOnlyRequest() → 非 Admin 调用 Admin 接口 → 返回 403
```

### 当前 Token 机制的限制

- **已改为数据库会话持久化**（`user_session`），进程重启后会话可恢复
- **仍不支持水平扩展的集中会话治理**，多实例场景建议后续迁移到 Redis/JWT 方案
- **具备会话过期控制**，过期时间由 `session-timeout-minutes` 配置驱动

---

## 2. 角色定义

| 角色 | 说明 |
|------|------|
| `ADMIN` | 管理员，可执行全部操作 |
| `OPERATOR` | 操作员，只能查询和新建/编辑草稿 |

角色解析规则（`UserServiceImpl.resolveRole()`）：
1. 优先取 `user.role` 字段值
2. 如果 `role` 为空且 `username == "admin"`（忽略大小写），则返回 `ADMIN`
3. 其他情况返回 `OPERATOR`

---

## 3. 权限矩阵

### 基础资料模块

| 操作 | ADMIN | OPERATOR |
|------|:-----:|:--------:|
| 查询列表/详情 | ✓ | ✓ |
| 新增 | ✓ | ✗ |
| 修改 | ✓ | ✗ |
| 删除 | ✓ | ✗ |
| 导出 Excel | ✓ | ✓ |

> 适用于：商品管理、客户管理、供应商管理

### 库存模块

| 操作 | ADMIN | OPERATOR |
|------|:-----:|:--------:|
| 查询库存列表 | ✓ | ✓ |
| 导出库存 | ✓ | ✓ |
| 手动调整库存 | ✓ | ✗ |
| 查询库存流水 | ✓ | ✓ |

### 入库/出库订单模块

| 操作 | ADMIN | OPERATOR |
|------|:-----:|:--------:|
| 查询列表/详情 | ✓ | ✓ |
| 新建草稿 | ✓ | ✓ |
| 编辑草稿 | ✓ | ✓ |
| **确认订单** | ✓ | ✗ |
| **作废订单** | ✓ | ✗ |
| 导出 Excel/PDF | ✓ | ✓ |

### AI 识别模块

| 操作 | ADMIN | OPERATOR |
|------|:-----:|:--------:|
| 上传单据识别 | ✓ | ✓ |
| 查看识别历史/详情 | ✓ | ✓ |
| **确认识别结果（生成正式单）** | ✓ | ✗ |

---

## 4. Admin-Only 接口列表

以下接口在 `LoginInterceptor.isAdminOnlyRequest()` 中明确标注为仅限 Admin：

| 路径 | 方法 | 说明 |
|------|------|------|
| `/product/add` | POST | 新增商品 |
| `/product/update` | PUT | 修改商品 |
| `/product/delete/{id}` | DELETE | 删除商品 |
| `/customer/add` | POST | 新增客户 |
| `/customer/update` | PUT | 修改客户 |
| `/customer/delete/{id}` | DELETE | 删除客户 |
| `/supplier/add` | POST | 新增供应商 |
| `/supplier/update` | PUT | 修改供应商 |
| `/supplier/delete/{id}` | DELETE | 删除供应商 |
| `/stock/update` | PUT | 手动调整库存 |
| `/stock/low-alert/trigger` | POST | 手动触发低库存预警 |
| `/inbound-order/{id}/confirm` | POST | 确认入库 |
| `/inbound-order/{id}/void` | POST | 作废入库单 |
| `/outbound-order/{id}/confirm` | POST | 确认出库 |
| `/outbound-order/{id}/void` | POST | 作废出库单 |
| `/ai/inbound/confirm` | POST | AI 入库确认 |
| `/ai/outbound/confirm` | POST | AI 出库确认 |
| `/operation/log/list` | GET | 操作日志查询 |
| `/user/add` | POST | 新增用户 |
| `/user/update` | PUT | 修改用户 |
| `/user/delete/{id}` | DELETE | 删除用户 |

> 此列表是权限控制的**权威来源**，新增 Admin-only 接口时必须同步更新此列表和 `LoginInterceptor`。

---

## 5. 无需鉴权的接口

在 `WebConfig.java` 的 `excludePathPatterns` 中配置：

| 路径 | 说明 |
|------|------|
| `/user/login` | 登录接口 |
| `/system/bootstrap` | 系统启动信息 |
| `/system/health` | 健康检查 |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI JSON |

---

## 6. 前端权限控制

前端从 localStorage 读取 `role` 字段，按角色控制按钮/操作的可见性：

```javascript
import { getRole } from '@/utils/auth'

const isAdmin = computed(() => getRole() === 'ADMIN')
```

> **注意**：前端的角色控制只是 UI 层隐藏，不能替代后端的权限校验。后端会对所有 Admin-only 接口做强制拦截，前端绕过只会得到 403。
