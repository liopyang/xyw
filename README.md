# 校园业务管理系统

当前目录：

- `admin-web/`：Vue 3 Web 管理端
- `backend/`：Spring Boot 后端
- `sql/schema.sql`：MySQL 8 表结构
- `sql/data.sql`：配置、测试账号与初始化数据
- `docs/`：项目文档目录
- `miniapp/`：uni-app 微信小程序端
- `docker-compose.yml`：本地 MySQL 8
- `docs/DEPLOYMENT.md`：当前服务器部署说明

开发环境启动说明分别见 `admin-web/README.md` 和 `backend/README.md`。

## 快速启动

```bash
docker compose up -d
cd backend && DB_PASSWORD=本地数据库密码 JWT_SECRET=至少32字节的本地随机密钥 ./mvnw spring-boot:run
cd admin-web && npm install && npm run dev
```

管理端对接真实后端时，将 `admin-web/.env` 中 `VITE_USE_MOCK` 改为 `false`。

小程序运行：

```bash
cd miniapp
npm install
npm run dev:mp-weixin
```

将生成的 `miniapp/dist/dev/mp-weixin` 导入微信开发者工具。

## Git 与 CI/CD

- 分支与 Pull Request 流程：`docs/GIT_WORKFLOW.md`
- GitHub Actions 和生产发布：`docs/CI_CD_DEPLOYMENT.md`
- GitHub Secrets：`docs/GITHUB_SECRETS.md`
- 自动与人工回滚：`docs/ROLLBACK.md`
- 一次性部署用户配置：`deploy/server/setup-deploy-user.md`

生产数据库配置、JWT 密钥、SSH 密钥和证书不得提交到 Git。
