# 测试策略

> 本文档描述项目的测试分层策略、各层的覆盖要求和运行方式。

---

## 1. 测试金字塔

```
        ┌─────────────┐
        │   E2E 测试   │  少量，验证关键业务链路（暂未实现，见待办）
        ├─────────────┤
        │  集成测试    │  API 级别验证（.http 文件手动执行）
        ├─────────────┤
        │  单元测试    │  Service 层核心业务逻辑（Mockito，持续扩展）
        └─────────────┘
```

---

## 2. 后端测试

### 2.1 单元测试（Mockito）

**位置**：`backend/src/test/java/com/yocaihua/wms/service/impl/`

**测试目标**：Service 层核心业务逻辑，Mock 所有外部依赖（Mapper、外部服务）。

**已覆盖的测试类**：

| 测试类 | 覆盖场景 |
|--------|---------|
| `CustomerServiceImplTest` | 分页参数归一化与 fallback、详情成功/不存在、新增默认状态/重复编码/自定义字段 JSON 归一化与非法 JSON 拒绝/非法状态/插入失败、修改重复编码/继承状态/自定义字段 JSON 归一化与非法 JSON 拒绝/非法状态/更新失败、删除不存在/引用校验/删除失败、Excel 导出 |
| `DashboardServiceImplTest` | 首页汇总计数、趋势参数校验、缺口日期补零、空行/空日期/空 total 归一化 |
| `InboundOrderServiceImplTest` | 保存入库单成功主链、重复商品/供应商停用/商品不存在/库存记录不存在/单头保存失败/明细为空/明细保存失败；确认入库不存在/已完成/明细为空/状态更新失败/非管理员/成功；作废不存在/草稿成功/已完成明细为空/已完成回滚；草稿编辑不存在/非草稿/头更新失败/成功；分页参数归一化与非法来源/非法状态；详情查询、Excel/PDF 导出与旧详情映射 |
| `LowStockAlertServiceImplTest` | 管理员手动触发、低库存筛选、冷却去重、邮件/Webhook 渠道发送、定时检查异常吞掉 |
| `OperationLogServiceImplTest` | 写日志字段归一化、写入失败容错、分页参数默认值/上限与筛选字段 trim |
| `OutboundOrderServiceImplTest` | 保存出库单成功主链、明细为空、客户不存在/停用、重复商品、商品不存在、库存记录不存在、库存不足、单头保存失败、明细保存失败；草稿编辑不存在/非草稿/明细为空/客户不存在或停用/重复商品/商品不存在/库存记录不存在/库存不足/头更新失败/明细保存失败/成功；确认不存在/已完成/已作废/明细为空/状态更新失败/库存不足/非管理员/成功；作废不存在/已作废/草稿成功/已完成明细为空/已完成状态更新失败/成功；分页参数归一化与非法状态；详情查询；Excel/PDF 导出 |
| `ProductServiceImplTest` | 分页边界、详情不存在、新增商品后库存初始化、自定义字段 JSON 校验、删除引用/库存校验 |
| `StockAdjustLogServiceImplTest` | 库存流水分页默认值、页大小上限、offset 计算与 `productName` 原样透传 |
| `StockFlowServiceImplTest` | 入库/出库库存增减、库存不足提示、作废回滚、手工调整日志写入 |
| `StockServiceImplTest` | 分页默认值、页大小上限、后续页 offset、库存记录不存在、空数量/空预警值、手工调整成功、默认操作人与默认原因、Excel 导出与 CSV 导出 |
| `SupplierServiceImplTest` | 分页参数边界与 offset、查询关键字原样透传、详情成功/不存在、新增重复编码/自定义字段 JSON 归一化与非法 JSON 拒绝/插入失败、修改重复编码/自定义字段 JSON 归一化与非法 JSON 拒绝/更新失败、删除不存在/引用校验/删除失败、Excel 导出 |
| `SystemServiceImplTest` | 系统健康检查、数据库/AI 状态汇总、AI 消息回退、bootstrap 配置映射与 AI URL 默认值 |
| `UserServiceImplTest` | 登录、当前用户、Admin-only 用户分页/新增/编辑/删除、独立重置密码、默认管理员保护、当前用户自保护 |
| `AiRecognitionServiceImplTest` | AI 识别异常路径；入库/出库成功识别后的草稿落库、状态更新与结果返回；确认成单成功路径；重复确认 / `markConfirmedToOrder<=0`；出库确认中的未匹配客户、明细写入失败、客户不存在、客户名称不能为空、匹配后客户不存在与客户显示名不能为空；入库确认中的供应商不存在与供应商名称不能为空 |
| `AbstractOrderServiceSupportTest` | 管理员权限、作废原因/备注、分页归一化、文本/金额/时间格式化、Excel 汇总行写入与 Jasper 模板加载 |

