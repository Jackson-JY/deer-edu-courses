# 雅思在线课程网站

微信生态 H5 + PC Web（以 PC 为主）的雅思课程平台。支持微信登录、微信支付、加密视频观看、课件在线预览、学习记录，以及视频防下载/动态水印/单端在线等内容安全能力。

## 文档

- [需求与方案梳理](docs/需求与方案梳理.md) — 需求点、安全方案、技术架构、数据模型
- [页面级 PRD](docs/页面级PRD.md) — C 端 / 讲师后台 / 管理后台页面详述
- [OpenAPI 接口定义](docs/openapi.yaml) — 字段级接口契约

## 技术栈

| 层 | 选型 |
|---|---|
| 前端 | Vue3 + TypeScript + Vite + Pinia + Vue Router |
| 后端 | Java 21 + Spring Boot 3 + Spring Security + JPA + Flyway |
| 存储 | MySQL 8 + Redis 7 |
| 视频 | 腾讯云点播 VOD（私有加密 + 防盗链 + TCPlayer） |
| 课件 | 腾讯云 COS（私有读 + 短时效签名预览） |
| 组织 | pnpm monorepo |

## 目录结构

```
courses/
├── apps/
│   └── web/                # Vue3 PC Web（H5 后续在 apps/h5）
├── packages/
│   └── shared-types/       # 前后端共享 TS 类型（对应 openapi.yaml）
├── server/                 # Spring Boot 后端
│   ├── src/main/java/com/ielts/courses/
│   │   ├── common/         # ApiResult / BizCode / 全局异常
│   │   ├── config/         # SecurityConfig 等
│   │   ├── auth catalog order play user teacher admin/
│   │   └── CoursesApplication.java
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/   # Flyway DDL
├── docs/                   # 设计文档
├── docker-compose.yml      # 本地 MySQL + Redis
└── pnpm-workspace.yaml
```

## 本地开发

### 1. 前端

```bash
pnpm install
pnpm dev:web          # http://localhost:5173 （/api 代理到 :8080）
pnpm build:web        # 生产构建
```

> 后端未启动时，首页会回退到占位数据，前端可独立运行。

### 2. 中间件

```bash
docker compose up -d  # 起 MySQL + Redis
cp .env.example .env  # 填写微信/腾讯云密钥
```

### 3. 后端（需 JDK 21 + Maven）

```bash
cd server
mvn spring-boot:run   # http://localhost:8080，首次启动 Flyway 自动建表
```

## 安全要点（落地见 docs）

- 视频：腾讯云 VOD 私有加密 HLS + 短时效播放凭证(psign) + 防盗链；无下载入口。
- 水印：播放器动态跑马灯（手机尾号+UID+时间），DOM 被篡改即暂停；课件预览同样加水印。
- 账号：强制绑定手机号；单端在线（Redis 记录活跃 session，新登录踢旧端）。
- 支付：以微信异步回调为权限发放权威；订单幂等 + 主动查单兜底；金额服务端计算。

## 状态

脚手架阶段：前端可构建运行；后端为可编译的结构骨架（Controller 返回占位数据，Service/Repository 待实现）。
