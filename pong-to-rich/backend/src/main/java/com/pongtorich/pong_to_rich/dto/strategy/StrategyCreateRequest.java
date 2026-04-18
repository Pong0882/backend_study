package com.pongtorich.pong_to_rich.dto.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "전략 생성 요청")
public class StrategyCreateRequest {

    @NotNull(message = "증권사 계좌 ID를 입력해주세요.")
    @Schema(description = "증권사 계좌 ID", example = "1")
    private Long brokerAccountId;

    @NotBlank(message = "종목 코드를 입력해주세요.")
    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @NotBlank(message = "전략 이름을 입력해주세요.")
    @Size(max = 100, message = "전략 이름은 100자 이하로 입력해주세요.")
    @Schema(description = "전략 이름", example = "삼성전자 RSI 전략")
    private String name;

    @NotNull(message = "1회 주문 수량을 입력해주세요.")
    @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
    @Schema(description = "1회 주문 수량", example = "10")
    private Integer orderQuantity;
}
