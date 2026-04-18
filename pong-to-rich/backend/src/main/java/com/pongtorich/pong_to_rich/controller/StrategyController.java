package com.pongtorich.pong_to_rich.controller;

import com.pongtorich.pong_to_rich.common.ApiResult;
import com.pongtorich.pong_to_rich.dto.strategy.StrategyCreateRequest;
import com.pongtorich.pong_to_rich.dto.strategy.StrategyResponse;
import com.pongtorich.pong_to_rich.service.StrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Strategy", description = "자동매매 전략 API")
@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyService strategyService;

    @Operation(summary = "전략 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "403", description = "증권사 계좌 권한 없음"),
            @ApiResponse(responseCode = "404", description = "계좌 또는 종목 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResult<StrategyResponse>> create(
            @Valid @RequestBody StrategyCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResult.ok(
                strategyService.create(authentication.getName(), request)));
    }

    @Operation(summary = "내 전략 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<StrategyResponse>>> getMyStrategies(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResult.ok(
                strategyService.getMyStrategies(authentication.getName())));
    }

    @Operation(summary = "전략 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "전략 없음")
    })
    @GetMapping("/{strategyId}")
    public ResponseEntity<ApiResult<StrategyResponse>> getStrategy(
            @PathVariable Long strategyId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResult.ok(
                strategyService.getStrategy(authentication.getName(), strategyId)));
    }

    @Operation(summary = "전략 활성화")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "활성화 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "전략 없음")
    })
    @PatchMapping("/{strategyId}/activate")
    public ResponseEntity<ApiResult<StrategyResponse>> activate(
            @PathVariable Long strategyId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResult.ok(
                strategyService.activate(authentication.getName(), strategyId)));
    }

    @Operation(summary = "전략 일시정지")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일시정지 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "전략 없음")
    })
    @PatchMapping("/{strategyId}/pause")
    public ResponseEntity<ApiResult<StrategyResponse>> pause(
            @PathVariable Long strategyId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResult.ok(
                strategyService.pause(authentication.getName(), strategyId)));
    }

    @Operation(summary = "전략 중지")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "중지 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "전략 없음")
    })
    @PatchMapping("/{strategyId}/deactivate")
    public ResponseEntity<ApiResult<StrategyResponse>> deactivate(
            @PathVariable Long strategyId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResult.ok(
                strategyService.deactivate(authentication.getName(), strategyId)));
    }

    @Operation(summary = "전략 삭제", description = "ACTIVE 상태에서는 삭제 불가")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "실행 중인 전략은 삭제 불가"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "전략 없음")
    })
    @DeleteMapping("/{strategyId}")
    public ResponseEntity<ApiResult<Void>> delete(
            @PathVariable Long strategyId,
            Authentication authentication) {
        strategyService.delete(authentication.getName(), strategyId);
        return ResponseEntity.ok(ApiResult.ok());
    }
}
