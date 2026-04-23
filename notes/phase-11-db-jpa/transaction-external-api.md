# 외부 API 호출과 트랜잭션 분리

## 문제

외부 API 호출을 트랜잭션 안에 넣으면 두 가지 문제가 생긴다.

**1. DB 커넥션 홀딩**

```java
@Transactional
public void create() {
    order = save(order)      // 트랜잭션 열림 → DB 커넥션 점유
    kisApi.sendOrder()       // 외부 API 대기 (수백ms~수초)
    order.fill()             // 상태 업데이트
}                            // 트랜잭션 닫힘 → 커넥션 반환
```

KIS API 응답 대기 중에 DB 커넥션을 계속 잡고 있다.
동시 요청이 많아지면 HikariCP 커넥션 풀이 고갈된다.

**2. 데이터 불일치**

```
DB 저장 성공 + KIS 주문 접수 성공
→ DB 상태 업데이트 실패
→ 실제로는 체결됐는데 DB에는 PENDING 상태
```

외부 API는 이미 실행됐는데 트랜잭션 롤백이 되면 되돌릴 방법이 없다.

## 올바른 구조 — 트랜잭션 분리

```
트랜잭션 1: Order PENDING 저장 → 커밋 (커넥션 반환)
외부 호출:  KIS API 주문 전송 (트랜잭션 밖)
트랜잭션 2: KIS 응답으로 상태 업데이트 → 커밋
```

각 단계가 독립적으로 커밋되므로 실패 지점이 명확하고, 외부 API 대기 중에는 DB 커넥션을 잡지 않는다.

## 단계별 진화 계획 (pong-to-rich)

| 단계 | 방식 | 문제 |
|------|------|------|
| 1단계 (현재) | 동기 + 단일 트랜잭션 | 커넥션 홀딩, 불일치 가능 |
| 2단계 | 트랜잭션 분리 | 커넥션 문제 해결 |
| 3단계 | `@Scheduled` 폴링 | 체결 확인 자동화 |
| 4단계 | `@Async` 비동기 | 응답 시간 개선 |
| 5단계 | KIS WebSocket | 실시간 체결 통보 |

## pong-to-rich에서 사용된 곳

- `OrderService.create()` — 1단계: 동기 처리, 단일 `@Transactional`
- KIS 주문 전송 실패 시 `BusinessException` → 트랜잭션 롤백 → DB 저장도 취소

```java
@Transactional
public OrderResponse create(String email, OrderCreateRequest request) {
    ...
    orderRepository.save(order);          // DB 저장

    // KIS 실패 시 BusinessException → 롤백
    String kisOrderNo = kisOrderService.sendOrder(order, brokerAccount);
    order.assignKisOrderNo(kisOrderNo);
    ...
}
```

> 1단계는 의도적으로 단순하게 구현. 직접 에러를 만나면서 2단계 이상으로 개선 예정.
