# 전역 예외 처리 (@RestControllerAdvice)

## Java 예외 계층 구조

```
Throwable
├── Error              ← JVM 수준 오류 (OOM 등). 잡으면 안 됨
└── Exception
    ├── Checked Exception   ← 컴파일러가 강제. throws 선언 필요 (IOException 등)
    └── RuntimeException    ← Unchecked. throws 선언 없이 던질 수 있음
```

Spring에서는 **RuntimeException**을 사용한다. Checked Exception은 메서드마다 `throws` 선언이 필요해서 코드가 지저분해지고, Spring의 트랜잭션 롤백도 기본적으로 RuntimeException 기준이기 때문이다.

---

## 문제 — 예외 처리가 없으면

Controller에서 예외가 터져도 아무도 안 잡으면 Spring이 알아서 500으로 변환한다.

```
중복 이메일 → IllegalArgumentException → 500
비밀번호 틀림 → BadCredentialsException → 500
DB 연결 실패 → DataAccessException → 500
```

클라이언트는 뭐가 문제인지 알 수 없다.

---

## @RestControllerAdvice

모든 Controller에서 발생하는 예외를 한 곳에서 잡아서 HTTP 응답으로 변환하는 클래스.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e) {
        return ResponseEntity
                .status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
```

- `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. 반환값을 JSON으로 직렬화
- `@ExceptionHandler(타입.class)` — 해당 타입의 예외가 오면 이 메서드가 처리
- 자식 예외도 잡힌다. `BusinessException`으로 선언하면 `DuplicateEmailException` 등 모든 자식도 처리

---

## 커스텀 예외 계층 구조

```
RuntimeException
    └── BusinessException       ← 모든 커스텀 예외의 공통 부모
            ├── auth/
            │   ├── DuplicateEmailException      → 409
            │   ├── InvalidCredentialsException  → 401
            │   ├── InvalidTokenException        → 401
            │   ├── ExpiredTokenException        → 401
            │   └── UserNotFoundException        → 404
            └── (도메인 추가 시 계속 확장)
```

**BusinessException을 중간에 두는 이유** — `@ExceptionHandler(BusinessException.class)` 하나로 모든 커스텀 예외를 잡을 수 있다. 예외가 늘어도 핸들러는 수정 불필요.

---

## ErrorCode Enum

예외 이름, HTTP 상태코드, 메시지를 한 곳에서 관리.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다. 다시 로그인해주세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
```

Service는 HTTP 상태코드를 몰라도 된다. `throw new DuplicateEmailException()`만 하면 ErrorCode가 알아서 409로 변환.

---

## 전체 흐름

```
AuthService
    throw new DuplicateEmailException()
        ↓
    DuplicateEmailException 생성 → super(ErrorCode.DUPLICATE_EMAIL) 호출
    → 예외 안에 ErrorCode.DUPLICATE_EMAIL 박힘
        ↓
    Controller까지 전파 (아무도 안 잡음)
        ↓
    DispatcherServlet → HandlerExceptionResolver 탐색
        ↓
    GlobalExceptionHandler.handle(BusinessException e) 가 잡음
        ↓
    e.getErrorCode() → DUPLICATE_EMAIL
    e.getErrorCode().getStatus() → 409
    e.getErrorCode().getMessage() → "이미 사용 중인 이메일입니다."
        ↓
    ResponseEntity 409 + { "code": "DUPLICATE_EMAIL", "message": "..." }
```

---

## 새 도메인 예외 추가 시

1. `ErrorCode`에 새 항목 추가
2. `exception/도메인명/` 폴더에 예외 클래스 추가
3. `GlobalExceptionHandler` — 수정 불필요

```java
// ErrorCode
ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),

// exception/order/OrderNotFoundException.java
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException() {
        super(ErrorCode.ORDER_NOT_FOUND);
    }
}
```

---

## pong-to-rich에서 사용된 곳

### GlobalExceptionHandler.java
모든 Controller 예외를 잡아서 `ErrorResponse { code, message }` 형태로 응답.

### AuthService.java
```java
// 기존 (IllegalArgumentException)
throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");

// 변경 후 (커스텀 예외)
throw new DuplicateEmailException();
```

### 응답 예시
```json
// 중복 이메일 409
{
  "code": "DUPLICATE_EMAIL",
  "message": "이미 사용 중인 이메일입니다."
}

// 유효하지 않은 토큰 401
{
  "code": "INVALID_TOKEN",
  "message": "유효하지 않은 토큰입니다."
}
```
