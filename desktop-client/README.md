# desktop-client

这是仓库管理系统的 JavaFX 桌面客户端工程。  
当前它已经不是“启动骨架”，而是一个已接入真实业务模块的桌面端第一版。

## 当前定位

桌面端当前目标不是替换全部 Web 页面，而是先把本地单机部署场景下最常用的业务链接起来，包括：

- 登录与本地配置
- 商品 / 客户 / 供应商基础资料维护
- 库存与库存流水查询
- 手工入库 / 手工出库
- AI 入库识别与确认
- AI 出库上传识别与确认
- 系统设置与本地服务状态检查

## 当前已接入模块

模块入口定义见：

- [ShellModule.java](/E:/Code/warehouse-management-system/desktop-client/src/main/java/com/yocaihua/wms/desktop/ui/layout/ShellModule.java)

当前左侧导航模块包括：

- 工作台
- 商品管理
- 客户管理
- 供应商管理
- 库存管理
- 入库管理
- 出库管理
- AI识别
- 系统设置

## 当前可用能力

### 通用能力

- 启动检查
- 登录 / 退出
- 顶部服务状态摘要
- 系统设置页
- 本地配置持久化

### 基础资料

- 商品管理：查询、分页、新增、编辑、删除
- 客户管理：查询、分页、新增、编辑、删除
- 供应商管理：查询、分页、新增

### 库存

- 库存列表
- 库存导出
- 库存流水
- 从库存流水跳转关联单据详情

### 入库

- 入库单列表
- 新增入库草稿
- 商品搜索选择
- 供应商搜索选择
- 确认入库 / 作废
- 详情返回列表状态保留

### 出库

- 出库单列表
- 新增出库草稿
- 商品搜索选择
- 客户搜索选择
- 确认出库 / 作废
- 详情返回列表状态保留

### AI

- AI 入库历史列表
- AI 入库详情
- AI 入库继续确认
- AI 入库上传识别
- AI 出库上传识别
- AI 出库确认

### 设置页

当前设置页已支持：

- 查看 / 修改服务地址
- 记住服务地址
- 测试连接
- 重新执行启动检查
- 启动后端
- 启动 AI 服务
- 打开日志目录
- 展示配置文件路径
- 展示日志目录路径
- 展示启动日志文件名

## 启动方式

### 环境前提

- JDK 17
- Maven
- 本机已启动后端或允许桌面端从设置页触发启动
- 如需 AI 能力，还需要 Python AI 服务可用

### 推荐启动命令

在 [desktop-client](/E:/Code/warehouse-management-system/desktop-client) 目录执行：

```powershell
cd desktop-client
mvn javafx:run
```

### 为什么不建议直接运行 `AppLauncher.main()`

当前项目的 JavaFX 运行时参数是通过 Maven 插件接入的。  
所以开发阶段最稳的方式仍然是：

```powershell
mvn javafx:run
```

启动入口类在这里：

- [AppLauncher.java](/E:/Code/warehouse-management-system/desktop-client/src/main/java/com/yocaihua/wms/desktop/AppLauncher.java)

## 推荐使用顺序

第一次进入桌面端，建议按这个顺序使用：

1. 先进入 `系统设置`
2. 检查服务地址和启动状态
3. 如有需要，点击：
   - `启动后端`
   - `启动 AI 服务`
4. 再回到业务模块测试：
   - 手工入库
   - 手工出库
   - AI 入库确认
   - AI 出库确认

## 当前已知边界

当前桌面端还不是最终交付版，主要边界有：

- 供应商管理目前只有查询 / 分页 / 新增
- AI 出库当前没有历史列表 / 详情页，属于“上传后直接确认”链
- 设置页能触发本地服务启动，但还没有完整的停止服务 / 进程托管能力
- 当前更偏开发态单机运行，还不是完整安装包交付

## 相关文档

- [根目录 README](/E:/Code/warehouse-management-system/README.md)
- [启动约定.md](/E:/Code/warehouse-management-system/docs/desktop/启动约定.md)
- [目录结构设计.md](/E:/Code/warehouse-management-system/docs/desktop/目录结构设计.md)
- [12周开发计划.md](/E:/Code/warehouse-management-system/docs/desktop/12周开发计划.md)
