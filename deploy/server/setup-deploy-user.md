# 一次性配置生产部署用户

本文命令需要由服务器管理员通过已验证的 root 会话人工执行。执行前不要修改 SSH 登录方式，也不要关闭现有 root 登录。

## 1. 创建独立部署用户和目录

```bash
id deploy >/dev/null 2>&1 || useradd --create-home --shell /bin/bash deploy

install -d -m 0755 -o root -g root /opt/campus-business/scripts
install -d -m 0750 -o root -g root /opt/campus-business/releases
install -d -m 0750 -o deploy -g deploy /opt/campus-business/incoming
install -d -m 0700 -o deploy -g deploy /home/deploy/.ssh
```

`deploy` 不加入 `wheel`，也不授予普通 root shell。

## 2. 在可信工作站生成专用密钥

不要复用个人密钥或 root 密钥：

```bash
ssh-keygen -t ed25519 -a 100 -f ./campus_github_deploy -C "github-actions-campus-production"
```

- 私钥 `campus_github_deploy` 只写入 GitHub Environment Secret `PROD_SSH_PRIVATE_KEY`。
- 公钥 `campus_github_deploy.pub` 安装到服务器的 `/home/deploy/.ssh/authorized_keys`。
- 私钥不要复制到生产服务器，也不要提交 Git。

在服务器安装公钥后修正权限：

```bash
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 0600 /home/deploy/.ssh/authorized_keys
```

## 3. 安装经过审查的脚本

先将仓库中的 `deploy/server/deploy.sh` 和 `deploy/server/rollback.sh` 上传到服务器临时目录，然后执行：

```bash
install -o root -g root -m 0755 /tmp/deploy.sh /opt/campus-business/scripts/deploy.sh
install -o root -g root -m 0755 /tmp/rollback.sh /opt/campus-business/scripts/rollback.sh
```

检查脚本和服务器依赖：

```bash
bash -n /opt/campus-business/scripts/deploy.sh
bash -n /opt/campus-business/scripts/rollback.sh
command -v bash curl flock sha256sum tar jar systemctl
```

## 4. 配置最小 sudo 权限

使用 `visudo -f /etc/sudoers.d/campus-deploy` 写入：

```sudoers
Defaults:deploy !requiretty
deploy ALL=(root) NOPASSWD: /opt/campus-business/scripts/deploy.sh *, /opt/campus-business/scripts/rollback.sh *
```

脚本由 root 拥有且部署用户不可写，脚本内部还会校验 commit SHA、压缩包路径、文件大小和发布目录。

校验：

```bash
chmod 0440 /etc/sudoers.d/campus-deploy
visudo -cf /etc/sudoers.d/campus-deploy
sudo -l -U deploy
```

不要给 `deploy` 添加 `NOPASSWD: ALL`，也不要授予任意 `systemctl`、`cp`、`mv` 或 shell 权限。

## 5. 验证密钥登录

从可信工作站测试：

```bash
ssh -i ./campus_github_deploy -p 22 deploy@服务器地址 'id && test -w /opt/campus-business/incoming'
```

只有新密钥登录验证成功后，才考虑限制 root 密码登录和 22 端口来源。SSH 加固是独立操作，不属于首次 CI/CD 安装步骤。

## 6. 获取并核对 SSH Host Key

服务器控制台查看真实指纹：

```bash
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

可信工作站获取公钥：

```bash
ssh-keyscan -H -p 22 服务器地址 > campus_known_hosts
ssh-keygen -lf campus_known_hosts
```

两处指纹必须一致。确认后将 `campus_known_hosts` 的完整内容写入 `PROD_KNOWN_HOSTS`。不要在 Actions 中使用 `StrictHostKeyChecking=no`。

## 7. 暂不执行的操作

- 不安装 self-hosted runner。
- 不在服务器执行 Maven、npm 或 Vite 构建。
- 不修改 Nginx root、TLS、HSTS、证书和续签机制。
- 不重建 MySQL 容器，不修改 `campus_mysql_data`。
- 不删除宝塔或 Docker。
