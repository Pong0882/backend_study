package com.pongtorich.pong_to_rich.service;

import com.pongtorich.pong_to_rich.domain.broker.BrokerAccount;
import com.pongtorich.pong_to_rich.domain.order.Order;
import com.pongtorich.pong_to_rich.dto.kis.KisOrderResponse;
import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    public String sendOrder(Order order, BrokerAccount brokerAccount) {
        boolean isMock = brokerAccount.getAccountType() == BrokerAccount.AccountType.MOCK;
        String baseUrl = isMock ? KIS_MOCK_BASE_URL : KIS_REAL_BASE_URL;
        String trId = resolveTrId(order.getOrderType(), isMock);
        String token = kisAuthService.getAccessToken();

        String ordDvsn = order.getPriceType() == Order.PriceType.MARKET ? "01" : "00";
        String price = order.getPriceType() == Order.PriceType.MARKET
                ? "0"
                : order.getPrice().toPlainString();

        Map<String, String> body = Map.of(
                "CANO", brokerAccount.getAccountNumber(),
                "ACNT_PRDT_CD", "01",
                "PDNO", order.getStock().getCode(),
                "ORD_DVSN", ordDvsn,
                "ORD_QTY", String.valueOf(order.getQuantity()),
                "ORD_UNPR", price,
                "EXCG_ID_DVSN_CD", "KRX",
                "SLL_TYPE", "",
                "CNDT_PRIC", ""
        );

        log.info("[KisOrder] === 요청 전체 ===");
        log.info("[KisOrder] URL: {}", baseUrl + "/uapi/domestic-stock/v1/trading/order-cash");
        log.info("[KisOrder] Content-Type: application/json; charset=utf-8");
        log.info("[KisOrder] authorization: Bearer {}", token);
        log.info("[KisOrder] appkey: {}", brokerAccount.getAppkey());
        log.info("[KisOrder] appsecret: {}", brokerAccount.getAppsecret());
        log.info("[KisOrder] tr_id: {}", trId);
        log.info("[KisOrder] custtype: P");
        log.info("[KisOrder] body: {}", body);

        try {
            KisOrderResponse response = restClient.post()
                    .uri(baseUrl + "/uapi/domestic-stock/v1/trading/order-cash")
                    .header("Content-Type", "application/json")
                    .header("authorization", "Bearer " + token)
                    .header("appkey", brokerAccount.getAppkey())
                    .header("appsecret", brokerAccount.getAppsecret())
                    .header("tr_id", trId)
                    .header("custtype", "P")
                    .body(body)
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
