# 构建阶段
FROM node:20-slim AS builder

WORKDIR /app

# 安装 pnpm
RUN npm install -g pnpm@10

# 先复制依赖配置，利用 Docker 缓存层
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
COPY packages ./packages

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制源码并构建生产包
COPY . .
RUN pnpm build

# 运行阶段：nginx 托管静态资源
FROM nginx:alpine

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
