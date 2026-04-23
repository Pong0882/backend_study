# KIS 계좌별 토큰 캐싱 설계

## 왜 계좌별로 토큰을 분리해야 하나

KIS API는 **토큰 발급자와 요청자의 appkey가 일치**해야 한다.
서버 공용 appkey로 발급한 토큰을 사용자 appkey와 함께 주문 헤더에 보내면 KIS가 `EGW00202(GW 라우팅 오류)`로 거부한다.

```
잘못된 구조
서버 공용 appkey → 토큰 발급
사용자 appkey + 서버 토큰 → KIS 주문 API → EGW00202 거부

올바른 구조
사용자 appkey/appsecret → 토큰 발급
사용자 appkey + 사용자 토큰 → KIS 주문 API → 정상 처리
```

## 토큰 종류와 역할

| 토큰 | 발급자 | Redis 키 | 용도 |
|------|--------|----------|------|
| 공용 토큰 | 서버 appkey/appsecret | `kis:token` | 시세 조회, 일봉 수집 |
| 계좌 토큰 | 사용자 appkey/appsecret | `kis:order-token:{accountId}` | 주문, 체결 조회, 예수금 조회 |

공용 토큰은 로그인 없이 시세를 보는 사용자도 혜택을 받는다.
계좌 토큰은 실제 계좌 작업이 필요한 경우에만 발급한다.

## 토큰 유효 시간

KIS 토큰은 **24시간** 유효하다. Redis TTL을 만료 1시간 전으로 잡아서 만료 직전 재발급 없이 안전하게 사용한다.

```java
LocalDateTime expiredAt = LocalDateTime
    .parse(response.accessTokenExpired().replace(" ", "T"))
    .minusHours(1);  // 만료 1시간 전에 TTL 종료
long ttlSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiredAt);
redisTemplate.opsForValue().set(redisKey, token, ttlSeconds, TimeUnit.SECONDS);
```

## pong-to-rich에서 사용된 곳

- `KisAuthService` — 공용 토큰 발급 및 캐싱 (`kis:token`)
- `KisOrderService.getOrIssueToken()` — 계좌별 토큰 발급 및 캐싱 (`kis:order-token:{accountId}`)

```java
// KisOrderService
private String getOrIssueToken(BrokerAccount brokerAccount, String baseUrl) {
    String redisKey = "kis:order-token:" + brokerAccount.getId();
    String cached = redisTemplate.opsForValue().get(redisKey);
    if (cached != null) return cached;

    // 계좌 appkey/appsecret으로 토큰 직접 발급
    KisTokenResponse response = restClient.post()
        .uri(baseUrl + "/oauth2/tokenP")
        .body(Map.of("grant_type", "client_credentials",
                     "appkey", brokerAccount.getAppkey(),
                     "appsecret", brokerAccount.getAppsecret()))
        .retrieve()
        .body(KisTokenResponse.class);
    ...
}
```
