# 服务器部署说明

当前生产验收部署：

- 管理端：`https://hutbxyw.click/`
- API：`https://hutbxyw.click/api/`
- 应用目录：`/opt/campus-business`
- 管理端目录：`/www/wwwroot/hutbxyw.click`
- systemd 服务：`campus-business`
- MySQL 8.4 容器：`campus-mysql`

后端仅监听 `127.0.0.1:18080`，MySQL 8.4 仅监听 `127.0.0.1:3307`，外部请求统一由 Nginx 代理。HTTP 自动跳转 HTTPS，`www.hutbxyw.click` 自动跳转到主域名，HTTPS 响应启用了 HSTS。旧 Java、Node、PHP-FPM、phpMyAdmin 888 监听和 MySQL 5.7 服务已经停止并禁用；其运行文件保留不会影响当前项目，需要释放磁盘时可另行删除。

运行方式：

- Nginx：静态管理端、TLS/HSTS、反向代理和登录限流
- Spring Boot 3 / Java 17：systemd 托管，失败自动重启
- MySQL 8.4：Docker `unless-stopped`，数据卷为 `campus_mysql_data`

常用检查命令：

```bash
systemctl status campus-business
journalctl -u campus-business -f
docker ps --filter name=campus-mysql
nginx -t
ss -lntp
```

发布新后端后需等待约 8 秒完成启动。管理端静态文件建议使用新目录解压后原子切换，并把上一版 `assets` 中的哈希资源合并保留一段时间，避免仍打开旧页面的浏览器请求到 404。

安全要求：

- 首次验收完成后立即修改系统初始化账号密码。
- 服务器 root 密码不得保存在代码或部署文件中，建议改用 SSH 密钥并关闭 root 密码登录。
- 定期备份 Docker 数据卷、`/opt/campus-business/uploads` 和 Nginx 证书。
