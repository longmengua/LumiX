# 管理端必須有自己的 build target，不能重用前台 bundle 後再靠路徑猜測掛載哪個 App。
FROM node:22.14-alpine AS build
WORKDIR /workspace/web
ARG LUMIX_WEB_SURFACE=admin
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web ./
RUN VITE_LUMIX_SURFACE=$LUMIX_WEB_SURFACE npm run build

# 管理端只提供 `/admin/*` 與同源 admin API proxy，避免入口層重新合併。
FROM nginx:1.27-alpine
COPY devops/admin-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/web/dist /usr/share/nginx/html
EXPOSE 8080
