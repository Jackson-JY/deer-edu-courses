# 雅思课程平台：手机号登录 + 管理后台 + 视频安全 实施计划

## Context

当前项目已完成 C 端前端页面骨架与后端 Spring Boot 骨架，但微信登录尚未打通，后端 Controller 多为占位实现，缺少真实鉴权、课程管理、视频播放安全等能力。本计划用于把项目从“可演示骨架”推进到“可给指定用户开通权限并安全播放课程”的最小可用状态。

## Goals

1. 替换微信登录为**手机号 + 短信验证码登录**，确保能识别用户，并保留后续接微信的扩展空间。
2. 新增**管理后台**（`/admin`），管理员可上传/配置课程视频、课件，并给指定用户开通课程权限。
3. 视频播放接入**防盗链、动态水印、防下载、单端在线**等安全能力。

## Assumptions

- 登录方式改为**手机号 + 短信验证码**（开发环境 mock 短信，生产对接短信 SDK）。保留 `user.unionid/openid_*` 字段，未来可再绑定微信。
- 管理后台做在现有 Vue3 工程内，路由 `/admin/*`，管理员通过 `staff.role = 'ADMIN'` 识别。
- 视频安全按既有 PRD 设计：腾讯云 VOD 私有加密 + TCPlayer + psign 短时效签名 + 前端动态水印。若暂未开通云服务，后端接口保持契约不变，前端可降级到测试视频。
- 课件上传先以腾讯云 COS 直签为目标，未开通时可用本地/服务器临时目录 fallback。

## Implementation Plan

### Phase 1：登录与鉴权基础设施

| # | 任务 | 关键文件 |
|---|---|---|
| 1.1 | 后端新增 JWT 过滤器，解析 `Authorization: Bearer <jwt>`，校验签名与 Redis 单端 session | `server/src/main/java/com/ielts/courses/config/JwtAuthFilter.java` |
| 1.2 | 改造 `SecurityConfig`：放行公开接口，`/api/admin/**` 限 ADMIN，其余需登录 | `server/src/main/java/com/ielts/courses/config/SecurityConfig.java` |
| 1.3 | 新增 `User`/`Staff` JPA Entity 与 Repository | `server/src/main/java/com/ielts/courses/user/*.java` |
| 1.4 | 实现手机号验证码登录接口：`/api/auth/phone/send-code`、`/api/auth/phone/login`、`/api/auth/logout` | `server/src/main/java/com/ielts/courses/auth/*.java` |
| 1.5 | 前端改造登录弹窗为纯手机号验证码流程，接入真实 API | `apps/web/src/components/LoginDialog.vue` |
| 1.6 | 前端 auth store 持久化、恢复 profile、全局处理 `SESSION_KICKED` | `apps/web/src/stores/auth.ts`、`apps/web/src/api/client.ts` |
| 1.7 | Flyway V2：插入初始管理员手机号与 staff 记录 | `server/src/main/resources/db/migration/V2__init_admin.sql` |

### Phase 2：管理后台骨架与课程管理

| # | 任务 | 关键文件 |
|---|---|---|
| 2.1 | 前端新增 `/admin/*` 路由、`AdminLayout`、管理员登录页 | `apps/web/src/router/index.ts`、`apps/web/src/views/admin/AdminLayout.vue`、`apps/web/src/views/admin/LoginView.vue` |
| 2.2 | 后端课程 CRUD API，支持状态 DRAFT/REVIEWING/ONLINE/OFFLINE | `server/src/main/java/com/ielts/courses/admin/AdminCourseController.java`、`server/src/main/java/com/ielts/courses/catalog/*.java` |
| 2.3 | 前端课程管理页：列表、新建/编辑、状态控制 | `apps/web/src/views/admin/CourseManageView.vue`、`apps/web/src/views/admin/CourseEditView.vue` |
| 2.4 | 后端章节/课时 CRUD API，字段包括 `vod_file_id`、`free_trial`、`duration` | `server/src/main/java/com/ielts/courses/catalog/*.java` |
| 2.5 | 前端课时编排页，支持排序、试看标记 | `apps/web/src/views/admin/CourseEditView.vue` |
| 2.6 | 后端 VOD 上传签名接口 `/api/admin/vod/upload-sign` | `server/src/main/java/com/ielts/courses/admin/AdminVodController.java` |
| 2.7 | 前端课时视频上传：直传 VOD，回写 `fileId` | `apps/web/src/views/admin/LessonUploadView.vue` |

