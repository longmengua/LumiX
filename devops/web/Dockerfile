# 前端先以 lockfile 建置，避免 image build 因浮動 npm dependency 產生不同結果。
FROM node:22.14-alpine AS build
WORKDIR /workspace/web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web ./
RUN npm run build

# Nginx 只提供靜態檔與同源 API reverse proxy；認證與授權仍由 backend 負責。
FROM nginx:1.27-alpine
COPY devops/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/web/dist /usr/share/nginx/html
EXPOSE 8080