**运行方式**：

```bash
cd backend
mvn test

# 指定单个测试类
mvn test -Dtest=InboundOrderServiceImplTest
```

**覆盖要求**：

| 层 | 覆盖目标 |
|----|---------|
| Service 层核心业务（状态流转、权限校验、库存联动） | 关键路径 + 异常路径均需测试 |
| 新增 Admin-only 操作 | 必须包含"非 Admin 调用被拒绝"用例 |
| 涉及库存变更的操作 | 必须包含增减正确性 + 作废回滚用例 |

### 2.2 API 手动测试（.http 文件）

**位置**：`backend/src/test/http/`

| 文件 | 用途 |
|------|------|
| `product-test.http` | 商品管理接口验证 |
| `test-aiassit.http` | AI 识别接口验证 |
| `test-dashboard.http` | Dashboard 接口验证 |
| `test-stock-update.http` | 库存手动调整验证 |
| `test-stock.http` | 库存查询验证 |
| `test-user.http` | 用户管理接口验证 |

可用 IntelliJ IDEA 的 HTTP Client 或 VS Code REST Client 执行。

### 2.3 数据库迁移测试

每次新增 Flyway 迁移脚本后必须验证：

1. **空库场景**：删除本地 `wms` 数据库，重新创建空库，启动后端确认 Flyway 全量迁移成功
2. **已有库场景**：在现有库上启动，确认增量迁移成功，无历史数据破坏

```bash
# 验证启动成功
curl http://localhost:8080/system/health
```

### 2.4 覆盖率报告（JaCoCo）

后端已接入 JaCoCo，当前已启用 `service.impl` 包级行覆盖率门槛。

**运行方式**：

```bash
cd backend
mvn verify
```

**报告输出**：

- HTML 报告：`backend/target/site/jacoco/index.html`
- XML 报告：`backend/target/site/jacoco/jacoco.xml`
- CSV 报告：`backend/target/site/jacoco/jacoco.csv`

**当前策略**：

- `backend/pom.xml` 已在 `verify` 阶段启用 `com.yocaihua.wms.service.impl` 包级 **行覆盖率 `80%`** 检查
- `.github/workflows/ci.yml` 的后端 job 已从 `./mvnw test` 切换为 `./mvnw verify`
- Linux runner 中已补 `chmod +x ./mvnw`，避免 Maven Wrapper 执行权限问题
- 当前门槛只卡 **LINE / COVEREDRATIO >= 0.80**，分支覆盖率继续作为观察指标，不直接卡死

**当前基线（2026-04）**：

- `com.yocaihua.wms.service.impl` 指令覆盖率：`91.34%`
- `com.yocaihua.wms.service.impl` 行覆盖率：`91.73%`
- `com.yocaihua.wms.service.impl` 分支覆盖率：`76.34%`

**当前已覆盖较多的 Service**：

- `CustomerServiceImpl`：行覆盖率约 `95.65%`
- `DashboardServiceImpl`：行覆盖率 `100%`
- `LowStockAlertServiceImpl`：行覆盖率约 `87.74%`
- `OperationLogServiceImpl`：行覆盖率 `100%`
- `ProductServiceImpl`：行覆盖率约 `65.89%`
- `StockAdjustLogServiceImpl`：行覆盖率 `100%`
- `StockFlowServiceImpl`：行覆盖率约 `93.01%`
- `StockServiceImpl`：行覆盖率约 `96.70%`
- `SupplierServiceImpl`：行覆盖率约 `97.17%`
- `SystemServiceImpl`：行覆盖率 `100%`
- `UserServiceImpl`：行覆盖率约 `97.67%`
- `InboundOrderServiceImpl`：行覆盖率约 `92.04%`
- `OutboundOrderServiceImpl`：行覆盖率约 `95.04%`
- `AbstractOrderServiceSupport`：行覆盖率 `100%`
- `AiRecognitionServiceImpl`：行覆盖率约 `86.26%`

**门槛评估结论（2026-04）**：

