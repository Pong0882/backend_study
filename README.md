# Backend Dev Roadmap

체계적으로 다시 쌓는 백엔드 학습 레포

---

## 목표

- 개발 경험은 있지만 원리를 모르고 썼던 것들을 제대로 정리
- Spring Boot 기반 서비스를 처음부터 끝까지 — 코드부터 인프라, 운영까지

---

## 진행 방식

```
이론 정리 → 구현 실습 → 성능 비교 / 검증
```

- 너무 깊어진다 싶으면 넘어가고 나중에 다시 돌아옴
- 각 주제는 브랜치 단위로 관리
- 커밋 컨벤션은 [COMMIT_CONVENTION.md](./COMMIT_CONVENTION.md) 참고

---

## 로드맵

전체 학습 계획은 **[ROADMAP.md](./ROADMAP.md)** 에서 확인

| Phase | 주제 |
|-------|------|
| 0-A | 네트워크 & HTTP 기초 |
| 0-B | 환경 설정 & Gradle |
| 0-C | Git & 협업 워크플로우 |
| 0-D | Linux & Shell |
| 0 | Java & JVM / 자료구조 / 동시성 |
| 1 | Spring 핵심 이론 & 구조 |
| 2 | RESTful API & 문서화 |
| 3 | TDD |
| 4 | DB / JPA / 인덱스 / 트랜잭션 |
| 5 | AOP |
| 6 | 커스텀 어노테이션 |
| 7 | Spring Security / JWT / OAuth2 |
| 8 | 디자인 패턴 |
| 9 | 클린 코드 & 성능 최적화 |
| 10 | Docker & 컨테이너 오케스트레이션 |
| 11 | AI 연동 |
| 12 | NGINX & SSL |
| 13 | MSA & Spring Cloud & Saga |
| 14 | Kafka & Elasticsearch |
| 15 | 모니터링 & CI/CD |
| 16 | AWS 배포 & 인프라 |
| 17 | 성능 테스트 |
| 18 | 장애 대응 & 트러블슈팅 |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.x |
| DB | MySQL, Redis |
| Build | Gradle |
| Container | Docker, Docker Compose, k3s |
| Infra | AWS (EC2, RDS, ECS, S3) |
| Monitoring | Prometheus, Grafana, ELK |
| CI/CD | GitHub Actions, Jenkins |
