# GitHub Environment Secrets

建议把以下值配置在 GitHub 仓库 Settings → Environments → `production`，不要放在代码、Actions YAML 或普通文档中。

| Secret | 填写内容 |
|---|---|
| `PROD_HOST` | 生产服务器域名或 IP，不包含协议 |
| `PROD_PORT` | SSH 端口，例如 `22` |
| `PROD_USER` | 独立部署用户，应为 `deploy`，不要使用 `root` |
| `PROD_SSH_PRIVATE_KEY` | 专用部署用户私钥的完整多行内容 |
| `PROD_KNOWN_HOSTS` | 已人工核对指纹的 SSH known_hosts 完整行 |

不要添加 root 密码、数据库密码、服务器 root 私钥或个人 SSH 私钥作为部署凭据。

## 生成部署密钥

在可信工作站执行：

```bash
ssh-keygen -t ed25519 -a 100 -f ./campus_github_deploy -C "github-actions-campus-production"
```

公钥安装到 `/home/deploy/.ssh/authorized_keys`，私钥仅存入 `PROD_SSH_PRIVATE_KEY`。密钥文件已被根 `.gitignore` 排除，但仍不要把它放进项目目录。

## 获取并验证 known_hosts

服务器控制台读取真实公钥指纹：

```bash
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

可信工作站获取 host key：

```bash
ssh-keyscan -H -p 22 服务器地址 > campus_known_hosts
ssh-keygen -lf campus_known_hosts
```

只有两个指纹一致时，才把 `campus_known_hosts` 的完整内容保存为 `PROD_KNOWN_HOSTS`。`ssh-keyscan` 的结果不能在未核对时直接信任。

工作流始终启用 SSH host key 校验，没有使用 `StrictHostKeyChecking=no`。

## Environment 审批

在 `production` 的 Deployment protection rules 中添加 Required reviewers。Secrets 和审批配置完成前，不要合并首次 `main` 发布 Pull Request。

如果当前套餐没有 Required reviewers，把生产工作流的 `push: main` 触发移除，只保留 `workflow_dispatch`，通过手工运行作为替代审批门。

GitHub Free 的私有仓库可能无法使用 Environment Secrets/Required reviewers。此时可把同名值保存为 Repository Secrets，但必须先把生产工作流改成仅 `workflow_dispatch`；否则 `main` 更新会在没有人工审批的情况下使用 Repository Secrets 部署。
