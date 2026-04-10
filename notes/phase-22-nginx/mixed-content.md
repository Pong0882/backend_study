# Mixed Content

## Mixed Content란

HTTPS 페이지에서 HTTP 리소스를 요청하면 브라우저가 차단하는 보안 정책.

```
https://pongtrader.pro/swagger  ← HTTPS 페이지
    ↓ API 호출
http://pongtrader.pro/api/auth/login  ← HTTP 요청 → 브라우저 차단
```

브라우저 입장에서는 "안전한 페이지에서 안전하지 않은 요청을 보내는 것"이기 때문에 차단한다.

---

## CORS와 혼동하기 쉬운 이유

브라우저 에러 메시지가 `CORS` 또는 `Failed to fetch`로 뜨는 경우가 있어서 혼동하기 쉽다.

| 구분 | 원인 | 발생 조건 |
|------|------|----------|
| CORS | 다른 Origin으로 요청 시 서버가 허용 안 함 | 서버 설정 문제 |
| Mixed Content | HTTPS 페이지에서 HTTP 요청 | 프로토콜 불일치 |

**Postman / curl에서는 둘 다 안 걸린다.** 브라우저에서만 발생하는 제약이다.

---

## Cloudflare + Spring 환경에서 발생하는 이유

```
브라우저(HTTPS) → Cloudflare → Spring(HTTP 8080)
                                    ↑
                          Spring은 자신이 HTTP라고 인식
                          → Swagger 서버 URL을 http:// 로 설정
                          → 브라우저가 Mixed Content로 차단
```

Cloudflare가 SSL을 대신 처리(SSL Termination)하기 때문에, Spring 입장에서는 HTTP로 요청이 들어오는 것처럼 보인다. SpringDoc이 서버 URL을 자동 감지할 때 `http://`로 잡히는 이유다.

---

## 해결 방법

SpringDoc이 서버 URL을 자동 감지하는 대신, 환경변수로 명시적으로 주입한다.

```java
// SwaggerConfig.java
@Value("${springdoc.server-url:http://localhost:8080}")
private String serverUrl;

public OpenAPI openAPI() {
    return new OpenAPI()
            .addServersItem(new Server().url(serverUrl))
            ...
}
```

```yaml
# application-prod.yml
springdoc:
  server-url: https://pongtrader.pro
```

- local 환경 → 기본값 `http://localhost:8080`
- prod 환경 → `https://pongtrader.pro`

Spring Profile로 환경별로 다른 값이 주입된다. Spring이 자신을 HTTP라고 인식해도 Swagger에 표시되는 서버 URL은 HTTPS로 고정된다.

---

## pong-to-rich에서 사용된 곳

### SwaggerConfig.java
`@Value`로 `springdoc.server-url`을 주입받아 `OpenAPI` 빈에 명시적으로 설정.

### application-prod.yml
```yaml
springdoc:
  server-url: https://pongtrader.pro
```

prod 프로파일 활성화 시 자동 적용.
