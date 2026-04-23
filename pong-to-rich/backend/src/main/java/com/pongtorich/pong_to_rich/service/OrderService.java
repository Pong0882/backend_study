package com.pongtorich.pong_to_rich.service;

import com.pongtorich.pong_to_rich.domain.broker.BrokerAccount;
import com.pongtorich.pong_to_rich.domain.broker.BrokerAccountRepository;
import com.pongtorich.pong_to_rich.domain.order.Order;
import com.pongtorich.pong_to_rich.domain.order.OrderRepository;
import com.pongtorich.pong_to_rich.domain.stock.Stock;
import com.pongtorich.pong_to_rich.domain.stock.StockRepository;
import com.pongtorich.pong_to_rich.domain.user.User;
import com.pongtorich.pong_to_rich.domain.user.UserRepository;
import com.pongtorich.pong_to_rich.dto.order.OrderCreateRequest;
import com.pongtorich.pong_to_rich.dto.order.OrderResponse;
import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;
import com.pongtorich.pong_to_rich.exception.auth.UserNotFoundException;
import com.pongtorich.pong_to_rich.exception.broker.BrokerAccountForbiddenException;
import com.pongtorich.pong_to_rich.exception.broker.BrokerAccountNotFoundException;
import com.pongtorich.pong_to_rich.exception.order.OrderCancelNotAllowedException;
import com.pongtorich.pong_to_rich.exception.order.OrderForbiddenException;
import com.pongtorich.pong_to_rich.exception.order.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BrokerAccountRepository brokerAccountRepository;
    private final StockRepository stockRepository;
    private final KisOrderService kisOrderService;

    // 수동 주문 생성
    @Transactional
    public OrderResponse create(String email, OrderCreateRequest request) {
        log.info("[주문] 생성 시도: {} — {}/{}", email, request.getStockCode(), request.getOrderType());

        User user = findUserByEmail(email);

        BrokerAccount brokerAccount = brokerAccountRepository.findById(request.getBrokerAccountId())
                .orElseThrow(() -> {
                    log.warn("[주문] 증권사 계좌 없음: {}", request.getBrokerAccountId());
                    return new BrokerAccountNotFoundException();
                });

        if (!brokerAccount.getUser().getEmail().equals(email)) {
            log.warn("[주문] 증권사 계좌 권한 없음: {} → accountId: {}", email, request.getBrokerAccountId());
            throw new BrokerAccountForbiddenException();
        }

        // LIMIT 주문에 price 미입력 시 검증
        if (request.getPriceType() == Order.PriceType.LIMIT && request.getPrice() == null) {
            log.warn("[주문] 지정가 미입력: {}", email);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Stock stock = stockRepository.findByCode(request.getStockCode())
                .orElseThrow(() -> {
                    log.warn("[주문] 종목 없음: {}", request.getStockCode());
                    return new BusinessException(ErrorCode.STOCK_NOT_FOUND);
                });

        Order order = Order.builder()
                .user(user)
                .brokerAccount(brokerAccount)
                .stock(stock)
                .orderType(request.getOrderType())
                .priceType(request.getPriceType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();

        orderRepository.save(order);
        log.info("[주문] DB 저장 완료 — ID: {}", order.getId());

        // KIS 주문 전송 — 실패 시 BusinessException 발생 → 트랜잭션 롤백 → DB 저장도 취소
        // 1단계: 동기 처리. 추후 비동기(@Async)로 교체 예정
        String kisOrderNo = kisOrderService.sendOrder(order, brokerAccount);
        order.assignKisOrderNo(kisOrderNo);

        OrderResponse response = OrderResponse.from(order);
        log.info("[주문] 생성 완료: {} — ID: {}, kisOrderNo: {}", email, response.id(), kisOrderNo);
        return response;
    }

    // 내 주문 목록 조회
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        log.info("[주문] 목록 조회: {}", email);

        User user = findUserByEmail(email);
        return orderRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(OrderResponse::from)
                .toList();
    }

    // 주문 단건 조회
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String email, Long orderId) {
        log.info("[주문] 단건 조회: {} — ID: {}", email, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        validateOwner(email, order);
        return OrderResponse.from(order);
    }

    // 주문 취소 — PENDING 상태에서만 가능
    @Transactional
    public OrderResponse cancel(String email, Long orderId) {
        log.info("[주문] 취소 시도: {} — ID: {}", email, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        validateOwner(email, order);

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            log.warn("[주문] 취소 불가 상태: {} — ID: {}, status: {}", email, orderId, order.getStatus());
            throw new OrderCancelNotAllowedException();
        }

        order.cancel();

        log.info("[주문] 취소 완료: {} — ID: {}", email, orderId);
        return OrderResponse.from(order);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[주문] 사용자 없음: {}", email);
                    return new UserNotFoundException();
                });
    }

    private void validateOwner(String email, Order order) {
        if (!order.getUser().getEmail().equals(email)) {
            log.warn("[주문] 권한 없음: {} → orderId: {}", email, order.getId());
            throw new OrderForbiddenException();
        }
    }
}