1. 当前 `service.impl` 行覆盖率约 `91.73%`，已经稳定越过包级 `80%` 门槛，并对后续继续上调门槛留出了余量
2. 当前已在 `backend/pom.xml` 启用包级 `LINE >= 80%` 检查，并在 GitHub Actions 后端 job 中切换到 `./mvnw verify`
3. 当前先只卡行覆盖率，不额外叠加分支覆盖率门槛，避免在低价值分支上过早阻塞构建
4. 如果后续继续抬升核心服务覆盖率，可再评估：
   - 包级 `BRANCH` 门槛
   - 或把包级 `LINE` 从 `80%` 再逐步上调到 `85%`

**建议分阶段门槛方案（2026-04）**：

1. 阶段 A：集中补核心低覆盖服务
   - 已完成：`InboundOrderServiceImpl`、`OutboundOrderServiceImpl`、`StockServiceImpl`
   - 结果：`service.impl` 包级行覆盖率已提升到约 `91.73%`
2. 阶段 B：启用包级基础门槛
   - 已完成：`service.impl` 包级 `LINE >= 80%`
   - 目标：先防止整体覆盖率回退
3. 阶段 C：继续提高门槛强度
   - 备选路径：
     - 包级 `LINE` 提升到 `85%`
     - 或新增包级 `BRANCH` 基线门槛
   - 下一批若继续补测，优先对象：
     1. `ProductServiceImpl`
     2. 仍需更强异常覆盖的 AI / 订单边角分支
     3. 如需继续提高门槛，再回头补低收益导出或 helper 边角分支

**如果未来要把门槛接入 CI，前置条件必须同时满足**：

1. `backend/pom.xml` 已增加 `jacoco:check`
2. `.github/workflows/ci.yml` 的后端 job 已切换到 `./mvnw verify`
3. Linux runner 已补 `chmod +x ./mvnw`
4. 当前仓库已完成 `protect-main` 规则配置，`backend-test` / `frontend-test` 已设为 required checks；如果后续重建仓库，需先让两项 job 成功运行一次，再重新绑定检查项

---

## 3. 前端测试

### 3.1 当前状态

前端已接入 Vitest，当前已覆盖 composable 纯逻辑层、关键组件渲染 / 交互测试，以及创建页保存链首批页面级联动测试。

### 3.2 已配置：Vitest 单测

当前已引入 Vitest，并完成 composable + 关键组件首批单元测试：

```bash
npm test
```

**当前已覆盖**：

| 目标 | 优先级 |
|------|:------:|
| `useOrderCalc`：行金额计算、合计数量/金额 | 高 |
| `useOrderValidation`：字段校验、行校验、单据级校验 | 高 |
| `useOrderItems`：增删插改行、最小行数限制 | 中 |
| `useInboundCreatePage`：保存入库草稿 happy path / error path、成功后路由跳转 | 高 |
| `useOutboundCreatePage`：保存出库草稿 happy path / error path、成功后路由跳转 | 高 |
| `OrderItemTable`：工具条按钮、商品选择/清空事件、出库库存列与合计行、只读展示 | 高 |
| `ProductSelectDialog`：搜索输入、快速新建、当前已选商品标签、确认/取消交互 | 高 |
| `MainLayout`：侧边栏折叠按钮、220px/64px 宽度切换、`localStorage` 持久化与折叠菜单标题提示 | 高 |
| `productCustomFields`：自定义字段 JSON 解析、摘要生成、空字段名 / 重复字段名校验与序列化 | 中 |
| `ProductCustomFieldsEditor`：新增字段、修改键值、删除字段行与 `v-model` 同步 | 中 |
| `InboundOrderCreate`：保存草稿、保存并新建、智能识别导入按钮与页面 composable 联动 | 高 |
| `OutboundOrderCreate`：保存草稿、保存并新建、智能识别导入按钮与页面 composable 联动 | 高 |

**测试文件位置**：

| 文件 | 说明 |
|------|------|
| `frontend/src/composables/__tests__/useOrderCalc.spec.js` | 行金额与合计计算 |
| `frontend/src/composables/__tests__/useOrderValidation.spec.js` | 入库/出库/AI 导入校验 |
| `frontend/src/composables/__tests__/useOrderItems.spec.js` | 明细增删插改与商品回填 |
| `frontend/src/composables/__tests__/useInboundCreatePage.spec.js` | 入库创建页保存草稿 happy/error path、成功后跳转列表 |
| `frontend/src/composables/__tests__/useOutboundCreatePage.spec.js` | 出库创建页保存草稿 happy/error path、成功后跳转列表 |
| `frontend/src/components/order/__tests__/OrderItemTable.spec.js` | 明细表工具条、商品列事件、出库库存列与只读渲染 |
| `frontend/src/components/order-workbench/__tests__/ProductSelectDialog.spec.js` | 商品弹窗搜索、快速新建、当前选中商品确认与取消 |
| `frontend/src/layouts/__tests__/MainLayout.spec.js` | 侧边栏折叠按钮、状态持久化与折叠态标题提示 |
| `frontend/src/utils/__tests__/productCustomFields.spec.js` | 自定义字段解析、序列化、摘要与校验 |
| `frontend/src/components/product/__tests__/ProductCustomFieldsEditor.spec.js` | 键值编辑器新增 / 删除字段行与双向绑定 |
| `frontend/src/views/inbound/__tests__/InboundOrderCreate.spec.js` | 入库创建页底部操作条事件联动、智能识别入口与保存并新建聚焦 |
| `frontend/src/views/outbound/__tests__/OutboundOrderCreate.spec.js` | 出库创建页底部操作条事件联动、智能识别入口与保存并新建聚焦 |

