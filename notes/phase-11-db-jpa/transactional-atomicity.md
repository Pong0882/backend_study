# @Transactional — 원자성과 트랜잭션 범위

## 트랜잭션이란

여러 DB 작업을 **하나의 단위**로 묶는 것. 모두 성공하거나 모두 실패해야 한다.

```
User 저장 성공
Portfolio 저장 실패
→ User 저장도 취소 (롤백)
```

이 원칙을 **원자성(Atomicity)** 이라고 한다. ACID의 A.

---

## @Transactional 동작 원리

Spring은 `@Transactional`이 붙은 메서드를 **프록시**로 감싼다.

```
클라이언트 호출
    ↓
프록시 (트랜잭션 시작)
    ↓
실제 메서드 실행
    ↓
정상 종료 → 커밋
예외 발생 → 롤백
```

개발자는 비즈니스 로직만 짜고, 트랜잭션 시작/커밋/롤백은 Spring이 처리한다.

---

## pong-to-rich 적용 사례 — signUp()

### 기존 코드 (문제)

```java
// @Transactional 없음
public void signUp(SignUpRequest request) {
    // ...
    userRepository.save(user);         // 1. User 저장 성공
    portfolioRepository.save(portfolio); // 2. Portfolio 저장 실패 시?
    // → User는 저장됐는데 Portfolio는 없는 불완전한 상태
}
```

User는 저장됐는데 Portfolio 저장이 실패하면 DB가 불일치 상태가 된다. 이 유저는 포트폴리오 없는 유저가 되어버린다.

### 수정 후

```java
@Transactional
public void signUp(SignUpRequest request) {
    // ...
    userRepository.save(user);
    portfolioRepository.save(Portfolio.builder().user(user).build());
    // Portfolio 저장 실패 → User 저장도 롤백
    // 둘 다 성공해야 커밋
}
```

---

## 언제 @Transactional이 필요한가

**DB 쓰기 작업이 2개 이상일 때** — 원자성이 필요하다.

```java
// 필요 — 두 테이블에 동시 저장
@Transactional
public void signUp() {
    userRepository.save(user);
    portfolioRepository.save(portfolio);
}

// 필요 — 조회 후 수정 (더티 체킹)
@Transactional
public void updateNickname(Long userId, String nickname) {
    User user = userRepository.findById(userId).orElseThrow();
    user.updateNickname(nickname);  // @Transactional 없으면 UPDATE 안 나감
}

// 불필요 — 단순 조회
public User findUser(Long userId) {
    return userRepository.findById(userId).orElseThrow();
}
```

---

## readOnly = true

조회 전용 트랜잭션에 붙이는 옵션.

```java
@Transactional(readOnly = true)
public User findUser(Long userId) {
    return userRepository.findById(userId).orElseThrow();
}
```

**효과:**
- 더티 체킹 비활성화 — 스냅샷을 안 만들어서 메모리 절약
- DB에 따라 읽기 전용 복제본으로 라우팅 가능 (MySQL Replication 환경)
- 실수로 수정 쿼리가 나가는 것을 방지

조회만 하는 메서드에 `readOnly = true`를 붙이는 것이 실무 관례다.

---

## 롤백 기준 — 어떤 예외에서 롤백되나

기본적으로 **RuntimeException과 Error**에서만 롤백된다.

```java
@Transactional
public void signUp() {
    userRepository.save(user);
    portfolioRepository.save(portfolio);  // RuntimeException 발생 → 롤백
    // CheckedException(IOException 등) 발생 → 롤백 안 됨 (기본값)
}
```

CheckedException에서도 롤백하려면:

```java
@Transactional(rollbackFor = Exception.class)
public void signUp() { ... }
```

pong-to-rich의 `BusinessException`은 `RuntimeException`을 상속하므로 별도 설정 없이 롤백된다.

---

## 주의사항 — 같은 클래스 내부 호출

`@Transactional`은 프록시 기반이라 **같은 클래스 내에서 호출하면 적용 안 된다.**

```java
@Service
public class AuthService {

    public void signUp() {
        internalSignUp();  // 프록시를 거치지 않음 → @Transactional 무시
    }

    @Transactional
    public void internalSignUp() {
        userRepository.save(user);
        portfolioRepository.save(portfolio);
    }
}
```

외부에서 호출되는 메서드에 `@Transactional`을 붙여야 한다.

---

## pong-to-rich에서 사용된 곳

| 메서드 | 이유 |
|--------|------|
| `AuthService.signUp()` | User + Portfolio 두 테이블 저장 — 원자성 필요 |
| `AuthService.login()` | Refresh Token 저장 포함 |
| `AuthService.logout()` | Refresh Token 삭제 |
