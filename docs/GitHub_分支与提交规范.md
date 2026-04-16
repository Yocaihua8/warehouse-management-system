# GitHub 分支与提交规范

---

## 1. 分支策略

### 长期分支

| 分支 | 用途 |
|------|------|
| `main` | 稳定可展示版本，不直接提交功能开发 |
| `dev` | 日常开发集成分支，功能完成后从此合入 main |

### 短期分支（按需创建）

从 `dev` 拉取，完成后合并回 `dev`：

```
feature/<description>   新功能
fix/<description>        问题修复
refactor/<description>   重构
docs/<description>       文档更新
chore/<description>      维护类改动
```

**示例：**
- `feature/user-management-ui`
- `fix/stock-update-concurrent`
- `docs/feature-spec-rewrite`

---

## 2. 提交信息规范

格式：`<type>: <简要描述>`

| type | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | 问题修复 |
| `refactor` | 重构（不影响功能） |
| `docs` | 文档更新 |
| `test` | 测试相关 |
| `chore` | 维护、依赖、配置 |

**示例：**
```
feat: 新增用户管理前端页面
fix: 修复出库确认时库存并发扣减问题
refactor: 拆分 AiRecognitionServiceImpl 为 OcrAdapterService 和 ProductMatchService
docs: 补充功能规格文档与 BACKLOG
chore: 升级 springdoc-openapi 至 2.8.6
```

**避免：** `update`、`修改`、`继续`、`bugfix`、`test` 等无意义描述。

---

## 3. 合并原则

**功能分支合入 dev 前：**
1. 功能目标已完成
2. 本地启动测试通过
3. 无明显编译错误
4. 不含无关试验代码

**dev 合入 main 前：**
1. 核心流程可完整跑通
2. README 已更新
3. 无遗留的调试代码或 TODO

### GitHub Required Checks（建议开启）

仓库已提供 GitHub Actions 工作流：`backend-test`、`frontend-test`。  
如果希望“测试失败的 PR 不能合并”，还需要在 GitHub 仓库中额外配置分支保护规则。

**建议配置步骤：**

1. 打开仓库 `Settings`
2. 进入 `Branches` 或 `Rulesets`
3. 为 `main`（必要时也包括 `dev`）创建保护规则
4. 开启 `Require a pull request before merging`
5. 开启 `Require status checks to pass before merging`
6. 勾选以下检查项：
   - `backend-test`
   - `frontend-test`
7. 建议同时开启 `Require branches to be up to date before merging`

**说明：**

- `.github/workflows/ci.yml` 只能负责“自动执行测试”
- 只有在仓库里把 CI job 配成 required checks 后，GitHub 才会真正阻止失败测试的合并
- 如果后续新增 CI job（如 `coverage`、`e2e-test`），也要同步更新 required checks 列表

---

## 4. 版本标签

使用语义化版本：`vMAJOR.MINOR.PATCH`

- `MAJOR`：重大架构变更
- `MINOR`：新增功能
- `PATCH`：Bug 修复

当前版本历史见 [项目路线图.md](./项目路线图.md)。
