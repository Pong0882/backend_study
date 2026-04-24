package com.pongtorich.pong_to_rich.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pongtorich.pong_to_rich.domain.broker.BrokerAccount;
import com.pongtorich.pong_to_rich.domain.order.Order;
import com.pongtorich.pong_to_rich.dto.kis.KisOrderResponse;
import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisOrderService {

    private static final String KIS_MOCK_BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String KIS_REAL_BASE_URL = "https://openapi.koreainvestment.com:9443";

    private static final String TR_BUY_MOCK  = "VTTC0012U";
    private static final String TR_SELL_MOCK = "VTTC0011U";
    private static final String TR_BUY_REAL  = "TTTC0012U";
    private static final String TR_SELL_REAL = "TTTC0011U";

    private final RestClient restClient;
    private final KisAuthService kisAuthService;
    private final ObjectMapper objectMapper;

    public String sendOrder(Order order, BrokerAccount brokerAccount) {
        boolean isMock = brokerAccount.getAccountType() == BrokerAccount.AccountType.MOCK;
        String baseUrl = isMock ? KIS_MOCK_BASE_URL : KIS_REAL_BASE_URL;
        String trId = resolveTrId(order.getOrderType(), isMock);
        String token = kisAuthService.getAccessToken(brokerAccount);

        String ordDvsn = order.getPriceType() == Order.PriceType.MARKET ? "01" : "00";
        String price = order.getPriceType() == Order.PriceType.MARKET
                ? "0"
                : order.getPrice().toPlainString();

        // LinkedHashMap으로 필드 순서 보장 — KIS가 chunked 인코딩을 처리 못해서
        // Map을 JSON 문자열로 직렬화 후 byte[]로 전달 → Content-Length 헤더가 자동 설정됨
        Map<String, String> bodyMap = new LinkedHashMap<>();
        bodyMap.put("CANO", brokerAccount.getAccountNumber());
        bodyMap.put("ACNT_PRDT_CD", "01");
        bodyMap.put("PDNO", order.getStock().getCode());
        bodyMap.put("ORD_DVSN", ordDvsn);
        bodyMap.put("ORD_QTY", String.valueOf(order.getQuantity()));
        bodyMap.put("ORD_UNPR", price);
        bodyMap.put("EXCG_ID_DVSN_CD", "KRX");
        bodyMap.put("SLL_TYPE", "");
        bodyMap.put("CNDT_PRIC", "");

        byte[] bodyBytes;
        try {
            bodyBytes = objectMapper.writeValueAsBytes(bodyMap);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.KIS_ORDER_FAILED);
        }

        log.info("[KisOrder] === 요청 전체 ===");
        log.info("[KisOrder] URL: {}", baseUrl + "/uapi/domestic-stock/v1/trading/order-cash");
        log.info("[KisOrder] tr_id: {}", trId);
        log.info("[KisOrder] body: {}", new String(bodyBytes, StandardCharsets.UTF_8));

        try {
            KisOrderResponse response = restClient.post()
                    .uri(baseUrl + "/uapi/domestic-stock/v1/trading/order-cash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", "Bearer " + token)
                    .header("appkey", brokerAccount.getAppkey())
                    .header("appsecret", brokerAccount.getAppsecret())
                    .header("tr_id", trId)
                    .header("custtype", "P")
                    .body(bodyBytes)
                    .retrieve()
                    .body(KisOrderResponse.class);

            if (response == null || !response.isSuccess()) {
                String msg = response != null ? response.msg1() : "응답 없음";
                log.warn("[KisOrder] 주문 실패 — {}", msg);
                throw new BusinessException(ErrorCode.KIS_ORDER_FAILED);
            }

            log.info("[KisOrder] 주문 접수 완료 — kisOrderNo: {}", response.kisOrderNo());
            return response.kisOrderNo();

        } catch (RestClientException e) {
            log.warn("[KisOrder] 주문 전송 중 오류 — {}", e.getMessage());
            throw new BusinessException(ErrorCode.KIS_ORDER_FAILED);
        }
    }

    private String resolveTrId(Order.OrderType orderType, boolean isMock) {
        if (isMock) {
            return orderType == Order.OrderType.BUY ? TR_BUY_MOCK : TR_SELL_MOCK;
        }
        return orderType == Order.OrderType.BUY ? TR_BUY_REAL : TR_SELL_REAL;
    }
}
