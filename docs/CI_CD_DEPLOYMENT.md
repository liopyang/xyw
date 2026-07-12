# CI/CD 构建与生产发布

## 执行位置

GitHub-hosted Ubuntu Runner 执行：

- 检出代码。
- Java 17 和 Maven Wrapper 后端 `clean verify`。
- 管理端 `npm ci`、类型检查和生产构建。
- 微信小程序 `npm ci`、类型检查和构建检查。
- 检查 JAR、`dist/index.html`、JS、CSS 和小程序产物。
- 生成带 commit SHA、UTC 构建时间和 SHA-256 校验和的发布包。

阿里云生产服务器只执行：

- 接收已经构建完成的 JAR 和管理端压缩包。
- 校验 SHA、校验和、文件大小、JAR 和压缩包结构。
- 保存当前运行版本。
- 原子替换后端 JAR，先发布新哈希资源、最后替换前端 `index.html`。
- 重启 `campus-business` 并执行本机、外网健康检查。
- 失败时调用固定路径回滚脚本。

生产服务器不安装 Actions Runner，也不执行 Maven、npm 或 Vite。

## CI 触发规则

`.github/workflows/ci.yml` 在以下情况运行：

- push 到 `dev`。
- push 到 `feature/**`。
- push 到 `fix/**`。
- 面向 `dev` 或 `main` 的 Pull Request。

任何测试、类型检查或构建失败都会使对应 job 失败，不会进入生产部署。

## 生产触发和批准

`.github/workflows/deploy-production.yml` 在 `main` 更新时重新测试并生成发布包，也支持手工 `workflow_dispatch`。真正的 deploy job 绑定 GitHub Environment `production`。

第一次启用前必须在 GitHub 网页为 `production` 配置 Required reviewers。若套餐不支持审批，请先将工作流调整为仅 `workflow_dispatch`，再手工触发。

并发组固定为 `campus-production`，不会取消正在执行的生产部署，也不会同时运行两次发布。

## 发布目录

```text
/opt/campus-business/incoming/<commit-sha>/   GitHub 临时上传
/opt/campus-business/releases/<commit-sha>/   已验证发布版本
/opt/campus-business/releases/legacy-*/       首次发布前的现网快照
/opt/campus-business/CURRENT_RELEASE          当前版本标识
/opt/campus-business/scripts/                 root 拥有的固定部署脚本
```

管理端仍发布到 `/www/wwwroot/hutbxyw.click`，不修改当前 Nginx root。脚本保留旧哈希资源，避免已打开的旧页面出现资源 404。

## 健康检查

新后端只公开：

```text
GET /api/actuator/health
```

响应仅包含健康状态，不公开数据库地址、环境变量或内部组件详情。部署依次检查：

1. `systemctl is-active campus-business`。
2. `http://127.0.0.1:18080/api/actuator/health` 返回 `UP`。
3. `https://hutbxyw.click/` 为非空应用页面。
4. `https://hutbxyw.click/api/actuator/health` 返回 `UP`。

健康检查不创建、修改或删除任何业务数据。

服务器脚本先从服务器侧检查公网；随后 GitHub Runner 再独立检查一次。如果 Runner 侧检查失败，工作流会调用固定回滚脚本恢复上一版本并明确标记部署失败。

## 首次安全测试发布

1. 完成 [setup-deploy-user.md](../deploy/server/setup-deploy-user.md)。
2. 在 GitHub 创建 `production` Environment，配置 Required reviewer 和 Environment Secrets。
3. 先创建初始提交和 `dev`，通过一个无生产影响的 `feature/ci-smoke-test` Pull Request 验证 CI。
4. 确认三个 CI job 全部通过。
5. 将 `dev` 合并到 `main`。
6. 生产工作流完成构建后，先核对 commit SHA，再批准 deploy job。
7. 部署脚本第一次运行会自动把当前现网 JAR 和前端保存为 `legacy-<timestamp>`，因此第一次发布也可回滚。
8. 部署后检查 Actions Summary、管理端登录、订单只读列表和服务器日志。

## 数据库边界

当前项目使用 `sql/schema.sql` 和 `sql/data.sql` 手工管理结构，没有 Flyway 或 Liquibase。工作流不会执行这些 SQL，不会迁移生产数据库，也不会重建 `campus-mysql` 或修改 `campus_mysql_data`。

代码回滚不等于数据库回滚。未来引入迁移工具时，应单独设计向前兼容、备份和人工审批流程。

## 已知依赖风险

2026-07-12 本地审计结果：

- 管理端有 2 个 moderate，来自 `exceljs` 间接使用的旧版 `uuid`。
- 小程序依赖树有 41 个告警，其中 11 个 high；排除开发依赖后仍有 8 个 high，主要来自当前固定的 DCloud/uni-app 依赖链。

当前版本可以构建，但不能用 `npm audit fix --force` 直接强制升级，因为这会跨越 uni-app 和 Vite 的兼容边界。应建立独立升级分支，升级 DCloud 依赖后完整回归微信登录、请求、上传、订单和问题流程。现阶段 CI 没有把 `npm audit` 设置为阻断项，以免所有业务修复都无法合并；该风险必须纳入后续依赖升级计划。
