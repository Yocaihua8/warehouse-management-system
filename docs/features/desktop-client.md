# 桌面端客户端功能规格

---

## 1. 系统定位

JavaFX 桌面客户端是 WMS 系统的本地部署交互入口，面向不使用浏览器的个体户/小商家场景。

**核心原则：桌面端只负责交互，不负责业务落库。所有业务操作通过 HTTP 调用后端 API 完成。**

### 历史演进摘要

- 早期文档将“启动约定”“目录结构设计”“12周计划”分散维护。
- 当前已统一收口到本规格：启动链（health -> bootstrap -> user/me）、目录分层、模块现状与待办统一在此维护。

---

## 2. 架构约束

| 约束 | 说明 |
|------|------|
| 不直连数据库 | 所有数据通过后端 REST API 获取和提交 |
| 不复制业务逻辑 | 校验、状态流转、库存计算全部由后端 Service 层负责 |
| 单机部署 | 默认 `127.0.0.1:8080`（后端）+ `127.0.0.1:9000`（AI 服务） |
| Token 认证 | 复用后端 TokenStore 机制，本地持久化 token 但每次启动必须服务端校验 |

---

## 3. 启动流程

```
应用启动
    │
    ▼
读取本地配置（serverBaseUrl, token, lastUsername）
    │
    ▼
GET /system/health ──── 失败 ────→ 启动异常页（"后端不可用"，提供重试）
    │ 成功
    ▼
GET /system/bootstrap ── 检查 desktopSupported
    │ true
    ▼
本地有 token？
    │
    ├─ 有 → POST /user/me ── 失败 → 清 token → 登录页
    │                        成功 → 主界面
    │
    └─ 无 → 登录页
```

### 启动异常处理

| 场景 | 表现 | 处理 |
|------|------|------|
| 后端不可达 | `/system/health` 失败 | 停留启动页，显示服务地址，提供重试 |
| 数据库不可用 | health 返回 database=DOWN | 阻止进入主界面 |
| AI 服务不可用 | health 返回 ai=DOWN | 允许进入，提示"AI 识别不可用" |
| Token 已过期 | `/user/me` 返回失败 | 清空本地 token，跳转登录页 |

---

## 4. 已实现功能

### 基础能力层

| 模块 | 状态 | 说明 |
|------|------|------|
| 启动检查链 | 已完成 | health → bootstrap → user/me 三步验证 |
| 登录/登出 | 已完成 | 本地 token 持久化 + 服务端校验 |
| API Client 封装 | 已完成 | 统一 HTTP 请求 + token 注入 + 异常处理 |
| 主框架 | 已完成 | 左侧导航 + 顶部状态区 + 主内容区 |
| 本地配置存储 | 已完成 | serverBaseUrl / token / lastUsername |

### 业务模块

| 模块 | 状态 | 覆盖能力 |
|------|------|---------|
| 工作台首页 | 已完成 | 统计数据展示 |
| 商品管理 | 已完成 | 查询 / 分页 / 新增 / 编辑 / 删除 |
| 客户管理 | 已完成 | 查询 / 分页 / 新增 / 编辑 / 删除 |
| 供应商管理 | 部分完成 | 查询 / 分页 / 新增（编辑/删除待补） |
| 库存管理 | 已完成 | 列表 / 低库存展示 / 导出 / 库存流水 |
| 手工入库 | 已完成 | 列表 / 新建草稿 / 确认 / 作废 / 详情 |
| 手工出库 | 已完成 | 列表 / 新建草稿 / 确认 / 作废 / 详情 |
| AI 入库 | 已完成 | 历史列表 / 上传识别 / 确认 / 详情 |
| AI 出库 | 已完成 | 上传识别 / 确认 |
| 系统设置 | 已完成 | 连接状态 / 启停后端与 AI 服务 / 日志目录与启动检查 |

### 系统设置页当前能力

- 支持保存服务地址、本地配置与健康检查结果展示
- 支持从桌面端直接启动后端与 AI 服务，并在操作完成后重新执行启动检查
- 支持从桌面端停止后端与 AI 服务；优先读取 PID 文件定位进程，PID 不可用时再按端口回退查找本地监听进程
- 支持显示后端 / 数据库 / AI 状态、最近失败原因、最近刷新时间与日志目录位置

---

## 5. 目录结构

```
desktop-client/
  pom.xml
  src/main/java/com/yocaihua/wms/desktop/
    AppLauncher.java          # 应用入口
    bootstrap/                # 启动流程编排
      StartupCoordinator.java
      StartupState.java
    config/                   # 本地配置读写
      AppConfig.java
      LocalConfigStore.java
    auth/                     # 认证相关
      AuthService.java
      AuthSession.java
    api/                      # HTTP 客户端封装
      ApiClient.java
      endpoint/               # 按模块拆分的 API
        SystemApi.java
        UserApi.java
        ProductApi.java
        ...
    common/                   # 常量、枚举、工具
    ui/                       # 通用 UI 组件
      layout/                 # 主框架壳
      component/              # 公共组件
    module/                   # 业务模块
      home/
      product/
      customer/
      supplier/
      stock/
      inbound/
      outbound/
      ai/
      settings/
  src/main/resources/
    fxml/                     # JavaFX 布局文件
    css/                      # 样式文件
    icons/                    # 图标资源
```

---

## 6. 依赖后端接口清单

### 启动与认证

| 接口 | 用途 |
|------|------|
| `GET /system/health` | 后端/数据库/AI 健康检查 |
| `GET /system/bootstrap` | 获取应用配置（版本、超时、AI 地址等） |
| `POST /user/login` | 登录 |
| `POST /user/logout` | 登出 |
| `POST /user/me` | Token 有效性校验 + 获取当前用户信息 |

### 业务接口

桌面端复用 Web 前端所使用的全部后端 API，按模块对应 Controller。详见 Swagger 文档：`http://localhost:8080/swagger-ui.html`

---

## 7. 本地配置结构

```json
{
  "serverBaseUrl": "http://127.0.0.1:8080",
  "token": "",
  "lastUsername": "",
  "rememberServer": true
}
```

- `token` 仅用于本地自动登录，每次启动仍需 `/user/me` 服务端校验
- `serverBaseUrl` 支持局域网部署时修改

---

## 8. 已知问题与待完成项

| 问题 | 状态 |
|------|------|
| 入库/出库打印功能未收口 | 待实现 |
| 系统设置页异常排障引导可继续增强 | 待实现 |
| 供应商管理缺少编辑/删除 | 待实现 |
| 数据库初始化引导未实现 | 待实现 |
| 完整安装包/一键部署未实现 | 待实现 |
| 创建页离开后不保留未保存表单状态 | 已知限制 |

---

## 9. 启动方式

```bash
cd desktop-client
mvn javafx:run
```

> 不建议直接运行 `AppLauncher.java`，请使用 `mvn javafx:run`。  
> 桌面端依赖后端服务，请确保后端已启动。
