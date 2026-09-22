# ---- 构建阶段 ----
FROM node:22-alpine AS builder
WORKDIR /build

# 安装 pnpm
RUN corepack enable && corepack prepare pnpm@11.8.0 --activate

# 复制 workspace 配置和 lockfile（利用缓存）
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./

# 复制各包的 package.json
COPY packages/shared-types/package.json ./packages/shared-types/
COPY apps/web/package.json ./apps/web/

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制源码
COPY packages/shared-types ./packages/shared-types
COPY apps/web ./apps/web

# 构建前端
RUN pnpm --filter @ielts/web build

# ---- 运行阶段 ----
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /build/apps/web/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf

# 绕过 nginx 镜像的 entrypoint 初始化脚本（新版镜像该脚本可能挂起导致 nginx 不启动），
# 我们的配置为纯静态 + 反代，无需 ipv6/envsubst 等初始化逻辑
ENTRYPOINT []
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
