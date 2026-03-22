# 仓库管理系统（Warehouse Management System）

一个基于 **Spring Boot + MyBatis + MySQL** 的仓库管理系统后端项目，围绕 **商品、客户、库存、入库单、出库单** 等核心业务展开实现。

该项目不只停留在基础 CRUD，还补充了 **参数校验、全局异常处理、库存联动、删除受控、业务存在性校验、订单状态常量化** 等后端常见能力，适合作为 **Java 后端学习项目、个人项目展示项目、简历项目**。

---

## 项目亮点

- 基于 **Spring Boot + MyBatis** 搭建标准后端分层结构
- 实现 **商品 / 客户 / 库存 / 入库 / 出库** 主业务链路
- 入库自动增加库存，出库自动扣减库存，体现库存联动逻辑
- 增加 `@Valid` 参数校验与统一异常处理，提高接口健壮性
- 对商品、客户删除增加引用校验，避免脏数据产生
- 对入库 / 出库流程补充业务校验，如客户存在、商品存在、库存充足、重复商品校验等

---

## 技术栈

- Java 17
- Spring Boot 3.5.11
- MyBatis
- MySQL
- Lombok
- Maven

---

## 项目结构

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

## 已实现功能

### 1. 商品管理

* 商品分页查询
* 商品新增
* 商品修改
* 商品详情查询
* 商品删除（受控删除）

### 2. 客户管理

* 客户分页查询
* 客户新增
* 客户修改
* 客户详情查询
* 客户删除（受控删除）

### 3. 库存管理

* 库存查询
* 库存修改
* 商品新增时自动初始化库存记录

### 4. 入库管理

* 新增入库单
* 入库单分页查询
* 入库单详情查询
* 入库明细保存
* 入库后自动增加库存
* 入库业务校验（商品存在、重复商品校验、库存记录存在等）

### 5. 出库管理

* 新增出库单
* 出库单分页查询
* 出库单详情查询
* 出库明细保存
* 出库后自动扣减库存
* 出库业务校验（客户存在、商品存在、库存记录存在、库存充足、重复商品校验等）

---

## 关键改造

相比基础版 CRUD，本项目额外完成了以下增强：

* 商品新增自动初始化库存记录
* 商品 / 入库 / 出库模块接入参数校验
* 全局异常处理支持参数校验异常返回
* 入库 / 出库 service 层增加业务存在性校验
* 商品删除受控化
* 客户删除受控化
* 订单状态常量化

---

## 运行环境

* JDK 17
* Maven 3.9+
* MySQL 8.x
* IntelliJ IDEA（推荐）

---

## 快速启动

### 1. 创建数据库

```sql
CREATE DATABASE wms DEFAULT CHARACTER SET utf8mb4;
```

### 2. 导入表结构

将 `sql/wms.sql` 导入到本地 MySQL 数据库。

### 3. 修改数据库配置

修改 `src/main/resources/application.yaml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/wms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的数据库密码
```

### 4. 启动项目

运行主启动类：

`WarehouseManagementSystemApplication`

启动成功后默认访问：

```text
http://localhost:8080
```

---

## 接口示例

### 商品

* `GET /product/list`
* `POST /product/add`
* `PUT /product/update`
* `DELETE /product/delete/{id}`
* `GET /product/{id}`

### 客户

* `GET /customer/list`
* `POST /customer/add`
* `PUT /customer/update`
* `DELETE /customer/delete/{id}`
* `GET /customer/{id}`

### 库存

* `GET /stock/list`
* `PUT /stock/update`

### 入库单

* `POST /inbound-order/add`
* `GET /inbound-order/list`
* `GET /inbound-order/{id}`

### 出库单

* `POST /outbound-order/add`
* `GET /outbound-order/list`
* `GET /outbound-order/{id}`

---

## 测试建议

可以通过以下场景验证项目核心能力：

1. 新增商品后，自动生成对应库存记录
2. 商品编码为空时，新增商品失败
3. 出库时客户不存在，保存失败
4. 出库时库存不足，保存失败
5. 入库单中同一商品重复出现，保存失败
6. 已被单据引用的商品不能删除
7. 已被出库单引用的客户不能删除

---

## 后续优化方向

* 补充 Customer / Stock 模块参数校验
* 增加数据库初始化 SQL 与测试数据
* 增加 Swagger / Knife4j 接口文档
* 增加登录与权限控制
* 增加统一枚举与常量管理
* 增加接口测试与单元测试
* 补充 Docker 部署能力

---

## 项目定位

这是一个以 **Java 后端分层设计 + 仓储业务主链路实现** 为核心的练习项目，重点体现了：

* 分层结构设计
* MyBatis 数据访问
* 参数校验
* 统一异常处理
* 业务校验
* 库存联动
* 删除安全控制

适合作为：

* Java 后端学习项目
* 课程设计 / 毕业设计后端部分
* GitHub 个人项目展示
* 简历项目基础版本

```