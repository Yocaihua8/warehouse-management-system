# 仓库管理系统

这是一个围绕中小型仓储场景开发的仓库管理系统，当前已经从“纯 Web 版阶段”进入“桌面端设计与落地阶段”。

当前项目形态不是推倒重做，而是在现有工程基础上并行维护两条使用路径：

- Web 端：用于现有前后端联调、页面验证和备用管理入口
- 桌面端：用于本地单机部署、家庭/小型商户使用场景和更完整的交付形态

## 当前阶段

当前仓库已经包含 4 个主要部分：

- [backend](/E:/Code/warehouse-management-system/backend)：Spring Boot 后端
- [frontend](/E:/Code/warehouse-management-system/frontend)：Vue 3 Web 前端
- [desktop-client](/E:/Code/warehouse-management-system/desktop-client)：JavaFX 桌面客户端
- [python-ai-service](/E:/Code/warehouse-management-system/python-ai-service)：Python OCR / AI 服务

当前核心状态：

- Web 端核心业务链已基本闭环
- 桌面端已不再是骨架，已接入真实业务模块
- AI 入库桌面链已基本完整
- AI 出库桌面链已具备“上传识别 -> 确认 -> 正式出库单”主链

## 技术栈

### 后端

- Spring Boot
- MyBatis
- MySQL
- Maven

### Web 前端

- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router

### 桌面端

- Java 17
- JavaFX
- Maven

### AI 服务

- FastAPI
- PaddleOCR
- Uvicorn
- DeepSeek API（可选）

## 目录结构

```text
warehouse-management-system/
├── backend/             Spring Boot 后端
├── frontend/            Vue Web 前端
├── desktop-client/      JavaFX 桌面端
├── python-ai-service/   Python OCR / AI 服务
├── sql/                 数据库脚本
├── docs/                项目文档
└── README.md
```

## 当前已完成能力

### 后端

已接入并可支撑桌面端 / Web 端的主要接口包括：

- 用户登录与当前用户校验
- 用户管理（列表 / 新增 / 编辑 / 删除，管理员）
- 商品管理
- 客户管理
- 供应商管理
- 库存列表
- 库存流水
- 入库单列表 / 新增 / 详情 / 确认 / 作废
- 出库单列表 / 新增 / 详情 / 确认 / 作废
- AI 入库识别 / 历史列表 / 详情 / 确认
- AI 出库识别 / 历史列表 / 详情 / 确认
- Dashboard / System 健康检查

对应控制器可参考：

- [ProductController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/ProductController.java)
- [CustomerController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/CustomerController.java)
- [SupplierController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/SupplierController.java)
- [StockController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/StockController.java)
- [StockAdjustLogController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/StockAdjustLogController.java)
- [InboundOrderController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/InboundOrderController.java)
- [OutboundOrderController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/OutboundOrderController.java)
- [AiAssistController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/AiAssistController.java)
- [DashboardController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/DashboardController.java)
- [SystemController.java](/E:/Code/warehouse-management-system/backend/src/main/java/com/yocaihua/wms/controller/SystemController.java)

### Web 端

Web 端当前主要适合作为：

- 后端接口联调入口
- 业务页面参考实现
- 桌面端未覆盖场景的备用入口

### 桌面端

桌面端当前已接入的模块包括：

- 工作台
- 商品管理
- 客户管理
- 供应商管理
- 用户管理（管理员）
- 库存管理
- 库存流水
- 入库管理
- 出库管理
- AI 识别
- 系统设置

桌面端当前已可走通的主要业务链：

- 登录 / 退出
- 手工入库草稿 -> 详情 -> 确认 / 作废
- 手工出库草稿 -> 详情 -> 确认 / 作废
- AI 入库上传识别 -> 详情 -> 继续确认 -> 正式入库单
- AI 出库上传识别 -> 详情/列表 -> 继续确认 -> 正式出库单

桌面端模块入口定义可参考：

- [ShellModule.java](/E:/Code/warehouse-management-system/desktop-client/src/main/java/com/yocaihua/wms/desktop/ui/layout/ShellModule.java)

## 角色权限矩阵（MVP）

当前系统采用两类基础角色：

- `ADMIN`（管理员）
- `OPERATOR`（操作员）

### 管理员

- 可访问全部模块
- 可执行入库/出库单：确认、作废、批量确认、批量作废
- 可维护基础资料（商品/客户/供应商）与库存调整

### 操作员

- 可登录并访问业务查询与草稿编辑相关页面
- 默认仅可查看单据详情、编辑草稿
- 不允许执行入库/出库的确认与作废（含批量）

说明：

- Web 前端已按角色隐藏对应按钮
- 后端拦截器与 Service 层均已对确认/作废接口做管理员强校验（前后端双保险）

## 列表页能力（当前）

入库/出库列表当前均支持：

- 左侧固定导航与快捷筛选（如“只看草稿”）
- 主列表表格展示（状态标签、金额格式化、备注悬浮）
- 多条件筛选与快速查询
- 批量操作（管理员）
- 详情与列表往返保留筛选与分页状态（URL query 同步）
- 批量失败可读提示（失败单号与原因摘要）

## AI 模块拆分（当前）

为降低 `AiRecognitionServiceImpl` 复杂度，当前已完成第一轮职责拆分：

- `OcrAdapterService`：封装 Python OCR 调用适配
- `ProductMatchService`：承接商品/客户/供应商匹配逻辑
- `AiOrderAssemblerService`：承接 AI 确认后的订单与明细组装
- `AiDraftPersistenceService`：承接 AI 草稿持久化与明细实体转换

