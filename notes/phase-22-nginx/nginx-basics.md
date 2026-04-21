# Nginx — 개념, 정적 서빙 원리, conf 설정 완전 정리

## Nginx가 뭔가

웹 서버 + 리버스 프록시 + 로드 밸런서를 하나로 처리하는 소프트웨어.

Apache와 달리 **이벤트 기반(event-driven) + 비동기(async)** 모델로 동작.
연결 하나당 스레드를 생성하지 않아서 동시 연결이 많아도 메모리를 거의 안 씀.

---

## 동작 원리 — Apache와 비교

### Apache (스레드/프로세스 기반)

```
연결 1 → Worker 스레드 1 (점유, 응답 끝날 때까지 대기)
연결 2 → Worker 스레드 2 (점유)
연결 3 → Worker 스레드 3 (점유)
연결 1000 → 스레드 부족 → 큐 대기 or 거절
```

요청 하나당 스레드 하나를 독점. 스레드는 스택 메모리(보통 1~8MB)를 점유.
동시 연결 1000개 → 스레드 1000개 → 메모리 수 GB 소비.
I/O 대기 중(DB 쿼리, 파일 읽기)에도 스레드가 블로킹되어 낭비.

### Nginx (이벤트 기반 비동기)

```
Master Process (1개) — 설정 읽기, Worker 관리
    └── Worker Process (CPU 코어 수만큼)
            └── 수천 개의 연결을 단일 스레드로 처리
```

**epoll (Linux 커널 I/O 이벤트 통지 메커니즘):**

```
Worker가 epoll에 소켓들을 등록
    ↓
epoll이 "이 소켓에 데이터 왔어요" 이벤트 통지
    ↓
Worker가 해당 소켓만 처리
    ↓
다시 epoll 대기 (다른 연결 처리 가능)
```

I/O 대기 중에 Worker는 다른 연결의 이벤트를 처리. 블로킹 없음.
Worker 하나가 수만 개의 동시 연결을 처리 가능.

**worker_processes, worker_connections 설정:**

```nginx
# nginx.conf
worker_processes auto;        # CPU 코어 수만큼 자동 설정

events {
    worker_connections 1024;  # Worker 하나당 최대 동시 연결 수
}
# 최대 동시 연결 = worker_processes × worker_connections
```

**Nginx가 Apache보다 빠른 이유 요약:**
- 스레드 생성/소멸 오버헤드 없음
- 컨텍스트 스위칭 최소화
- I/O 대기 중 다른 연결 처리 가능
- 메모리 사용량 압도적으로 적음

---

### Nginx가 할 수 있는 것

| 역할 | 설명 | 예시 |
|------|------|------|
| 정적 파일 서버 | HTML/CSS/JS/이미지를 직접 읽어서 응답 | React 빌드 결과물 서빙 |
| 리버스 프록시 | 클라이언트 요청을 백엔드 서버로 전달 | `api.pongtrader.pro` → Spring Boot |
| 로드 밸런서 | 여러 백엔드 서버로 요청 분산 | upstream 블록으로 설정 |
| SSL 종료 | HTTPS 복호화를 Nginx가 처리, 백엔드는 HTTP만 | Let's Encrypt + Certbot |
| 캐싱 | 백엔드 응답을 Nginx가 캐시 | `proxy_cache` 설정 |
| gzip 압축 | 응답 크기 줄여서 전송 속도 향상 | `gzip on` |
| 속도 제한 | IP별 요청 수 제한 (rate limiting) | `limit_req_zone` |
| 기본 인증 | HTTP Basic Auth | `auth_basic` |

---

## 정적 파일 서빙 원리

브라우저가 `pongtrader.pro/` 요청 → Nginx가 `root` 디렉토리에서 파일을 찾아 응답.
Spring Boot 같은 WAS를 전혀 거치지 않음. 파일 I/O만으로 처리.

```
브라우저 → Nginx → 디스크(HTML/JS/CSS) → 브라우저
```

**React SPA의 특수한 문제:**

React Router는 클라이언트 사이드 라우팅. `/portfolio` 같은 경로는 실제 파일이 없음.
브라우저에서 직접 URL 입력하면 Nginx가 `/portfolio` 파일을 찾다가 404 반환.

