# 小程序内容系统 V1 本地测试说明

## 安全边界

- 本次只生成迁移脚本，未连接或修改生产数据库。
- 未执行 Git commit、push 或生产部署。
- 百度地图 AK、微信 AppSecret、对象存储 AccessKey 均不得提交 Git。
- 小程序不包含 `web-view`、外链自动打开、支付、教务系统、评论或公开发布内容。

## 测试数据库迁移

先备份测试库，再执行：

```bash
mysql -h 127.0.0.1 -u root -p campus_business < sql/migrations/V2__mini_content_and_map.sql
```

迁移新增：`mini_user`、`media_asset`、`mini_content_category`、`mini_content_article`、
`mini_content_publish_record`、`mini_home_config`、`campus_place_category`、`campus_place`。

## 后端本地环境变量

至少配置：

```text
WECHAT_APP_ID=wx29a5927e8e34a951
WECHAT_APP_SECRET=微信小程序后台获取的 AppSecret
BAIDU_MAP_AK=本地 AK
OBJECT_STORAGE_ENDPOINT=https://你的 S3 兼容对象存储地址
OBJECT_STORAGE_BUCKET=私有 Bucket 名称
OBJECT_STORAGE_ACCESS_KEY=服务端 AccessKey
OBJECT_STORAGE_SECRET_KEY=服务端 SecretKey
```

对象存储 Bucket 必须保持私有。图片访问由 Java 后端签发十分钟临时地址。

## 启动

```bash
cd backend && ./mvnw spring-boot:run
cd admin-web && npm run dev
cd miniapp && npm run build:mp-weixin
```

微信开发者工具导入：`miniapp/dist/build/mp-weixin`。

## 第一轮测试顺序

1. 使用 Web 管理端创建栏目和图文，保存草稿后确认小程序不可见。
2. 发布图文，确认小程序首页与详情无需重新构建即可读取。
3. 添加 `copy_link` 内容块，确认只复制、不跳转。
4. 配置私有对象存储并上传素材，确认长期密钥不出现在浏览器请求中。
5. 创建地点，确认分类筛选、标记详情、复制地址和微信地图导航。
6. 小程序微信登录后提交带图问题，确认其他用户无法读取图片。
7. 老板在“小程序用户”中授予代理，用户重新登录后确认代理工作台出现。
8. 验证代理只能读取自己的订单，修改已确认订单恢复待确认，已导出校园网订单不可修改。

## 上线前配置

- 微信公众平台：request/uploadFile/downloadFile 合法域名配置为 `https://hutbxyw.click`。
- 后端：配置微信 AppSecret、私有对象存储参数与随机 JWT Secret。
- 对象存储：配置私有 Bucket、容量/生命周期策略和服务端最小权限账号。
- 数据库：在独立测试库完整回归后，维护窗口内备份并执行迁移。
- 百度地图控制台：为 AK 设置小程序 AppID、域名或服务端 IP 白名单，按实际调用方式限制权限。
