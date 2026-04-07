# Commit Convention

[Conventional Commits](https://www.conventionalcommits.org/) 기반 커밋 메시지 규칙

---

## 형식

```
<type>: <subject>

[body]
```

- **subject**: 50자 이내, 마침표 없이, 명령형으로
- **body**: 선택사항. 변경 이유 / 이전과 달라진 점 설명 (72자 줄바꿈)

---

## Type 종류

| Type | 설명 | 예시 |
|------|------|------|
| `init` | 프로젝트 초기 세팅 | `init: 프로젝트 초기 세팅` |
| `feat` | 새로운 기능 추가 | `feat: JWT 발급 기능 추가` |
| `fix` | 버그 수정 | `fix: 토큰 만료 시 NPE 수정` |
| `refactor` | 기능 변경 없는 코드 개선 | `refactor: UserService 메서드 분리` |
| `test` | 테스트 코드 추가/수정 | `test: UserService 단위 테스트 추가` |
| `docs` | 문서 추가/수정 | `docs: API 명세 업데이트` |
| `chore` | 빌드, 설정, 패키지 변경 | `chore: Gradle 의존성 업데이트` |
| `style` | 포맷, 세미콜론 등 코드 스타일 | `style: 코드 포맷 정리` |
| `perf` | 성능 개선 | `perf: N+1 쿼리 fetch join으로 개선` |
| `ci` | CI/CD 설정 변경 | `ci: GitHub Actions 배포 파이프라인 추가` |
| `study` | 학습 정리, 실습 코드 | `study: 커넥션 풀 성능 비교 실습` |

---

## 예시

```
feat: 회원가입 API 구현

- 이메일 중복 검증 추가
- 비밀번호 BCrypt 암호화 적용
```

```
fix: 로그인 시 Redis 연결 실패 오류 수정
```

```
study: HikariCP 풀 사이즈별 성능 비교 실습 완료
```

---

## 규칙

- 커밋은 **하나의 논리적 단위**로 쪼개기
- 하나의 커밋에 여러 목적 섞지 않기
- 이론 정리와 구현 코드는 **별도 커밋**으로 분리