### Phase 3：用户权限与视频播放安全

| # | 任务 | 关键文件 |
|---|---|---|
| 3.1 | 后端 `EntitlementService`：判定已购/订阅/试看权限 | `server/src/main/java/com/ielts/courses/play/EntitlementService.java` |
| 3.2 | 后端管理员开通权限 API：`/api/admin/users/{id}/entitlements` | `server/src/main/java/com/ielts/courses/admin/AdminUserController.java` |
| 3.3 | 前端用户管理与开通权限页 | `apps/web/src/views/admin/UserManageView.vue`、`apps/web/src/views/admin/UserEntitlementView.vue` |
| 3.4 | 后端 `PlayService.issueCredential`：校验权限、单端在线、生成 psign | `server/src/main/java/com/ielts/courses/play/PlayService.java` |
| 3.5 | 后端 `VodPsignGenerator`：按腾讯云算法生成短时效签名 | `server/src/main/java/com/ielts/courses/play/VodPsignGenerator.java` |
| 3.6 | 前端 `PlayerView` 接入 TCPlayer，fallback 测试视频 | `apps/web/src/views/PlayerView.vue` |
| 3.7 | 前端动态水印、禁用右键/下载、水印 DOM 防篡改监听 | `apps/web/src/views/PlayerView.vue` |
| 3.8 | 播放心跳 `/api/play/heartbeat` 实现：合并学习记录 + 单端校验 | `server/src/main/java/com/ielts/courses/play/PlayController.java` |

### Phase 4：课件上传与收尾

| # | 任务 | 关键文件 |
|---|---|---|
| 4.1 | 后端课件上传签名/回调，类型 PDF/IMAGE/AUDIO，全部 `preview_only=1` | `server/src/main/java/com/ielts/courses/admin/AdminMaterialController.java` |
| 4.2 | 前端课件管理页：上传、列表、绑定课程/课时 | `apps/web/src/views/admin/MaterialManageView.vue` |
| 4.3 | 课件预览短时效签名接口 `/api/materials/{id}/preview-url` | `server/src/main/java/com/ielts/courses/play/PlayController.java` 或新建 `MaterialController.java` |
| 4.4 | 可选：新增 `system_config` 表与后台水印策略页 | `server/src/main/resources/db/migration/V3__system_config.sql`、`apps/web/src/views/admin/SecurityView.vue` |

## Key Files to Modify

### 前端
- `apps/web/src/components/LoginDialog.vue`
- `apps/web/src/stores/auth.ts`
- `apps/web/src/api/client.ts`
- `apps/web/src/router/index.ts`
- `apps/web/src/views/PlayerView.vue`
- `apps/web/src/App.vue`
- `apps/web/src/api/auth.ts`（新增）
- `apps/web/src/api/admin.ts`（新增）
- `apps/web/src/views/admin/*.vue`（新增）
- `packages/shared-types/src/index.ts`（新增类型）

### 后端
- `server/src/main/java/com/ielts/courses/config/SecurityConfig.java`
- `server/src/main/java/com/ielts/courses/config/JwtAuthFilter.java`（新增）
- `server/src/main/java/com/ielts/courses/auth/*.java`（新增）
- `server/src/main/java/com/ielts/courses/user/*.java`（新增）
- `server/src/main/java/com/ielts/courses/catalog/*.java`（新增 Entity/Service/Repository）
- `server/src/main/java/com/ielts/courses/play/PlayService.java`（新增）
- `server/src/main/java/com/ielts/courses/play/VodPsignGenerator.java`（新增）
- `server/src/main/java/com/ielts/courses/admin/*.java`（新增）
- `server/src/main/resources/db/migration/V2__init_admin.sql`（新增）
- `server/src/main/resources/application.yml`

## Verification

### 手动验证
1. 非管理员手机号登录后访问 `/admin` 被拦截；管理员可进入后台。
2. 管理员给用户 A 开通课程 X 后，用户 A 在 C 端课程详情页看到「开始学习」，进入播放器可获取 psign。
3. 用户 B 未开通课程 X，调用 `/api/play/credential` 返回 `NO_ENTITLEMENT`。
4. 同一账号在两浏览器登录，先登录端触发 `SESSION_KICKED` 弹窗。
5. 播放器页面右键被禁用，无下载按钮；删除水印 DOM 后视频暂停。

### 回归验证
- 前端 `pnpm build:web` 通过。
- 后端 `mvn clean package` 通过。
- Flyway 迁移在干净数据库上可成功执行。
