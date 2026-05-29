/*
 * 檔案用途：領域服務，封裝撮合、風控、Polymarket 同步與交易規則。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketClobOrderRequest;
import com.example.exchange.domain.model.dto.PolymarketNormalizedClobOrder;
import com.example.exchange.domain.model.dto.PolymarketPlaceOrderRequest;
import com.example.exchange.domain.model.dto.PolymarketPlaceOrderResponse;
import com.example.exchange.domain.model.entity.PredictionMarketInfo;
import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.model.entity.PredictionSessionRecord;
import com.example.exchange.domain.model.enums.PolymarketClobSide;
import com.example.exchange.domain.model.enums.PolymarketOrderDirection;
import com.example.exchange.domain.model.enums.PolymarketOrderType;
import com.example.exchange.domain.repository.jpa.PredictionMarketInfoRepository;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketOrderRepository;
import com.example.exchange.domain.util.PolymarketOrderSigner;
import com.example.exchange.infra.config.PolymarketConfigs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

/**
 * Polymarket order service。
 *
 * Deposit Wallet + POLY_1271 架構：
 *
 * 1. 使用者資產放在 Polymarket Deposit Wallet。
 * 2. 下單時，真正檢查 allowance / approval 的 owner 是 Deposit Wallet。
 * 3. Session Signer 只負責簽 CLOB order。
 * 4. maker / signer / signatureType 由 PolymarketOrderSigner 處理。
 *
 * TODO:
 * 1. prediction_market_info 補 neg_risk 欄位。
 * 2. no_buy_price / no_sell_price 建議直接入庫，不要每次推導。
 * 3. 下單成功後補內部 order record / ledger / reconciliation。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketOrderService {

    private static final BigDecimal ATOMIC_UNIT =
            new BigDecimal("1000000");

    private final PolymarketConfigs polymarketConfigs;

    private final PolymarketClobTradingClient polymarketClobTradingClient;

    private final PolymarketSessionService polymarketSessionService;

    private final PolymarketApprovalService polymarketApprovalService;

    private final PredictionMarketInfoRepository predictionMarketInfoRepository;

    private final PredictionPolymarketOrderRepository polymarketOrderRepository;

    /**
     * 建立真實 Polymarket CLOB order。
     */
    public PolymarketPlaceOrderResponse placeOrder(
            PolymarketPlaceOrderRequest request
    ) {
        String internalOrderId =
                resolveInternalOrderId(request);
        PredictionPolymarketOrder orderRecord = null;

        logStart(
                internalOrderId,
                request
        );

        try {
            validate(request);

            Optional<PolymarketPlaceOrderResponse> duplicate =
                    duplicateResponseIfPresent(
                            internalOrderId,
                            request
                    );
            if (duplicate.isPresent()) {
                return duplicate.get();
            }

            PredictionSessionRecord sessionRecord =
                    polymarketSessionService.getActiveSession(
                            request.getSessionId()
                    );

            String signingPrivateKey =
                    resolveSigningPrivateKey(sessionRecord);

            String polygonSignerAddress =
                    Credentials
                            .create(signingPrivateKey)
                            .getAddress();

            polymarketSessionService.assertAndConsumeLimit(
                    sessionRecord,
                    request.getUsdtAmount()
            );

            /**
             * Deposit Wallet / POLY_1271 模式：
             *
             * 資產 owner 是 deposit wallet，
             * 不是 session signer，
             * 也不是前端登入 MetaMask EOA。
             */
            String assetOwner =
                    polymarketConfigs
                            .getWallet()
                            .getFunderAddress();

            PredictionMarketInfo marketInfo =
                    predictionMarketInfoRepository
                            .findByMarketSlug(request.getMarketSlug())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "market not found: "
                                                    + request.getMarketSlug()
                                    )
                            );

            PolymarketNormalizedClobOrder normalizedOrder =
                    buildNormalizedOrder(
                            request,
                            marketInfo
                    );

            orderRecord =
                    createOrderRecord(
                            internalOrderId,
                            request,
                            marketInfo,
                            normalizedOrder
                    );

            log.info(
                    "[PolymarketOrder] Normalized order. internalOrderId={}, marketSlug={}, tokenId={}, side={}, price={}, size={}, usdtAmount={}, orderType={}, negRisk={}, assetOwner={}",
                    internalOrderId,
                    request.getMarketSlug(),
                    normalizedOrder.getTokenId(),
                    normalizedOrder.getSide(),
                    normalizedOrder.getPrice(),
                    normalizedOrder.getSize(),
                    normalizedOrder.getUsdtAmount(),
                    normalizedOrder.getOrderType(),
                    normalizedOrder.getNegRisk(),
                    assetOwner
            );

            checkApprovalBeforeOrder(
                    assetOwner,
                    normalizedOrder
            );

            PolymarketClobOrderRequest clobOrderRequest =
                    PolymarketOrderSigner.sign(
                            polymarketConfigs,
                            signingPrivateKey,
                            normalizedOrder
                    );

            logSignedOrder(
                    internalOrderId,
                    clobOrderRequest
            );

            PolymarketPlaceOrderResponse response =
                    polymarketClobTradingClient.postOrder(
                            polygonSignerAddress,
                            clobOrderRequest
                    );

            fillResponse(
                    response,
                    internalOrderId,
                    normalizedOrder
            );

            updateOrderRecord(
                    orderRecord,
                    response
            );

            logResult(
                    internalOrderId,
                    request,
                    response
            );

            return response;

        } catch (Exception e) {
            log.error(
                    "[PolymarketOrder] Place order exception. internalOrderId={}, userId={}, sessionId={}, marketSlug={}, direction={}",
                    internalOrderId,
                    request == null ? null : request.getUserId(),
                    request == null ? null : request.getSessionId(),
                    request == null ? null : request.getMarketSlug(),
                    request == null ? null : request.getDirection(),
                    e
            );

            markOrderException(
                    orderRecord,
                    e
            );

            return PolymarketPlaceOrderResponse.builder()
                    .success(false)
                    .internalOrderId(internalOrderId)
                    .status("EXCEPTION")
                    .errorMsg(normalizeError(e))
                    .build();
        }
    }

    private String resolveInternalOrderId(
            PolymarketPlaceOrderRequest request
    ) {
        if (request != null
                && request.getClientRequestId() != null
                && !request.getClientRequestId().isBlank()) {
            return request.getClientRequestId().trim();
        }
        return UUID.randomUUID().toString();
    }

    private Optional<PolymarketPlaceOrderResponse> duplicateResponseIfPresent(
            String internalOrderId,
            PolymarketPlaceOrderRequest request
    ) {
        if (request.getClientRequestId() == null
                || request.getClientRequestId().isBlank()) {
            return Optional.empty();
        }
        return polymarketOrderRepository.findByInternalOrderId(internalOrderId)
                .map(existing -> responseFromExistingOrder(existing, request));
    }

    private PolymarketPlaceOrderResponse responseFromExistingOrder(
            PredictionPolymarketOrder existing,
            PolymarketPlaceOrderRequest request
    ) {
        if (!sameIdempotentPayload(existing, request)) {
            return PolymarketPlaceOrderResponse.builder()
                    .success(false)
                    .internalOrderId(existing.getInternalOrderId())
                    .status("IDEMPOTENCY_CONFLICT")
                    .errorMsg("clientRequestId already used with different payload")
                    .build();
        }
        boolean terminalSuccess =
                existing.getClobOrderId() != null
                        && !"FAILED".equalsIgnoreCase(existing.getStatus())
                        && !"EXCEPTION".equalsIgnoreCase(existing.getStatus());
        boolean uncertain =
                existing.getClobOrderId() == null
                        && ("CREATED".equalsIgnoreCase(existing.getStatus())
                        || existing.getStatus() == null);
        return PolymarketPlaceOrderResponse.builder()
                .success(terminalSuccess)
                .internalOrderId(existing.getInternalOrderId())
                .clobOrderId(existing.getClobOrderId())
                .status(uncertain ? "CLOB_OUTCOME_UNCERTAIN" : existing.getStatus())
                .tokenId(existing.getTokenId())
                .side(parseSide(existing.getSide()))
                .price(existing.getPrice())
                .size(existing.getSize())
                .usdtAmount(existing.getUsdtAmount())
                .errorMsg(uncertain ? "existing local order has no terminal CLOB result" : existing.getLastError())
                .build();
    }

    private boolean sameIdempotentPayload(
            PredictionPolymarketOrder existing,
            PolymarketPlaceOrderRequest request
    ) {
        PolymarketOrderType orderType =
                request.getOrderType() == null
                        ? PolymarketOrderType.FOK
                        : request.getOrderType();
        return equalsText(existing.getUserId(), request.getUserId())
                && equalsText(existing.getSessionId(), request.getSessionId())
                && equalsText(existing.getMarketSlug(), request.getMarketSlug())
                && equalsText(existing.getDirection(), request.getDirection().name())
                && equalsText(existing.getOrderType(), orderType.name())
                && compareDecimal(existing.getUsdtAmount(), request.getUsdtAmount());
    }

    private void markOrderException(
            PredictionPolymarketOrder orderRecord,
            Exception e
    ) {
        if (orderRecord == null) {
            return;
        }
        orderRecord.setStatus("EXCEPTION");
        orderRecord.setLastError(normalizeError(e));
        polymarketOrderRepository.save(orderRecord);
    }

    private String normalizeError(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "UNKNOWN_ERROR";
        }

        String message =
                e.getMessage();

        String lower =
                message.toLowerCase();

        if (lower.contains("market not found")) {
            return "MARKET_NOT_FOUND: " + message;
        }

        if (lower.contains("allowance")) {
            return "INSUFFICIENT_ALLOWANCE: " + message;
        }

        if (lower.contains("conditional tokens not approved")) {
            return "CONDITIONAL_TOKENS_NOT_APPROVED: " + message;
        }

        if (lower.contains("session expired")) {
            return "SESSION_EXPIRED";
        }

        if (lower.contains("session is not active")) {
            return "SESSION_NOT_ACTIVE";
        }

        if (lower.contains("api key")
                || lower.contains("api secret")
                || lower.contains("api passphrase")) {
            return "CLOB_AUTH_CONFIG_ERROR: " + message;
        }

        return "ORDER_ERROR: " + message;
    }

    /**
     * 官方 CLOB REST 方式要求：
     * 1. order payload 由 signer private key 做 EIP-712 簽名。
     * 2. POST /order 的 L2 API credentials 必須由同一個 signer 派生。
     *
     * 目前先採用後端平台 signer：
     * - polymarket.wallet.private-key 負責簽 CLOB order
     * - polymarket.clob.api-* 必須由這把 private key create/derive
     *
     * SessionRecord 在這個版本只作為你平台內部的交易授權，不直接作為
     * Polymarket order signer。若要改成 per-session signer，必須替每個
     * session signer 派生並保存各自的 CLOB API credentials。
     */
    private String resolveSigningPrivateKey(
            PredictionSessionRecord sessionRecord
    ) {
        String platformPrivateKey =
                polymarketConfigs
                        .getWallet()
                        .getPrivateKey();

        if (platformPrivateKey != null
                && !platformPrivateKey.isBlank()) {
            return platformPrivateKey;
        }

        return sessionRecord.getSessionPrivateKey();
    }

    /**
     * 從 DB market info 組 CLOB order。
     */
    private PolymarketNormalizedClobOrder buildNormalizedOrder(
            PolymarketPlaceOrderRequest request,
            PredictionMarketInfo marketInfo
    ) {
        PolymarketOrderType orderType =
                request.getOrderType() == null
                        ? PolymarketOrderType.FOK
                        : request.getOrderType();

        TokenAndPrice tokenAndPrice =
                resolveTokenAndPrice(
                        request.getDirection(),
                        marketInfo
                );

        BigDecimal size =
                request.getUsdtAmount()
                        .divide(
                                tokenAndPrice.price(),
                                6,
                                RoundingMode.DOWN
                        );

        return PolymarketNormalizedClobOrder.builder()
                .userId(request.getUserId())
                .eventSlug(marketInfo.getEventSlug())
                .marketSlug(marketInfo.getMarketSlug())
                .outcomeKey(marketInfo.getOutcomeKey())
                .tokenId(tokenAndPrice.tokenId())
                .side(tokenAndPrice.side())
                .price(tokenAndPrice.price())
                .size(size)
                .usdtAmount(request.getUsdtAmount())
                .orderType(orderType)

                .negRisk(Boolean.TRUE.equals(marketInfo.getNegRisk()))
                .build();
    }

    /**
     * 根據方向決定 tokenId / side / price。
     */
    private TokenAndPrice resolveTokenAndPrice(
            PolymarketOrderDirection direction,
            PredictionMarketInfo marketInfo
    ) {
        return switch (direction) {
            case BUY_YES -> new TokenAndPrice(
                    marketInfo.getYesTokenId(),
                    PolymarketClobSide.BUY,
                    toBigDecimal(
                            marketInfo.getBestAsk(),
                            "bestAsk"
                    )
            );

            case SELL_YES -> new TokenAndPrice(
                    marketInfo.getYesTokenId(),
                    PolymarketClobSide.SELL,
                    toBigDecimal(
                            marketInfo.getBestBid(),
                            "bestBid"
                    )
            );

            case BUY_NO -> new TokenAndPrice(
                    marketInfo.getNoTokenId(),
                    PolymarketClobSide.BUY,
                    toBigDecimal(
                            marketInfo.getNoBuyPrice(),
                            "noBuyPrice"
                    )
            );

            case SELL_NO -> new TokenAndPrice(
                    marketInfo.getNoTokenId(),
                    PolymarketClobSide.SELL,
                    toBigDecimal(
                            marketInfo.getNoSellPrice(),
                            "noSellPrice"
                    )
            );
        };
    }

    private BigDecimal toBigDecimal(
            Double value,
            String fieldName
    ) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(
                    fieldName + " is empty or invalid"
            );
        }

        return BigDecimal.valueOf(value);
    }

    /**
     * 下單前檢查 allowance / approval。
     *
     * BUY：
     * 需要 Deposit Wallet approve pUSD 給 Exchange / NegRiskExchange。
     *
     * SELL：
     * 需要 Deposit Wallet setApprovalForAll ConditionalTokens。
     *
     * TODO:
     * Deposit Wallet 模式下，approval 正式應走 Polymarket relayer WALLET batch。
     */
    private void checkApprovalBeforeOrder(
            String assetOwner,
            PolymarketNormalizedClobOrder normalizedOrder
    ) {
        if (normalizedOrder.getSide() == PolymarketClobSide.BUY) {
            BigInteger requiredAmountAtomic =
                    toAtomic(
                            normalizedOrder
                                    .getPrice()
                                    .multiply(normalizedOrder.getSize())
                    );

            polymarketApprovalService.requireCollateralAllowance(
                    assetOwner,
                    requiredAmountAtomic
            );

            return;
        }

        polymarketApprovalService.requireConditionalTokensApproval(
                assetOwner
        );
    }

    private BigInteger toAtomic(
            BigDecimal value
    ) {
        return value
                .multiply(ATOMIC_UNIT)
                .toBigInteger();
    }

    private void validate(
            PolymarketPlaceOrderRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "request is required"
            );
        }

        if (request.getUserId() == null
                || request.getUserId().isBlank()) {
            throw new IllegalArgumentException(
                    "userId is required"
            );
        }

        if (request.getSessionId() == null
                || request.getSessionId().isBlank()) {
            throw new IllegalArgumentException(
                    "sessionId is required"
            );
        }

        if (request.getMarketSlug() == null
                || request.getMarketSlug().isBlank()) {
            throw new IllegalArgumentException(
                    "marketSlug is required"
            );
        }

        if (request.getDirection() == null) {
            throw new IllegalArgumentException(
                    "direction is required"
            );
        }

        if (request.getUsdtAmount() == null
                || request.getUsdtAmount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "usdtAmount must be positive"
            );
        }

        if (request.getClientRequestId() != null
                && request.getClientRequestId().trim().length() > 64) {
            throw new IllegalArgumentException(
                    "clientRequestId must be 64 characters or fewer"
            );
        }

        validateClobApiConfig();

        if (polymarketConfigs.getWallet().getFunderAddress() == null
                || polymarketConfigs.getWallet().getFunderAddress().isBlank()) {
            throw new IllegalStateException(
                    "polymarket wallet funder-address is empty"
            );
        }
    }

    private void validateClobApiConfig() {
        if (polymarketConfigs.getClob().getApiKey() == null
                || polymarketConfigs.getClob().getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "polymarket clob api key is empty"
            );
        }

        if (polymarketConfigs.getClob().getApiSecret() == null
                || polymarketConfigs.getClob().getApiSecret().isBlank()) {
            throw new IllegalStateException(
                    "polymarket clob api secret is empty"
            );
        }

        if (polymarketConfigs.getClob().getApiPassphrase() == null
                || polymarketConfigs.getClob().getApiPassphrase().isBlank()) {
            throw new IllegalStateException(
                    "polymarket clob api passphrase is empty"
            );
        }
    }

    private void fillResponse(
            PolymarketPlaceOrderResponse response,
            String internalOrderId,
            PolymarketNormalizedClobOrder normalizedOrder
    ) {
        response.setInternalOrderId(internalOrderId);
        response.setTokenId(normalizedOrder.getTokenId());
        response.setSide(normalizedOrder.getSide());
        response.setPrice(normalizedOrder.getPrice());
        response.setSize(normalizedOrder.getSize());
        response.setUsdtAmount(normalizedOrder.getUsdtAmount());
    }

    private static PolymarketClobSide parseSide(String side) {
        if (side == null || side.isBlank()) {
            return null;
        }
        return PolymarketClobSide.valueOf(side.trim().toUpperCase());
    }

    private static boolean equalsText(String left, String right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private static boolean compareDecimal(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.compareTo(right) == 0;
    }

    private PredictionPolymarketOrder createOrderRecord(
            String internalOrderId,
            PolymarketPlaceOrderRequest request,
            PredictionMarketInfo marketInfo,
            PolymarketNormalizedClobOrder normalizedOrder
    ) {
        PredictionPolymarketOrder entity =
                new PredictionPolymarketOrder();

        entity.setInternalOrderId(internalOrderId);
        entity.setUserId(request.getUserId());
        entity.setSessionId(request.getSessionId());
        entity.setEventSlug(marketInfo.getEventSlug());
        entity.setMarketSlug(marketInfo.getMarketSlug());
        entity.setConditionId(marketInfo.getConditionId());
        entity.setOutcomeKey(marketInfo.getOutcomeKey());
        entity.setTokenId(normalizedOrder.getTokenId());
        entity.setDirection(request.getDirection().name());
        entity.setSide(normalizedOrder.getSide().name());
        entity.setOrderType(normalizedOrder.getOrderType().name());
        entity.setPrice(normalizedOrder.getPrice());
        entity.setSize(normalizedOrder.getSize());
        entity.setUsdtAmount(normalizedOrder.getUsdtAmount());
        entity.setStatus("CREATED");

        return polymarketOrderRepository.save(entity);
    }

    private void updateOrderRecord(
            PredictionPolymarketOrder entity,
            PolymarketPlaceOrderResponse response
    ) {
        entity.setClobOrderId(response.getClobOrderId());
        entity.setStatus(response.getStatus());
        entity.setLastError(response.getErrorMsg());

        polymarketOrderRepository.save(entity);
    }

    private void logStart(
            String internalOrderId,
            PolymarketPlaceOrderRequest request
    ) {
        if (request == null) {
            log.info(
                    "[PolymarketOrder] Start place order. internalOrderId={}, request=null",
                    internalOrderId
            );
            return;
        }

        log.info(
                "[PolymarketOrder] Start place order. internalOrderId={}, userId={}, sessionId={}, marketSlug={}, direction={}, usdtAmount={}, orderType={}",
                internalOrderId,
                request.getUserId(),
                request.getSessionId(),
                request.getMarketSlug(),
                request.getDirection(),
                request.getUsdtAmount(),
                request.getOrderType()
        );
    }

    private void logSignedOrder(
            String internalOrderId,
            PolymarketClobOrderRequest clobOrderRequest
    ) {
        log.info(
                "[PolymarketOrder] Signed CLOB order. internalOrderId={}, owner={}, orderType={}, deferExec={}, maker={}, signer={}, tokenId={}, makerAmount={}, takerAmount={}, side={}, signatureType={}, signaturePrefix={}",
                internalOrderId,
                clobOrderRequest.getOwner(),
                clobOrderRequest.getOrderType(),
                clobOrderRequest.getDeferExec(),
                clobOrderRequest.getOrder().getMaker(),
                clobOrderRequest.getOrder().getSigner(),
                clobOrderRequest.getOrder().getTokenId(),
                clobOrderRequest.getOrder().getMakerAmount(),
                clobOrderRequest.getOrder().getTakerAmount(),
                clobOrderRequest.getOrder().getSide(),
                clobOrderRequest.getOrder().getSignatureType(),
                maskSignature(
                        clobOrderRequest.getOrder().getSignature()
                )
        );
    }

    private void logResult(
            String internalOrderId,
            PolymarketPlaceOrderRequest request,
            PolymarketPlaceOrderResponse response
    ) {
        if (Boolean.TRUE.equals(response.getSuccess())) {
            log.info(
                    "[PolymarketOrder] Place order success. internalOrderId={}, sessionId={}, clobOrderId={}, status={}",
                    internalOrderId,
                    request.getSessionId(),
                    response.getClobOrderId(),
                    response.getStatus()
            );
            return;
        }

        log.warn(
                "[PolymarketOrder] Place order failed. internalOrderId={}, sessionId={}, status={}, errorMsg={}",
                internalOrderId,
                request.getSessionId(),
                response.getStatus(),
                response.getErrorMsg()
        );
    }

    private String maskSignature(
            String signature
    ) {
        if (signature == null || signature.length() < 16) {
            return "EMPTY";
        }

        return signature.substring(0, 10)
                + "..."
                + signature.substring(signature.length() - 6);
    }

    private record TokenAndPrice(
            String tokenId,
            PolymarketClobSide side,
            BigDecimal price
    ) {
    }
}
