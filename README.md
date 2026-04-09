# Backend A to Z 🚀

> 원리를 모르고 썼던 것들을 제대로 다시 쌓는 백엔드 학습 레포

---

## 시작하게 된 이유

개발 경험은 있었다. 근데 돌아보니 왜 되는지 몰랐다.

어노테이션을 붙이면 동작했고, 에러가 나면 구글링으로 해결했다.
원리를 아는 척했지만 사실 겉핥기였다.

그래서 처음부터 다시 쌓기로 했다.
네트워크부터 시작해서, Spring 내부 동작, JVM, 인프라, AI 연동까지.
이론만 정리하는 게 아니라 실제 서비스를 만들면서 전부 체감하는 것이 목표다.

---

## 메인 프로젝트 — pong-to-rich

모든 Phase의 학습 내용은 하나의 실제 서비스에 순차적으로 적용된다.

**[🏓 pong-to-rich](./pong-to-rich/README.md)** — AI 기반 주식 자동매매 플랫폼

- 한국투자증권 KIS API 연동 (모의투자 → 실전매매)
- AI 모델 선택 + 자연어 전략 입력 → 자동매매 실행
- 뉴스 기반 AI 주가 예측 + 머신러닝 모델
- 종목별 실시간 채팅 + 주주 뱃지 + 커뮤니티
- 아키텍처 진화: 모놀리식 → 멀티모듈 → MSA → k8s

> 처음부터 최적화하지 않는다.
> 느려지는 지점을 직접 체감하고 단계적으로 개선한다.

---

## 진행 방식

```
이론 정리 → 구현 실습 → 성능 비교 → devlog 기록
```

- 이론만 정리하고 끝내지 않는다. 반드시 pong-to-rich에 직접 적용한다
- 에러가 나면 로그를 보고 스스로 원인을 분석한 뒤 해결 과정을 기록한다
- 커밋도, 터미널 명령어도 전부 직접 실행한다
- 학습한 내용은 velog에 내 언어로 다시 풀어서 정리한다

> AI를 적극적으로 활용하되, 코드와 개념에 대한 이해는 온전히 내 것으로 만드는 것을 원칙으로 한다

---

## 로드맵

전체 32 Phase 학습 계획 → **[ROADMAP.md](./ROADMAP.md)**

| Phase | 주제 |
|-------|------|
| 1 | 네트워크 & 웹 기초 |
| 2 | OS 기초 (프로세스 / 스레드 / 메모리) |
| 3 | Linux & Shell |
| 4 | 환경 설정 & Gradle |
| 5 | Git & 협업 워크플로우 |
| 6 | Java & JVM / 자료구조 / 동시성 |
| 7 | Spring 핵심 이론 & 구조 |
| 8 | RESTful API & 문서화 |
| 9 | 예외 처리 & 로깅 전략 |
| 10 | TDD |
| 11 | 데이터베이스 & JPA |
| 12 | AOP |
| 13 | 커스텀 어노테이션 & 멀티모듈 |
| 14 | Spring Security / JWT / OAuth2 |
| 15 | 알림 & 인증 서비스 |
| 16 | Redis 심화 |
| 17 | 디자인 패턴 |
| 18 | 클린 코드 & 성능 최적화 |
| 19 | Docker |
| 20 | 컨테이너 오케스트레이션 (Swarm / k3s / k8s) |
| 21 | AI 연동 (Spring AI / RAG / Agent / MCP) |
| 22 | NGINX & SSL |
| 23 | MSA & Spring Cloud & Saga |
| 24 | Kafka & Elasticsearch |
| 25 | 모니터링 & Observability |
| 26 | CI/CD 파이프라인 |
| 27 | AWS 배포 & 인프라 |
| 28 | 성능 테스트 & 분석 |
| 29 | 장애 대응 & 트러블슈팅 |
| 30 | 결제 시스템 |
| 31 | 보안 심화 |
| 32 | 서비스 런칭 & 사업 운영 |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.x |
| DB | MySQL, Redis |
| Build | Gradle |
| Container | Docker, Docker Compose, k8s |
| Infra | AWS (EC2, RDS, ECS, EKS, S3) |
| AI | LLM API (GPT / Claude), RAG, 머신러닝 |
| Monitoring | Prometheus, Grafana, ELK |
| CI/CD | GitHub Actions, ArgoCD |
