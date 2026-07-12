# 生产回滚

## 自动回滚条件

部署脚本在已经开始替换现网文件后，遇到以下任一错误会尝试恢复上一运行版本：

- 新 JAR 无法安装或 systemd 重启失败。
- `campus-business` 不是 active。
- 本机 `/api/actuator/health` 未在等待时间内返回 `UP`。
- 公网首页为空或不是管理端入口页面。
- 公网健康接口失败、返回 5xx 或状态不是 `UP`。
- 发布脚本中间命令失败。

自动回滚会恢复上一版 JAR 和前端 `index.html`/静态资源，重启服务并再次检查本机和公网。数据库不会回滚。

## 查看当前版本

```bash
cat /opt/campus-business/CURRENT_RELEASE
cat /opt/campus-business/releases/$(cat /opt/campus-business/CURRENT_RELEASE)/release.env
cat /opt/campus-business/releases/$(cat /opt/campus-business/CURRENT_RELEASE)/previous-release
```

第一次 CI/CD 发布前的现网版本会保存为 `legacy-<timestamp>`。

## 人工回滚到上一版

使用部署用户：

```bash
ssh deploy@服务器地址
sudo /opt/campus-business/scripts/rollback.sh
```

回滚到指定保留版本：

```bash
sudo /opt/campus-business/scripts/rollback.sh 完整的40位commit-sha
```

不要手工拼接 `cp`、`rm` 和 `systemctl` 命令绕过脚本。脚本会验证目录、JAR、健康状态并最后更新 `CURRENT_RELEASE`。

## 查看服务和日志

```bash
systemctl status campus-business --no-pager -l
journalctl -u campus-business -n 200 --no-pager
tail -n 100 /www/wwwlogs/hutbxyw.click.error.log
```

验证：

```bash
curl --fail http://127.0.0.1:18080/api/actuator/health
curl --fail https://hutbxyw.click/
curl --fail https://hutbxyw.click/api/actuator/health
```

旧版本如果早于 Actuator 健康接口，回滚脚本会改用受保护 API 的标准 401 响应确认后端已经启动。

## 保留策略

- 普通 Git 发布只自动清理到最近 5 个。
- 当前版本和上一版本不会自动删除。
- `legacy-*` 首次现网快照不会自动删除。
- 数据库备份、上传文件和无法识别的目录不会自动删除。

## 数据库说明

回滚只恢复应用代码和管理端静态文件，不执行 SQL，也不回滚 MySQL 数据。若某次未来发布包含数据库结构变更，必须在发布前准备独立数据库备份和兼容回退方案。
