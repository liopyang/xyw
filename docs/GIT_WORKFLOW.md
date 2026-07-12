# Git 工作流程

## 分支用途

- `main`：生产稳定版本，只通过 Pull Request 合并进入。
- `dev`：日常集成分支，只测试和构建，不部署生产。
- `feature/*`：一项新功能一个临时分支。
- `fix/*`：一个问题修复一个临时分支。

分支代表一项独立修改，不代表固定员工。不使用复杂 Git Flow。

## 首次建立 GitHub 仓库

本地仓库已经初始化为 `main`，但尚无提交和远程地址。检查准备提交的内容：

```bash
git status --short
git check-ignore -v admin-web/node_modules admin-web/dist backend/target work
git add .
git diff --cached --check
git diff --cached
git commit -m "chore: initialize campus business system"
```

在 GitHub 创建空仓库后再执行，替换实际地址：

```bash
git remote add origin https://github.com/liopyang/xyw.git
git push -u origin main
git checkout -b dev
git push -u origin dev
```

当前任务不会代替你创建远程仓库、提交或推送。

## 日常开发

```bash
git checkout dev
git pull --ff-only origin dev
git checkout -b feature/example

# 修改并本地验证
git add .
git diff --cached
git commit -m "feat: 描述功能"
git push -u origin feature/example
```

修复问题时使用：

```bash
git checkout dev
git pull --ff-only origin dev
git checkout -b fix/example
git add .
git commit -m "fix: 描述问题"
git push -u origin fix/example
```

随后在 GitHub 创建 Pull Request：

1. `feature/*` 或 `fix/*` 合并到 `dev`。
2. 等待 CI 的后端、管理端、小程序三个检查全部通过。
3. 在 `dev` 完成测试后，再创建 `dev` 合并到 `main` 的 Pull Request。
4. `main` 更新后生产工作流重新测试和构建，并在部署步骤等待 `production` 环境批准。

不要直接向 `main` 强制推送，不使用 `git push --force`。

## GitHub 分支保护建议

在仓库 Settings → Rules → Rulesets 中为 `main` 设置：

- Require a pull request before merging。
- Require status checks to pass。
- 必选检查：`Backend verify`、`Admin web verify`、`WeChat miniapp verify`。
- Block force pushes 和 branch deletion。

`dev` 至少启用必需状态检查并阻止 force push。单人仓库可以不要求第二位审批者，但生产部署仍应使用 Environment 人工批准。

## 查看 Actions 和批准部署

- Pull Request 页面的 Checks 或仓库 Actions 页面可查看每个失败步骤。
- `main` 更新后打开 Actions → Build and deploy production。
- Build 完成后，Deploy after production approval 会等待 `production` Environment 的 Required reviewer 批准。
- 审核 commit SHA 和变更内容后再点 Approve and deploy。

如果当前 GitHub 套餐不支持 Environment Required reviewers，必须先把生产工作流触发方式改成仅 `workflow_dispatch`，由 Actions 页面手动启动；不要让生产部署无审批自动运行。
