# 本地开发环境搭建

---

## 前置条件

| 工具 | 版本要求 | 用途 |
|------|---------|------|
| Java JDK | 17+ | 后端 + 桌面端 |
| Maven | 3.8+ | 后端/桌面端构建 |
| Node.js | 18+ | Web 前端 |
| MySQL | 8.x | 数据库 |
| Python | 3.9+ | AI 识别服务 |
| Git | — | 版本管理 |

---

## 一、数据库初始化

```bash
# 1. 确认 MySQL 已启动，进入 MySQL
mysql -u root -p

# 2. 仅创建空库（不要手工建表）
CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4;

# 3. 可选：导入演示数据（仅用于本地演示）
source /path/to/sql/demo_seed_mvp.sql
```

> 后端已接入 Flyway。启动后会自动执行 `backend/src/main/resources/db/migration/V1__init_schema.sql` 建表与基础初始化。  
> `sql/` 下 `V1_x` 脚本保留为历史参考，不再作为新环境主流程。
> 后续任何数据库字段/表结构变更，请只新增 `backend/src/main/resources/db/migration/V*.sql`，不要再往 `sql/V1_x` 追加。
>
> 注意：
> - 当前迁移链里仍有历史兼容性风险，首次建库或旧库重新迁移前，建议先确认 `db/migration` 脚本与当前 MySQL 版本兼容
> - 如果 `flyway_schema_history` 中已经出现失败记录，仅重启应用通常不会自动恢复，需要先处理失败记录再重试

---

## 二、后端启动

### 1. 配置本地数据库连接

复制示例文件：

```bash
cd backend/src/main/resources
cp application-local.example.yml application-local.yml
```

编辑 `application-local.yml`，填入本地数据库密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}   # 在此填入密码，或通过环境变量传入
```

说明：

- `application-local.yml` 按约定属于本地配置文件，不应继续提交真实账号密码
- 当前仓库里已经存在该文件，请先核对内容，不要直接信任其中的数据库连接信息
- 建议优先通过环境变量覆盖本机账号密码，减少“文档一套、机器上一套”的问题

### 2. 启动方式（任选其一）

```bash
# 方式一：通过环境变量传入密码（推荐）
cd backend
DB_PASSWORD=你的密码 mvn spring-boot:run

# Windows PowerShell
$env:DB_PASSWORD="你的密码"; mvn spring-boot:run

# 方式二：直接在 application-local.yml 里填密码
cd backend
mvn spring-boot:run
```

启动后访问：`http://localhost:8080`

API 文档：`http://localhost:8080/swagger-ui.html`

### 3. IntelliJ IDEA 运行配置（推荐）

如果你通过 IDEA 直接运行 `WarehouseManagementSystemApplication`，请在 Run Configuration 中显式设置环境变量：

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/wms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码
```

IDEA 路径：

```text
Run | Edit Configurations... | 选择 Spring Boot 配置 | Environment variables
```

说明：

- 如果你刚在 Windows“系统环境变量”里新增了 `DB_USERNAME/DB_PASSWORD`，需要**重启 IDEA**后才会被新进程读取。
- 控制台出现 `using password: NO` 基本可判定为 `DB_PASSWORD` 未传入当前 Java 进程。
- 主配置默认激活 `local` profile，本地启动默认读取 `application-local.yml`。
- `docker-compose.yml` 中的后端环境与手工启动并不完全一致：Compose 默认 `DB_PASSWORD=root`，并切到 `AUTH_SESSION_STORE=redis`。

### 4. 首次启动说明

首次启动时 `PasswordMigrationRunner` 会自动将数据库中的明文密码（如 demo_seed_mvp.sql 写入的 `123456`）迁移为 BCrypt 哈希，控制台会打印迁移日志。之后每次启动会自动跳过已迁移的用户，幂等安全。

此外，Flyway 会在启动时自动校验并记录数据库版本（`flyway_schema_history`）。已有旧库会自动 baseline，不会重复执行历史建表脚本。

补充说明：

- 当前 `mvn test` / `mvn install` 会受到本地数据库和 Flyway 状态影响，执行前请先确认本地库可正常启动应用
- 如果只是验证代码编译，建议优先使用 `-DskipTests`

### 5. 可选：启用 Redis 会话持久化

默认会话存储为数据库（`auth.session-store=db`）。如果要切到 Redis：

```bash
# Linux / macOS
AUTH_SESSION_STORE=redis REDIS_HOST=127.0.0.1 REDIS_PORT=6379 mvn spring-boot:run

