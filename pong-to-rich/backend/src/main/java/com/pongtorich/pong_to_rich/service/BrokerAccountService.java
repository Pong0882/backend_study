package com.pongtorich.pong_to_rich.service;

import com.pongtorich.pong_to_rich.domain.broker.BrokerAccount;
import com.pongtorich.pong_to_rich.domain.broker.BrokerAccountRepository;
import com.pongtorich.pong_to_rich.domain.user.User;
import com.pongtorich.pong_to_rich.domain.user.UserRepository;
import com.pongtorich.pong_to_rich.dto.broker.BrokerAccountCreateRequest;
import com.pongtorich.pong_to_rich.dto.broker.BrokerAccountResponse;
import com.pongtorich.pong_to_rich.dto.kis.KisTokenResponse;
import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;
import com.pongtorich.pong_to_rich.exception.auth.UserNotFoundException;
import com.pongtorich.pong_to_rich.exception.broker.BrokerAccountDuplicateException;
import com.pongtorich.pong_to_rich.exception.broker.BrokerAccountForbiddenException;
import com.pongtorich.pong_to_rich.exception.broker.BrokerAccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerAccountService {

    // MOCK/REAL base URL이 다름 — accountType에 따라 분기
    private static final String KIS_MOCK_BASE_URL = "https://openapivts.koreainvestment.com:29443";
    private static final String KIS_REAL_BASE_URL = "https://openapi.koreainvestment.com:9443";

    private final BrokerAccountRepository brokerAccountRepository;
    private final UserRepository userRepository;
    private final RestClient restClient;

    // 증권사 계좌 등록
    @Transactional
    public BrokerAccountResponse create(String email, BrokerAccountCreateRequest request) {
        log.info("[증권사계좌] 등록 시도: {} — {}/{}", email, request.getBroker(), request.getAccountType());

        User user = findUserByEmail(email);

        if (brokerAccountRepository.existsByUserAndBrokerAndAccountType(
                user, request.getBroker(), request.getAccountType())) {
            log.warn("[증권사계좌] 중복 등록: {} — {}/{}", email, request.getBroker(), request.getAccountType());
            throw new BrokerAccountDuplicateException();
        }

        validateKisApiKey(request.getAppkey(), request.getAppsecret(), request.getAccountType());

        BrokerAccount account = BrokerAccount.builder()
                .user(user)
                .broker(request.getBroker())
                .accountType(request.getAccountType())
                .appkey(request.getAppkey())
                .appsecret(request.getAppsecret())
                .build();

        BrokerAccountResponse response = BrokerAccountResponse.from(brokerAccountRepository.save(account));
        log.info("[증권사계좌] 등록 완료: {} — ID: {}", email, response.id());
        return response;
    }

    // KIS API 토큰 발급 시도로 appkey/appsecret 유효성 검증
    // 검증용 토큰은 저장하지 않음 (KisAuthService 캐시와 무관)
    private void validateKisApiKey(String appkey, String appsecret, BrokerAccount.AccountType accountType) {
        String baseUrl = accountType == BrokerAccount.AccountType.MOCK ? KIS_MOCK_BASE_URL : KIS_REAL_BASE_URL;
        log.info("[증권사계좌] KIS API 키 검증 시작 — {}", accountType);

        try {
            KisTokenResponse response = restClient.post()
                    .uri(baseUrl + "/oauth2/tokenP")
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", appkey,
                            "appsecret", appsecret
                    ))
                    .retrieve()
                    .body(KisTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                log.warn("[증권사계좌] KIS API 키 검증 실패 — 토큰 응답 없음");
                throw new BusinessException(ErrorCode.BROKER_ACCOUNT_INVALID_KEY);
            }

            log.info("[증권사계좌] KIS API 키 검증 성공 — {}", accountType);
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            if (body.contains("EGW00133")) {
                log.warn("[증권사계좌] KIS API rate limit — {}: {}", accountType, e.getMessage());
                throw new BusinessException(ErrorCode.BROKER_ACCOUNT_KIS_RATE_LIMIT);
            }
            log.warn("[증권사계좌] KIS API 키 검증 실패 — {}: {}", accountType, e.getMessage());
            throw new BusinessException(ErrorCode.BROKER_ACCOUNT_INVALID_KEY);
        } catch (RestClientException e) {
            log.warn("[증권사계좌] KIS API 키 검증 실패 — {}: {}", accountType, e.getMessage());
            throw new BusinessException(ErrorCode.BROKER_ACCOUNT_INVALID_KEY);
        }
    }

    // 내 계좌 목록 조회
    @Transactional(readOnly = true)
    public List<BrokerAccountResponse> getMyAccounts(String email) {
        log.info("[증권사계좌] 목록 조회: {}", email);

        User user = findUserByEmail(email);
        return brokerAccountRepository.findAllByUser(user).stream()
                .map(BrokerAccountResponse::from)
                .toList();
    }

    // 계좌 단건 조회 (소유자 검증 포함)
    @Transactional(readOnly = true)
    public BrokerAccountResponse getAccount(String email, Long accountId) {
        log.info("[증권사계좌] 단건 조회: {} — ID: {}", email, accountId);

        BrokerAccount account = brokerAccountRepository.findById(accountId)
                .orElseThrow(BrokerAccountNotFoundException::new);

        validateOwner(email, account);
        return BrokerAccountResponse.from(account);
    }

    // 계좌 활성화
    @Transactional
    public void activate(String email, Long accountId) {
        log.info("[증권사계좌] 활성화 시도: {} — ID: {}", email, accountId);

        BrokerAccount account = brokerAccountRepository.findById(accountId)
                .orElseThrow(BrokerAccountNotFoundException::new);

        validateOwner(email, account);
        account.activate();

        log.info("[증권사계좌] 활성화 완료: {} — ID: {}", email, accountId);
    }

    // 계좌 비활성화 (삭제 대신 소프트 비활성화)
    @Transactional
    public void deactivate(String email, Long accountId) {
        log.info("[증권사계좌] 비활성화 시도: {} — ID: {}", email, accountId);

        BrokerAccount account = brokerAccountRepository.findById(accountId)
                .orElseThrow(BrokerAccountNotFoundException::new);

        validateOwner(email, account);
        account.deactivate();

        log.info("[증권사계좌] 비활성화 완료: {} — ID: {}", email, accountId);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[증권사계좌] 사용자 없음: {}", email);
                    return new UserNotFoundException();
                });
    }

    private void validateOwner(String email, BrokerAccount account) {
        if (!account.getUser().getEmail().equals(email)) {
            log.warn("[증권사계좌] 권한 없음: {} → accountId: {}", email, account.getId());
            throw new BrokerAccountForbiddenException();
        }
    }
}
