# GitHub Environment Secrets

以下值配置在 GitHub 仓库 Settings → Secrets and variables → Actions → Repository secrets，不要放在代码、Actions YAML 或普通文档中。`main` 分支每次 push 都会自动构建、测试并部署，无需人工审批。

| Secret                 | 填写内容                                     |
| ---------------------- | -------------------------------------------- |
| `PROD_HOST`            | 生产服务器域名或 IP，不包含协议              |
| `PROD_PORT`            | SSH 端口，例如 `22`                          |
| `PROD_USER`            | 独立部署用户，应为 `deploy`，不要使用 `root` |
| `PROD_SSH_PRIVATE_KEY` | 专用部署用户私钥的完整多行内容               |
| `PROD_KNOWN_HOSTS`     | 已人工核对指纹的 SSH known_hosts 完整行      |

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

## 自动部署

生产工作流监听 `main` 分支 push，并直接读取上述 Repository secrets 完成部署。部署前仍会完整构建和测试，部署后执行公网健康检查；检查失败会自动回滚到上一版本并让工作流失败。
