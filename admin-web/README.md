# 校园业务管理系统 Web 管理端

基于 Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios 与 ECharts。

## 本地启动

```bash
cp .env.example .env
npm install
npm run dev
```

本地开发默认通过 Vite 代理连接真实服务器，避免浏览器跨域：

```text
VITE_API_BASE_URL=/api
VITE_USE_MOCK=false
VITE_DEV_PROXY_TARGET=https://hutbxyw.click
```

## 已实现

- 登录、JWT 会话保存与 401 自动退出
- 老板/管理员菜单和路由权限
- 固定侧栏、顶部账号区和管理端布局
- 首页业务卡片、ECharts 趋势、代理排名、待办跳转
- 订单筛选、分页、脱敏、确认、作废、恢复
- 校园网订单筛选与 Excel 下载流程
- API 统一响应与错误提示

页面只调用需求文档定义的真实后端接口，不包含前端假数据。

## 构建

```bash
npm run build
```
