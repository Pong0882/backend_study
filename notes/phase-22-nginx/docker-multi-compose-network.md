# Docker 다중 Compose 파일 + External Network

## 왜 필요한가

`docker-compose.yml` 하나에 모든 서비스를 넣으면 간단하지만, 서비스를 다른 서버로 이전하거나 독립적으로 배포할 때 분리가 어려움.

pong-to-rich는 백엔드/프론트/Nginx를 각각 별도 compose 파일로 관리:

```
pong-to-rich/
├── docker-compose.yml          ← 백엔드 (app, mysql, redis, promtail, redis-exporter)
├── frontend/
│   └── docker-compose.yml      ← 프론트엔드 (frontend)
└── nginx/
    └── docker-compose.yml      ← Nginx 리버스 프록시
```

프론트를 다른 서버로 이전할 때 `frontend/` 디렉토리만 통째로 이동하면 됨.

---

## 문제: 분리된 compose 파일끼리 통신이 안 됨

Docker Compose는 기본적으로 각 compose 파일마다 독립적인 네트워크를 생성함.

```
docker-compose.yml      → pong-to-rich_default 네트워크
frontend/compose.yml    → frontend_default 네트워크
nginx/compose.yml       → nginx_default 네트워크
```

서로 다른 네트워크에 있으면 컨테이너 이름으로 접근 불가.
Nginx가 `http://app:8080`, `http://frontend:80`으로 접근하려면 같은 네트워크여야 함.

---

## 해결: External Network

Docker 네트워크를 compose 파일 밖에서 미리 만들고, 각 compose 파일이 그걸 참조.

### 1. 네트워크 생성 (최초 1회)

```bash
docker network create pong-network
```

### 2. 각 compose 파일에서 external 참조

```yaml
# 어느 compose 파일이든 동일하게
networks:
  pong-network:
    external: true
```

`external: true` — "이 네트워크는 내가 만드는 게 아니라 이미 존재하는 것을 가져다 씀"

### 3. 서비스에 네트워크 연결

```yaml
services:
  app:
    networks:
      - pong-network
```

---

## 컨테이너 간 통신: 서비스 이름으로 접근

같은 네트워크에 있으면 컨테이너 이름(또는 서비스 이름)으로 접근 가능.
`localhost`는 컨테이너 자기 자신을 가리키기 때문에 다른 컨테이너에 접근할 수 없음.

```nginx
# 잘못된 방식 — localhost는 Nginx 컨테이너 자신을 가리킴
proxy_pass http://localhost:8080;

# 올바른 방식 — Docker 네트워크 내 서비스 이름으로 접근
proxy_pass http://app:8080;
proxy_pass http://frontend:80;
```

Docker 내부 DNS가 `app` → 해당 컨테이너 IP로 자동 해석.

---

## 실행 순서

```bash
# 1. 네트워크 먼저 (최초 1회)
docker network create pong-network

# 2. 백엔드
cd pong-to-rich
docker compose up -d

# 3. 프론트
cd pong-to-rich/frontend
docker compose up -d --build

# 4. Nginx
cd pong-to-rich/nginx
docker compose up -d
```

Nginx가 마지막인 이유 — `app`, `frontend` 컨테이너가 먼저 떠 있어야 upstream으로 연결 가능.

---

## docker network ls로 보면

```
pong-network                bridge    local  ← 우리가 만든 external 네트워크
pong-to-rich_pong-network   bridge    local  ← 이전 방식 잔재 (external 전환 전)
pong-to-rich_default        bridge    local  ← compose 자동 생성 기본 네트워크
```

`pong-to-rich_pong-network`는 compose 파일 내부에서 `driver: bridge`로 정의했을 때 Docker Compose가 프로젝트명을 prefix로 붙여 자동 생성한 것. external 전환 후 더 이상 사용 안 함.

---

## pong-to-rich에서 사용된 곳

| 파일 | 역할 |
|------|------|
| `pong-to-rich/docker-compose.yml` | 백엔드 서비스들 — `pong-network` external 참조 |
| `pong-to-rich/frontend/docker-compose.yml` | 프론트 서비스 — `pong-network` external 참조 |
| `pong-to-rich/nginx/docker-compose.yml` | Nginx — `pong-network` external 참조, `app`/`frontend`로 프록시 |