# Windows PowerShell
$env:AUTH_SESSION_STORE="redis"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
mvn spring-boot:run
```

> Redis 模式下，token 会按 `auth.session-timeout-minutes` 写入 TTL；登出会删除 Redis 会话键。  
> Redis key 前缀可通过 `AUTH_REDIS_KEY_PREFIX` 调整（默认 `wms:session:`）。

---

## 三、Python AI 服务启动

```bash
cd python-ai-service

# 使用仓库内置虚拟环境（推荐）
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 9000

# 或使用系统 Python（需已安装 requirements.txt 依赖）
pip install -r requirements.txt
python -m uvicorn app:app --host 127.0.0.1 --port 9000
```

启动后验证：`http://127.0.0.1:9000/health`

> AI 服务是可选启动项。不启动时 AI 识别功能不可用，其余功能正常。
> 当前 Python AI 服务只支持图片识别（jpg/jpeg/png），PDF 上传会被服务端直接拒绝。

---

## 四、Web 前端启动

```bash
cd frontend
npm install       # 首次需要
npm run dev
```

访问地址：`http://localhost:5173`

> 默认代理后端地址为 `http://localhost:8080`，通过 `VITE_API_BASE_URL` 环境变量控制。
> 如果后端端口不是 8080，在 `frontend/.env.local` 中覆盖：
> ```
> VITE_API_BASE_URL=http://localhost:你的端口
> ```

---

## 五、桌面端启动

```bash
cd desktop-client
mvn javafx:run
```

> 不建议直接 run `AppLauncher.java`，请使用 `mvn javafx:run`。
> 桌面端依赖后端服务，请确保后端已启动。

---

## 六、推荐启动顺序

```
MySQL → 后端（8080）→ Python AI 服务（9000，可选）→ Web 前端（5173）/ 桌面端
```

---

## 七、默认演示账号

> 仅在执行 `demo_seed_mvp.sql` 后存在。

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `123456` | ADMIN（管理员） |
| `operator` | `123456` | OPERATOR（操作员） |

---

## 八、常见问题

**Q：如何启用低库存预警通知（邮件/消息推送）？**
→ 设置以下环境变量后重启后端：
`LOW_STOCK_ALERT_ENABLED=true`  
邮件：`LOW_STOCK_ALERT_MAIL_ENABLED=true` + `LOW_STOCK_ALERT_MAIL_TO=xxx@example.com` + `MAIL_HOST/...`  
Webhook：`LOW_STOCK_ALERT_WEBHOOK_ENABLED=true` + `LOW_STOCK_ALERT_WEBHOOK_URL=https://xxx`
可用接口 `POST /stock/low-alert/trigger` 手动触发一次验证发送。

**Q：后端启动报错 `Access denied for user 'root'`**
→ 先检查 `application-local.yml` 和环境变量哪个在生效，再确认密码是否正确。

**Q：Web 前端请求返回 CORS 错误**
→ 检查后端是否正常启动，确认 `GlobalCorsConfig` 中允许的 origin 包含 `http://localhost:5173`。

**Q：首次登录后 token 立即失效**
→ 先检查是否超过会话超时时间（默认 7 天）或已主动退出登录。若使用 DB 会话模式，还要确认 `user_session` 表可正常写入；若使用 Redis，会话配置需与当前启动方式一致。

**Q：AI 识别返回 500**
→ 先检查 Python AI 服务是否正常运行（访问 `http://127.0.0.1:9000/health`），再确认上传的是图片而不是 PDF。

**Q：执行 `mvn test` / `mvn install` 时 Flyway 校验失败**
→ 这通常不是 Maven 本身的问题，而是本地数据库中的 Flyway 历史状态或迁移脚本兼容性出了问题。先确认应用能否正常启动，再排查 `flyway_schema_history`。

**Q：后端启动报 `Port 8080 was already in use`**
→ 先查占用进程再决定“复用现有服务”或“杀进程重启”：
```bash
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```
如果只是想调试，不建议双开同端口实例。

**Q：AI 启动报 `WinError 10048`（9000 端口占用）**
→ 原因通常是已有 AI 进程在跑，不是代码错误：
```bash
netstat -ano | findstr :9000
taskkill /PID <PID> /F
```
然后重新执行：
```bash
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 9000
```

**Q：执行 `mvn javafx:run` 报找不到 JavaFX 模块**
→ 确认使用的是 JDK 17，不是 JRE。检查 `desktop-client/pom.xml` 中 JavaFX 版本是否与 JDK 匹配。
