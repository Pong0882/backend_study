package com.pongtorich.pong_to_rich.dto.strategy;

import com.pongtorich.pong_to_rich.domain.strategy.Strategy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "전략 응답")
public record StrategyResponse(
        @Schema(description = "전략 ID") Long id,
        @Schema(description = "전략 이름") String name,
        @Schema(description = "종목 코드") String stockCode,
        @Schema(description = "종목명") String stockName,
        @Schema(description = "증권사") String broker,
        @Schema(description = "계좌 유형") String accountType,
        @Schema(description = "1회 주문 수량") Integer orderQuantity,
        @Schema(description = "전략 상태") String status,
        @Schema(description = "마지막 체크 시각") LocalDateTime lastCheckedAt,
        @Schema(description = "생성일시") LocalDateTime createdAt
) {
    public static StrategyResponse from(Strategy strategy) {
        return new StrategyResponse(
                strategy.getId(),
                strategy.getName(),
                strategy.getStock().getCode(),
                strategy.getStock().getName(),
                strategy.getBrokerAccount().getBroker().name(),
                strategy.getBrokerAccount().getAccountType().name(),
                strategy.getOrderQuantity(),
                strategy.getStatus().name(),
                strategy.getLastCheckedAt(),
                strategy.getCreatedAt()
        );
    }
}
