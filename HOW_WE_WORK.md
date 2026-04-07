# 진행 방식 가이드

이 레포를 처음 보는 AI 또는 사람이 읽고 동일한 방식으로 진행할 수 있도록 작성된 문서

---

## 디렉토리 구조

```
spring_study/
├── HOW_WE_WORK.md          ← 지금 이 파일. 진행 방식 설명
├── README.md               ← 레포 소개 및 로드맵 요약
├── ROADMAP.md              ← 전체 학습 로드맵 (Phase 0-A ~ Phase 22)
├── COMMIT_CONVENTION.md    ← 커밋 메시지 규칙
├── .gitignore
│
├── devlog/                 ← 날짜별 학습 일지
│   └── day01-2026-04-07.md
│
├── notes/                  ← Phase별 이론 정리
│   ├── README.md
│   ├── phase-0A-network/
│   ├── phase-0B-env-gradle/
│   ├── phase-0C-git/
│   ├── phase-0D-linux/
│   ├── phase-0-java-jvm/
│   ├── phase-1-spring-core/
│   └── ... (ROADMAP.md의 Phase 순서와 동일)
│
├── hello-spring/           ← 간단한 테스트 & 실습용 (버려도 되는 코드)
│
└── pong-to-rich/           ← 메인 Spring 프로젝트
    ├── PROJECT.md          ← 프로젝트 기획 전체 문서
    └── (Spring Boot 프로젝트)  ← 로드맵의 모든 Phase를 여기에 적용
```

---

## 프로젝트 방향

- **hello-spring**: 개념 확인용 간단한 테스트 코드. 가볍게 쓰고 버려도 됨
- **pong-to-rich**: 실제 서비스 수준으로 키워나가는 메인 프로젝트. ROADMAP.md의 모든 Phase를 이 프로젝트에 순차적으로 적용
  - 기획 전체 내용: [pong-to-rich/PROJECT.md](./pong-to-rich/PROJECT.md)
  - 아키텍처 진화: 모놀리식 → 멀티모듈 → MSA

---

## 학습 사이클

각 Phase는 아래 순서로 진행한다:

```
1. 이론 정리   → notes/phase-N-xxx/*.md 에 작성
2. 구현 실습   → pong-to-rich/ 에 실제 코드로 적용
3. 성능 비교   → 적용 전/후 차이를 측정하고 기록
4. 깨달은 점   → devlog/dayNN-날짜.md 에 정리
```

---

## devlog 작성 규칙

- 파일명: `dayNN-YYYY-MM-DD.md` (예: `day01-2026-04-07.md`)
- 내용 구성:

```markdown
# Day N — YYYY-MM-DD

## 오늘 한 것
- ...

## 이론 정리 링크
- [주제명](../notes/phase-N-xxx/파일명.md)

## 구현 내용
- ...

## 성능 비교
- ...

## 깨달은 점 / 막혔던 것
- ...

## 내일 할 것
- ...
```

---

## notes 작성 규칙

- 각 Phase 폴더 안에 주제별 `.md` 파일로 작성
- 파일명은 영어 소문자, 하이픈 구분 (예: `ioc-di.md`, `http-basics.md`)
- 이론 정리 후 해당 날짜의 devlog에서 링크로 연결

---

## 커밋 규칙

커밋은 목적별로 분리한다. 자세한 내용은 [COMMIT_CONVENTION.md](./COMMIT_CONVENTION.md) 참고

| 상황 | 타입 | 예시 |
|------|------|------|
| 이론 정리 | `study` | `study: Spring IoC/DI 이론 정리` |
| 구현 코드 | `feat` | `feat: 빈 수동 등록 실습 코드 추가` |
| devlog 작성 | `docs` | `docs: day01 devlog 작성` |
| 프로젝트 기획/구조 | `init` | `init: pong-to-rich Spring 프로젝트 초기 세팅` |

---

## 현재 진행 상황

전체 진행 현황은 [ROADMAP.md](./ROADMAP.md) 의 체크리스트로 관리
