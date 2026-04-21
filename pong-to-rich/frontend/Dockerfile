# 1단계 — 빌드
FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# VITE_API_BASE_URL은 빌드 시 ARG로 주입 (docker-compose build-arg 또는 CI 환경변수)
ARG VITE_API_BASE_URL=https://api.pongtrader.pro
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# 2단계 — Nginx로 정적 파일 서빙
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html

# React Router history 모드 — 모든 경로를 index.html로 fallback
COPY nginx-spa.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
