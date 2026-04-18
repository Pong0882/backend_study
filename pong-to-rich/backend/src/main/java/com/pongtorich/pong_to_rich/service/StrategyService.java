package com.pongtorich.pong_to_rich.service;

import com.pongtorich.pong_to_rich.domain.broker.BrokerAccount;
import com.pongtorich.pong_to_rich.domain.broker.BrokerAccountRepository;
import com.pongtorich.pong_to_rich.domain.stock.Stock;
import com.pongtorich.pong_to_rich.domain.stock.StockRepository;
import com.pongtorich.pong_to_rich.domain.strategy.Strategy;
import com.pongtorich.pong_to_rich.domain.strategy.StrategyRepository;
import com.pongtorich.pong_to_rich.domain.user.User;
import com.pongtorich.pong_to_rich.domain.user.UserRepository;
import com.pongtorich.pong_to_rich.dto.strategy.StrategyCreateRequest;
import com.pongtorich.pong_to_rich.dto.strategy.StrategyResponse;
import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;
import com.pongtorich.pong_to_rich.exception.auth.UserNotFoundException;
import com.pongtorich.pong_to_rich.exception.broker.BrokerAccountForbiddenException;
import com.pongtorich.pong_to_rich.exception.broker.BrokerAccountNotFoundException;
import com.pongtorich.pong_to_rich.exception.strategy.StrategyForbiddenException;
import com.pongtorich.pong_to_rich.exception.strategy.StrategyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyService {

    private final StrategyRepository strategyRepository;
    private final UserRepository userRepository;
    private final BrokerAccountRepository brokerAccountRepository;
    private final StockRepository stockRepository;

    // 전략 생성
    @Transactional
    public StrategyResponse create(String email, StrategyCreateRequest request) {
        log.info("[전략] 생성 시도: {} — {}", email, request.getName());

        User user = findUserByEmail(email);

        BrokerAccount brokerAccount = brokerAccountRepository.findById(request.getBrokerAccountId())
                .orElseThrow(() -> {
                    log.warn("[전략] 증권사 계좌 없음: {}", request.getBrokerAccountId());
                    return new BrokerAccountNotFoundException();
                });

        // 본인 계좌만 전략에 연결 가능
        if (!brokerAccount.getUser().getEmail().equals(email)) {
            log.warn("[전략] 증권사 계좌 권한 없음: {} → accountId: {}", email, request.getBrokerAccountId());
            throw new BrokerAccountForbiddenException();
        }

        Stock stock = stockRepository.findByCode(request.getStockCode())
                .orElseThrow(() -> {
                    log.warn("[전략] 종목 없음: {}", request.getStockCode());
                    return new BusinessException(ErrorCode.STOCK_NOT_FOUND);
                });

        Strategy strategy = Strategy.builder()
                .user(user)
                .brokerAccount(brokerAccount)
                .stock(stock)
                .name(request.getName())
                .orderQuantity(request.getOrderQuantity())
                .build();

        StrategyResponse response = StrategyResponse.from(strategyRepository.save(strategy));
        log.info("[전략] 생성 완료: {} — ID: {}", email, response.id());
        return response;
    }

    // 내 전략 목록 조회
    @Transactional(readOnly = true)
    public List<StrategyResponse> getMyStrategies(String email) {
        log.info("[전략] 목록 조회: {}", email);

        User user = findUserByEmail(email);
        return strategyRepository.findAllByUser(user).stream()
                .map(StrategyResponse::from)
                .toList();
    }

    // 전략 단건 조회
    @Transactional(readOnly = true)
    public StrategyResponse getStrategy(String email, Long strategyId) {
        log.info("[전략] 단건 조회: {} — ID: {}", email, strategyId);

        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(StrategyNotFoundException::new);

        validateOwner(email, strategy);
        return StrategyResponse.from(strategy);
    }

    // 전략 활성화
    @Transactional
    public StrategyResponse activate(String email, Long strategyId) {
        log.info("[전략] 활성화 시도: {} — ID: {}", email, strategyId);

        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(StrategyNotFoundException::new);

        validateOwner(email, strategy);
        strategy.activate();

        log.info("[전략] 활성화 완료: {} — ID: {}", email, strategyId);
        return StrategyResponse.from(strategy);
    }

    // 전략 일시정지
    @Transactional
    public StrategyResponse pause(String email, Long strategyId) {
        log.info("[전략] 일시정지 시도: {} — ID: {}", email, strategyId);

        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(StrategyNotFoundException::new);

        validateOwner(email, strategy);
        strategy.pause();

        log.info("[전략] 일시정지 완료: {} — ID: {}", email, strategyId);
        return StrategyResponse.from(strategy);
    }

    // 전략 중지 (비활성화)
    @Transactional
    public StrategyResponse deactivate(String email, Long strategyId) {
        log.info("[전략] 중지 시도: {} — ID: {}", email, strategyId);

        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(StrategyNotFoundException::new);

        validateOwner(email, strategy);
        strategy.deactivate();

        log.info("[전략] 중지 완료: {} — ID: {}", email, strategyId);
        return StrategyResponse.from(strategy);
    }

    // 전략 삭제 — ACTIVE 상태에서는 삭제 불가
    @Transactional
    public void delete(String email, Long strategyId) {
        log.info("[전략] 삭제 시도: {} — ID: {}", email, strategyId);

        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(StrategyNotFoundException::new);

        validateOwner(email, strategy);

        if (strategy.getStatus() == Strategy.Status.ACTIVE) {
            log.warn("[전략] 실행 중 삭제 시도: {} — ID: {}", email, strategyId);
            throw new BusinessException(ErrorCode.STRATEGY_INVALID_STATUS);
        }

        strategyRepository.delete(strategy);
        log.info("[전략] 삭제 완료: {} — ID: {}", email, strategyId);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[전략] 사용자 없음: {}", email);
                    return new UserNotFoundException();
                });
    }

    private void validateOwner(String email, Strategy strategy) {
        if (!strategy.getUser().getEmail().equals(email)) {
            log.warn("[전략] 권한 없음: {} → strategyId: {}", email, strategy.getId());
            throw new StrategyForbiddenException();
        }
    }
}
