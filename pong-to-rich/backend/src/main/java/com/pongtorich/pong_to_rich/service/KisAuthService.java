package com.pongtorich.pong_to_rich.service;

import com.pongtorich.pong_to_rich.config.KisConfig;
import com.pongtorich.pong_to_rich.domain.broker.BrokerAccount;
import com.pongtorich.pong_to_rich.dto.kis.KisTokenResponse;
import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class KisAuthService {

    private final KisConfig kisConfig;
    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;

    public KisAuthService(KisConfig kisConfig, StringRedisTemplate redisTemplate, RestClient restClient) {
        this.kisConfig = kisConfig;
        this.redisTemplate = redisTemplate;
        this.restClient = restClient;
    }

    // 시세 조회 등 계좌 무관한 마켓 데이터용 — 공용 appkey/appsecret 사용
    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get("kis:token:common");
        if (cached != null) {
            log.debug("[KisAuth] 캐시 히트 — 공용 토큰");
            return cached;
        }
        log.info("[KisAuth] 캐시 미스 — 공용 토큰 KIS API 호출");
        return issueCommonToken();
    }

    private String issueCommonToken() {
        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", kisConfig.appKey(),
                "appsecret", kisConfig.appSecret()
        );

        try {
            KisTokenResponse response = restClient.post()
                    .uri(kisConfig.mockBaseUrl() + "/oauth2/tokenP")
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(KisTokenResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.KIS_AUTH_FAILED);
            }

            LocalDateTime expiredAt = LocalDateTime
                    .parse(response.accessTokenExpired().replace(" ", "T"))
                    .minusHours(1);
            long ttlSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiredAt);

            redisTemplate.opsForValue().set("kis:token:common", response.accessToken(), ttlSeconds, TimeUnit.SECONDS);
            log.info("[KisAuth] 공용 토큰 발급 완료 — TTL {}초", ttlSeconds);

            return response.accessToken();
        } catch (HttpClientErrorException e) {
            if (e.getResponseBodyAsString().contains("EGW00133")) {
                log.warn("[KisAuth] 공용 토큰 rate limit — {}", e.getResponseBodyAsString());
                throw new BusinessException(ErrorCode.BROKER_ACCOUNT_KIS_RATE_LIMIT);
            }
            log.warn("[KisAuth] 공용 토큰 발급 실패 — {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.KIS_AUTH_FAILED);
        }
    }

    // 주문 등 계좌 인증이 필요한 용도 — 계좌별 appkey/appsecret 사용
    public String getAccessToken(BrokerAccount account) {
        boolean isMock = account.getAccountType() == BrokerAccount.AccountType.MOCK;
        // 계좌별로 토큰을 캐싱 — 계좌 ID 기반 key
        String redisKey = "kis:token:" + account.getId();
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            log.debug("[KisAuth] 캐시 히트 — 계좌 ID {}", account.getId());
            return cached;
        }
        log.info("[KisAuth] 캐시 미스 — KIS API 호출 (계좌 ID {}, {})", account.getId(), isMock ? "모의" : "실전");
        return issueToken(account, redisKey);
    }

    private String issueToken(BrokerAccount account, String redisKey) {
        boolean isMock = account.getAccountType() == BrokerAccount.AccountType.MOCK;
        String baseUrl = isMock ? kisConfig.mockBaseUrl() : kisConfig.realBaseUrl();

        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", account.getAppkey(),
                "appsecret", account.getAppsecret()
        );

        try {
            KisTokenResponse response = restClient.post()
                    .uri(baseUrl + "/oauth2/tokenP")
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(KisTokenResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.KIS_AUTH_FAILED);
            }

            LocalDateTime expiredAt = LocalDateTime
                    .parse(response.accessTokenExpired().replace(" ", "T"))
                    .minusHours(1);
            long ttlSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiredAt);

            redisTemplate.opsForValue().set(redisKey, response.accessToken(), ttlSeconds, TimeUnit.SECONDS);
            log.info("[KisAuth] 토큰 발급 완료 — 계좌 ID {}, TTL {}초", account.getId(), ttlSeconds);

            return response.accessToken();
        } catch (HttpClientErrorException e) {
            if (e.getResponseBodyAsString().contains("EGW00133")) {
                log.warn("[KisAuth] 계좌 {} rate limit — {}", account.getId(), e.getResponseBodyAsString());
                throw new BusinessException(ErrorCode.BROKER_ACCOUNT_KIS_RATE_LIMIT);
            }
            log.warn("[KisAuth] 계좌 {} 토큰 발급 실패 — {}", account.getId(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.KIS_AUTH_FAILED);
        }
    }
}