当前 `AiRecognitionServiceImpl` 以流程编排为主，后续可继续拆分“记录校验与VO组装”职责。

## 启动方式

建议本地启动顺序：

1. MySQL
2. Spring Boot 后端
3. Python AI 服务
4. Web 前端（如需）
5. JavaFX 桌面端

### Docker Compose 一键启动（Redis + MySQL + 后端 + Python AI）

在仓库根目录执行：

```powershell
docker compose up -d --build
```

查看运行状态：

```powershell
docker compose ps
```

验证服务：

```text
后端健康检查：http://127.0.0.1:8080/system/health
AI健康检查：http://127.0.0.1:9000/health
```

停止并清理容器网络：

```powershell
docker compose down
```

说明：

- Compose 文件路径：[docker-compose.yml](/E:/Code/warehouse-management-system/docker-compose.yml)
- MySQL 默认账号密码：`root/root`（仅本地开发）
- Redis 默认端口：`6379`（后端会话可切换到 Redis）
- 如需启用 LLM 解析，可在执行前设置环境变量 `DEEPSEEK_API_KEY`

### 1. 数据库

- 确保本机 MySQL 已启动
- 创建数据库并导入 SQL 脚本

当前 SQL 目录：

- [sql](/E:/Code/warehouse-management-system/sql)

常用初始化脚本：

- [wms.sql](/E:/Code/warehouse-management-system/sql/wms.sql)
- [demo_seed_mvp.sql](/E:/Code/warehouse-management-system/sql/demo_seed_mvp.sql)（演示账号与演示业务数据）

### 2. 后端启动

在 [backend](/E:/Code/warehouse-management-system/backend) 目录执行：

```powershell
cd backend
mvn spring-boot:run
```

如果你本机 Maven Wrapper 可用，也可以：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### 3. Python AI 服务启动

在 [python-ai-service](/E:/Code/warehouse-management-system/python-ai-service) 目录执行：

```powershell
cd python-ai-service
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 9000
```

如果没有使用仓库内 `.venv`，也可以改为系统 Python：

```powershell
cd python-ai-service
py -3 -m uvicorn app:app --host 127.0.0.1 --port 9000
```

### 4. Web 前端启动

在 [frontend](/E:/Code/warehouse-management-system/frontend) 目录执行：

```powershell
cd frontend
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

### 5. 桌面端启动

在 [desktop-client](/E:/Code/warehouse-management-system/desktop-client) 目录执行：

```powershell
cd desktop-client
mvn javafx:run
```

说明：

- 当前桌面端更稳的启动方式是 `javafx:run`
- 不建议直接把 [AppLauncher.java](/E:/Code/warehouse-management-system/desktop-client/src/main/java/com/yocaihua/wms/desktop/AppLauncher.java) 当普通 Java 程序运行

## 桌面端使用说明

桌面端登录后，建议优先进入：

- `系统设置`

当前设置页已支持：

- 查看 / 修改服务地址
- 记住服务地址
- 测试连接
- 重新执行启动检查
- 启动后端
- 启动 AI 服务
- 打开日志目录
- 查看配置文件路径 / 日志目录路径 / 启动日志文件名

说明：

- `启动后端` 和 `启动 AI 服务` 是第一版本地服务编排入口
- 它们会在启动命令发出后自动重新执行一轮启动检查
- 当前仍不是完整进程托管，只是从桌面端触发启动并自动刷新状态

## 当前已知边界

当前桌面端仍有这些边界：

- 供应商管理当前是第一版，只支持查询、分页、新增
- 客户 / 供应商 / 商品与业务页之间已接入返回联动，但仍不保留离开前的未保存整页状态
- 设置页已能启动本地服务，但还没有停止服务、查看进程和更完整的安装引导

## 最小可交付版（MVP）回归清单

建议每次发版前至少执行一次：

1. 登录与角色
- 管理员登录后可见确认/作废/批量操作入口
- 操作员登录后不可见上述入口，直接调用接口会被后端拒绝

2. 手工入库链
- 新增入库草稿
- 列表“编辑草稿”回填并更新成功
- 管理员确认入库后库存增加正确

3. 手工出库链
- 新增出库草稿
- 列表“编辑草稿”回填并更新成功
- 管理员确认出库后库存扣减正确，库存不足有拦截

4. 列表交互链
- 入库/出库列表筛选后进入详情，返回列表时筛选与分页保持不变
- 批量操作出现部分失败时，提示中可看到失败单号与原因摘要

5. 导出打印链
- 商品/供应商 Excel 导出可下载
- 入库/出库 PDF 可下载并可打印

## 文档入口

如果你要继续看桌面端设计和约定，先看这些文档：

- [docs/README.md](/E:/Code/warehouse-management-system/docs/README.md)
- [启动约定.md](/E:/Code/warehouse-management-system/docs/desktop/启动约定.md)
- [目录结构设计.md](/E:/Code/warehouse-management-system/docs/desktop/目录结构设计.md)
- [12周开发计划.md](/E:/Code/warehouse-management-system/docs/desktop/12周开发计划.md)

## 当前建议

如果你现在是第一次接手这个仓库，建议按这个顺序理解：

1. 先读本 README
2. 再看 [backend](/E:/Code/warehouse-management-system/backend) 和 [desktop-client](/E:/Code/warehouse-management-system/desktop-client) 目录
3. 先启动后端和桌面端
4. 进入桌面端 `系统设置` 检查连接状态
5. 再走一条完整业务链：
   - 手工入库
   - 手工出库
   - AI 入库确认

这样最容易把当前项目状态看清楚，而不是只停留在目录层面。