해결: `try_files`로 파일 없으면 `index.html`로 fallback → React Router가 클라이언트에서 라우팅 처리.

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

`try_files $uri $uri/ /index.html` 동작 순서:
1. `$uri` — 요청 경로 그대로 파일이 있는지 확인 (예: `/assets/main.js`)
2. `$uri/` — 디렉토리인지 확인
3. `/index.html` — 둘 다 없으면 index.html 반환

정적 파일(`/assets/xxx.js`)은 1번에서 찾아서 바로 반환. React 라우트(`/portfolio`)는 3번으로 fallback.

---

## conf 파일 구조

```nginx
# 전체 구조
http {
    server {           ← 가상 호스트 (도메인 단위)
        listen 80;
        server_name example.com;

        location / {   ← URL 경로 단위 처리 규칙
            ...
        }
    }
}
```

Docker nginx 이미지는 `/etc/nginx/conf.d/*.conf` 파일을 자동으로 로드.
직접 `nginx.conf`를 건드리지 않아도 `conf.d/`에 파일 넣으면 됨.

---

## pongtrader.conf 각 지시어 설명

```nginx
server {
    listen 80;
    server_name pongtrader.pro;
```

- `listen 80` — HTTP 80포트 수신
- `server_name` — 이 server 블록이 처리할 도메인. 요청의 `Host` 헤더와 매칭

```nginx
    location / {
        proxy_pass http://frontend:80;
```

- `location /` — `/` 이하 모든 경로 매칭
- `proxy_pass` — 요청을 `frontend` 컨테이너 80포트로 전달 (리버스 프록시)

```nginx
        proxy_set_header Host $host;
```

- 백엔드에 전달하는 요청의 `Host` 헤더를 원래 클라이언트가 보낸 호스트명으로 설정
- 없으면 `Host: frontend:80` 이 전달되어 백엔드가 잘못된 호스트로 인식할 수 있음

```nginx
        proxy_set_header X-Real-IP $remote_addr;
```

- 실제 클라이언트 IP를 백엔드에 전달
- 없으면 백엔드에서 모든 요청이 Nginx IP로 들어온 것처럼 보임

```nginx
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

- 프록시 체인을 거친 전체 IP 목록. 여러 프록시를 거칠 때 원래 IP 추적용
- `$proxy_add_x_forwarded_for` — 기존 `X-Forwarded-For` 값에 현재 클라이언트 IP 추가

```nginx
        proxy_set_header X-Forwarded-Proto $scheme;
```

- 원래 요청이 HTTP인지 HTTPS인지를 백엔드에 전달
- HTTPS 종료를 Nginx에서 하면 백엔드는 HTTP로 받아서 `$scheme`이 항상 `http`가 됨
- 이 헤더로 백엔드가 원래 프로토콜을 알 수 있음 (리다이렉트 URL 생성 시 중요)

---

---

## 리버스 프록시란

클라이언트는 Nginx에만 요청. Nginx가 내부 서버(Spring Boot 등)에 대신 요청하고 응답을 클라이언트에 돌려줌.

```
클라이언트 → Nginx(리버스 프록시) → Spring Boot
                                  → 다른 서버