**当前与自定义字段相关的回归保护**：

- 商品 `customFieldsJson`：前端工具层 + 键值编辑器组件单测
- 客户 / 供应商 `customFieldsJson`：后端 Service 层 JSON 归一化与非法 JSON 拒绝用例
- 客户 / 供应商前端页面当前复用商品键值编辑器与序列化工具，主要通过组件 / 工具单测 + 构建验证覆盖

**当前未覆盖**：

- `useOrderForm`：pageMode 映射、草稿加载
- AI 识别弹窗自身确认成单链路（成功 / 异常 / 跳详情）
- 端到端联调测试

---

## 4. Python AI 服务测试

### 4.1 健康检查

```bash
curl http://127.0.0.1:9000/health
```

### 4.2 手动验证识别接口

```bash
curl -X POST http://127.0.0.1:9000/ocr/inbound/recognize \
  -F "file=@/path/to/test-image.jpg"
```

---

## 5. 回归测试清单（发版前执行）

以下核心业务链必须人工走通：

### 入库链
- [ ] 新建入库草稿（商品、数量、单价）
- [ ] 编辑草稿（修改数量、新增一行）
- [ ] Admin 确认入库 → 库存增加正确
- [ ] Admin 作废已完成入库单 → 库存回滚正确

### 出库链
- [ ] 新建出库草稿（含库存余量展示）
- [ ] 库存不足场景 → 拒绝创建草稿
- [ ] Admin 确认出库 → 库存扣减正确
- [ ] Admin 作废已完成出库单 → 库存补回正确

### 权限链
- [ ] Operator 尝试确认/作废 → 后端返回 403
- [ ] Operator 尝试调整库存 → 后端返回 403

### AI 识别链
- [ ] 上传图片 → 识别结果展示
- [ ] 修改识别结果 → Admin 确认 → 生成正式单据
- [ ] Python 服务不可用 → AI 功能返回错误，其余功能正常

### 桌面端链
- [ ] 打开桌面端系统设置页，确认后端 / 数据库 / AI 状态可见
- [ ] 点击“启动后端”或“启动 AI 服务”，确认日志目录生成对应日志文件且状态刷新
- [ ] 当 AI 服务已在后台运行时，再次点击“启动 AI 服务”，确认页面提示“当前已可用，无需重复启动”，且不会保留旧失败原因
- [ ] 点击“停止后端”或“停止 AI 服务”，确认状态回刷为 DOWN 或不可用
- [ ] 点击“重新执行启动检查”，确认最近刷新时间和失败原因按当前环境更新
- [ ] 系统设置页中的“AI 服务地址”应显示 `http://127.0.0.1:9000` 或服务端返回值，不应误显示为后端 `serverBaseUrl`

### 打印导出链
- [ ] 入库单打印预览正常
- [ ] 商品列表 Excel 导出可下载

---

## 6. 待建设事项

| 事项 | 优先级 | 说明 |
|------|:------:|------|
| GitHub Actions Required Checks 巡检 | 低 | 当前仓库已配置 `protect-main`；若后续重建仓库或迁移仓库，需重新确认 `backend-test` / `frontend-test` 仍为 required checks |
| 后端更高覆盖率门槛评估 | 低 | 当前 `service.impl` 已启用包级 `LINE >= 80%`；待低覆盖服务补测后，再评估是否提高到 `85%`，或追加 `BRANCH` 门槛 |
| 前端页面级联动测试 | 中 | 当前已覆盖创建页保存链 happy/error path；后续补浏览器级保存链、AI 识别弹窗确认成单链与跨页面回归 |
| 集成测试（Testcontainers） | 低 | 使用 Testcontainers 启动真实 MySQL，测试 Flyway + Mapper 层 |
