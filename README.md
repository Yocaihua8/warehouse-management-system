# 仓库管理系统（Warehouse Management System）

一个基于 **Spring Boot + MyBatis + MySQL** 的仓库管理系统后端项目，围绕商品、客户、库存、入库单、出库单等核心业务进行实现，适合作为 Java 后端学习项目和个人项目展示。

---

## 1. 项目简介

本项目是一个面向中小型仓储场景的后端管理系统，当前已完成以下核心能力：

- 商品管理
- 客户管理
- 库存管理
- 入库单管理
- 出库单管理
- 全局异常处理
- 参数校验
- 删除安全控制
- 订单状态常量化

项目重点不只是基础 CRUD，还补充了业务校验、库存联动、删除受控化等改造，使系统具备更完整的业务约束能力。

---

## 2. 技术栈

- Java 17
- Spring Boot 3.5.11
- MyBatis
- MySQL
- Lombok
- Maven

---

## 3. 项目结构

```text
src/main/java/com/yocaihua/wms
├── common        # 通用返回结构、异常处理、常量
├── controller    # 接口层
├── dto           # 请求参数对象
├── entity        # 实体类
├── mapper        # MyBatis Mapper 接口
├── service       # 业务接口
├── service/impl  # 业务实现
└── vo            # 返回结果对象

src/main/resources
├── application.yaml
└── mapper        # MyBatis XML 映射文件
```

---

## 4. 已实现功能

### 4.1 商品管理

- 商品分页查询
- 商品新增
- 商品修改
- 商品详情查询
- 商品删除（受控删除）

### 4.2 客户管理

- 客户分页查询
- 客户新增
- 客户修改
- 客户详情查询
- 客户删除（受控删除）

### 4.3 库存管理

- 库存查询
- 库存修改
- 商品新增时自动初始化库存记录

### 4.4 入库管理

- 新增入库单
- 入库单分页查询
- 入库单详情查询
- 入库明细保存
- 入库后自动增加库存
- 入库业务校验（商品存在、重复商品校验、库存记录存在等）

### 4.5 出库管理

- 新增出库单
- 出库单分页查询
- 出库单详情查询
- 出库明细保存
- 出库后自动扣减库存
- 出库业务校验（客户存在、商品存在、库存记录存在、库存充足、重复商品校验等）

---

## 5. 当前已完成的关键改造

相比基础版 CRUD，本项目额外完成了以下增强：

1. **商品新增自动初始化库存记录**
2. **商品 / 入库 / 出库模块参数校验接通**
3. **全局异常处理支持参数校验异常返回**
4. **入库 / 出库 service 层增加业务存在性校验**
5. **商品删除受控化**
6. **客户删除受控化**
7. **订单状态常量化**

---

## 6. 数据库配置

当前项目默认数据库配置位于：

`src/main/resources/application.yaml`

默认数据库名：

```yaml
wms
```

默认端口：

```yaml
8080
```

MySQL 连接示例：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/wms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的数据库密码
```

> 建议不要将真实数据库密码直接提交到 GitHub，后续可改为本地配置或环境变量方式。

---

## 7. 启动步骤

### 7.1 准备环境

- JDK 17
- Maven 3.9+
- MySQL 8.x
- IntelliJ IDEA（推荐）

### 7.2 创建数据库

先在 MySQL 中创建数据库：

```sql
CREATE DATABASE wms DEFAULT CHARACTER SET utf8mb4;
```

### 7.3 导入表结构

将项目对应的建表 SQL 导入到 `wms` 数据库中。

### 7.4 修改数据库配置

打开 `application.yaml`，修改为你自己的：

- 用户名
- 密码
- 数据库地址

### 7.5 启动项目

运行主启动类：


运行主启动类：`WarehouseManagementSystemApplication`


启动成功后，默认访问端口：

```text
http://localhost:8080
```

---

## 8. 主要接口示例

### 商品

- `GET /product/list`
- `POST /product/add`
- `PUT /product/update`
- `DELETE /product/delete/{id}`
- `GET /product/{id}`

### 客户

- `GET /customer/list`
- `POST /customer/add`
- `PUT /customer/update`
- `DELETE /customer/delete/{id}`
- `GET /customer/{id}`

### 库存

- `GET /stock/list`
- `PUT /stock/update`

### 入库单

- `POST /inbound-order/add`
- `GET /inbound-order/list`
- `GET /inbound-order/{id}`

### 出库单

- `POST /outbound-order/add`
- `GET /outbound-order/list`
- `GET /outbound-order/{id}`

---

## 9. 验收测试建议

可以通过以下场景验证项目核心能力：

1. 新增商品后，自动生成对应库存记录
2. 商品编码为空时，新增商品失败
3. 出库时客户不存在，保存失败
4. 出库时库存不足，保存失败
5. 入库单中同一商品重复出现，保存失败
6. 已被单据引用的商品不能删除
7. 已被出库单引用的客户不能删除

---

## 10. 后续优化方向

- 补充 Customer / Stock 模块的 `@Valid`
- 增加逻辑删除能力
- 增加统一枚举/常量管理
- 增加接口测试
- 补充数据库初始化 SQL
- 增加 README 中的接口示例请求体
- 增加 Swagger / Knife4j 文档支持
- 增加登录与权限控制

---

## 11. 项目定位

这是一个以 **Java 后端分层设计 + 仓储业务主链路实现** 为核心的练习项目，重点体现了：

- 分层结构设计
- MyBatis 数据访问
- 事务控制
- 参数校验
- 业务校验
- 库存联动
- 删除安全控制

适合作为：

- Java 后端学习项目
- 课程设计/毕业设计后端部分
- GitHub 个人项目展示
- 简历项目基础版本


## 2. 根目录最好补一个 SQL 文件

比如：

* `sql/wms.sql`

哪怕先只放建表 SQL，也比 README 里只写“请导入表结构”强很多。

---