```

**포워드 프록시와 차이:**

| 구분 | 위치 | 누구를 위해 |
|------|------|-----------|
| 포워드 프록시 | 클라이언트 앞 | 클라이언트를 대신해 외부에 요청 (VPN, 사내망) |
| 리버스 프록시 | 서버 앞 | 서버를 대신해 클라이언트 요청을 받음 (Nginx, API Gateway) |

**리버스 프록시를 쓰는 이유:**
- 백엔드 서버 IP/포트를 외부에 노출하지 않음 (보안)
- 도메인 기반 라우팅 (`pongtrader.pro` vs `api.pongtrader.pro`)
- SSL 종료를 한 곳에서 처리
- 로드 밸런싱, 캐싱, Rate Limiting 등 부가 기능

---

## 로드 밸런싱 알고리즘

백엔드 서버가 여러 대일 때 요청을 어떻게 분산할지 결정하는 전략.
Nginx에서는 `upstream` 블록으로 설정.

### Round Robin (기본값)

요청을 순서대로 돌아가며 분배. 가장 단순.

```nginx
upstream backend {
    server app1:8080;
    server app2:8080;
    server app3:8080;
}
```

요청 1 → app1, 요청 2 → app2, 요청 3 → app3, 요청 4 → app1 ...

**문제점:** 서버마다 처리 속도가 다를 때 느린 서버에도 계속 요청이 감.

### Least Connections

현재 처리 중인 요청이 가장 적은 서버로 보냄.
처리 시간이 들쭉날쭉한 API에 적합 (DB 쿼리, 외부 API 호출 등).

```nginx
upstream backend {
    least_conn;
    server app1:8080;
    server app2:8080;
}
```

### IP Hash

클라이언트 IP를 해싱해서 항상 같은 서버로 보냄. **세션 고정(Session Sticky)** 효과.
서버가 세션을 로컬에 저장할 때 (Redis 같은 공유 저장소 없을 때) 유용.

```nginx
upstream backend {
    ip_hash;
    server app1:8080;
    server app2:8080;
}
```

**문제점:** 특정 IP에서 트래픽이 폭발하면 한 서버에만 몰림. 서버 추가/제거 시 기존 매핑이 흐트러짐.

### 가중치 (weight)

서버마다 처리 비율 지정. 스펙이 다른 서버를 섞을 때 사용.

```nginx
upstream backend {
    server app1:8080 weight=3;  # 요청의 3/4
    server app2:8080 weight=1;  # 요청의 1/4
}
```

### 알고리즘 선택 기준

| 상황 | 추천 |
|------|------|
| 서버 스펙 동일, 처리 시간 균일 | Round Robin (기본값) |
| 처리 시간이 들쭉날쭉한 API | Least Connections |
| 세션을 서버 로컬에 저장 | IP Hash |
| 서버 스펙이 다름 | Weight |
| pong-to-rich (현재 단일 서버) | 해당 없음, 나중에 스케일 아웃 시 고려 |

---

## keepalive — 연결 재사용

HTTP 요청마다 TCP 연결을 새로 맺으면 3-way handshake 비용이 발생.
keepalive는 연결을 유지해서 재사용.

**클라이언트 ↔ Nginx keepalive** (기본 활성화):
```nginx
keepalive_timeout 65;   # 65초 동안 연결 유지
keepalive_requests 100; # 연결 하나로 최대 100 요청 처리
```

**Nginx ↔ 백엔드 keepalive** (upstream):
```nginx
upstream backend {
    server app:8080;
    keepalive 32;  # 백엔드와 최대 32개 연결 유지
}

server {
    location / {
        proxy_pass http://backend;
        proxy_http_version 1.1;          # keepalive는 HTTP/1.1 필요
        proxy_set_header Connection "";  # Connection: close 헤더 제거
    }
}
```

Nginx ↔ 백엔드 keepalive 없으면 요청마다 TCP 연결 생성 → Spring Boot 부하 증가.

---

## 버퍼 튜닝

Nginx가 백엔드 응답을 버퍼에 담아두고 클라이언트에 전송.
버퍼가 너무 작으면 디스크에 임시 저장(느림), 너무 크면 메모리 낭비.

```nginx
proxy_buffer_size   4k;    # 응답 헤더를 담는 버퍼
proxy_buffers       8 4k;  # 응답 바디를 담는 버퍼 (8개 × 4KB = 32KB)
proxy_busy_buffers_size 8k;
```

**언제 튜닝하나:**
- 백엔드 응답이 크고 느린 클라이언트가 많을 때 → 버퍼 크게
- 메모리가 부족할 때 → 버퍼 작게 + 디스크 캐시 활용
- 일반적인 API 서버는 기본값으로도 충분

---

## upstream 헬스체크

백엔드 서버가 죽었을 때 자동으로 해당 서버를 제외하는 기능.

**passive 헬스체크 (기본, 무료):**

실제 요청이 실패했을 때 감지. 별도 설정 없이 동작.

```nginx
upstream backend {
    server app1:8080 max_fails=3 fail_timeout=30s;
    # 30초 안에 3번 실패하면 30초 동안 해당 서버 제외
    server app2:8080 max_fails=3 fail_timeout=30s;
}
```

**active 헬스체크 (Nginx Plus 유료 기능):**

주기적으로 헬스체크 엔드포인트를 직접 호출해서 확인.
오픈소스 Nginx에서는 `nginx_upstream_check_module` 써드파티 모듈로 구현 가능.

---

## 로그 포맷 커스터마이징

기본 로그보다 더 많은 정보를 남길 수 있음.

```nginx
http {
    log_format main '$remote_addr - $remote_user [$time_local] '
                    '"$request" $status $body_bytes_sent '
                    '"$http_referer" "$http_user_agent" '
                    '$request_time $upstream_response_time';
                    # request_time: 전체 요청 처리 시간
                    # upstream_response_time: 백엔드 응답 시간 (병목 분석용)

    access_log /var/log/nginx/access.log main;
    error_log  /var/log/nginx/error.log warn;
}
```

`$upstream_response_time` — Nginx가 백엔드에 요청 보내고 응답 받기까지 시간.
`$request_time`에서 `$upstream_response_time`을 빼면 Nginx 자체 처리 시간.
응답이 느릴 때 병목이 Nginx인지 백엔드인지 구분 가능.

---

## 앞으로 추가할 수 있는 것들

### 1. HTTPS (SSL 인증서)

Cloudflare Flexible을 쓰면 Cloudflare ↔ 브라우저는 HTTPS지만, Cloudflare ↔ Nginx는 HTTP.
진짜 End-to-End HTTPS를 하려면 Let's Encrypt로 인증서 발급.

```nginx
server {
    listen 443 ssl;
    server_name pongtrader.pro;

    ssl_certificate     /etc/letsencrypt/live/pongtrader.pro/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pongtrader.pro/privkey.pem;
    ...
}

# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name pongtrader.pro;
    return 301 https://$host$request_uri;
}
```

### 2. gzip 압축

JS/CSS 응답을 압축해서 전송 크기 줄임. React 빌드 결과물(보통 수백 KB)에 효과적.

```nginx
gzip on;
gzip_types text/plain text/css application/javascript application/json;
gzip_min_length 1024;  # 1KB 이상만 압축
```

### 3. 정적 파일 캐시 헤더

브라우저가 JS/CSS를 캐시하도록 `Cache-Control` 헤더 설정.
Vite 빌드는 파일명에 해시(`main-DJ4CWwcE.js`)가 붙어 캐시 무효화가 자동으로 됨.

```nginx
location /assets/ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### 4. 로드 밸런싱

백엔드 서버가 여러 대일 때 요청 분산.

```nginx
upstream backend {
    server app1:8080;
    server app2:8080;
    server app3:8080;
}

server {
    location / {
        proxy_pass http://backend;
    }
}
```

### 5. Rate Limiting (요청 수 제한)

IP별로 초당 요청 수 제한. DDoS, brute force 방어.

```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

server {
    location /api/ {
        limit_req zone=api burst=20 nodelay;
    }
}
```

### 6. 프록시 캐싱

백엔드 응답을 Nginx가 캐시. DB 부하 감소.

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m;

location /api/stocks/ {
    proxy_cache api_cache;
    proxy_cache_valid 200 1m;  # 200 응답은 1분 캐시
}
```

---

## pong-to-rich에서 사용된 곳

| 파일 | 역할 |
|------|------|
| `pong-to-rich/nginx/pongtrader.conf` | `pongtrader.pro` → frontend 컨테이너, `api.pongtrader.pro` → app 컨테이너 리버스 프록시 |
| `pong-to-rich/nginx/docker-compose.yml` | Nginx 컨테이너 실행, conf 파일 볼륨 마운트 |
| `pong-to-rich/frontend/nginx-spa.conf` | React SPA history fallback (`try_files`) |
| `pong-to-rich/frontend/Dockerfile` | 2단계 빌드 — Node로 빌드 후 Nginx Alpine으로 정적 서빙 |
