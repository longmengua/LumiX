package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.*;
import com.example.exchange.domain.model.entity.PredictionSessionRecord;
import com.example.exchange.domain.model.enums.PolymarketClobSide;
import com.example.exchange.domain.util.PolymarketOrderMapper;
import com.example.exchange.domain.util.PolymarketOrderSigner;
import com.example.exchange.infra.config.PolymarketConfigs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

/**
 * Polymarket order service。
 *
 * 正式架構：
 *
 * Deposit Wallet：
 * - 真正持有資產
 * - 真正做 ERC20 / ERC1155 approve
 *
 * Session Signer：
 * - 只負責簽 CLOB order
 * - 不持有資產
 * - 不做鏈上 approve
 *
 * 流程：
 * 1. 前端 MetaMask connect
 * 2. 建立 session signer
 * 3. EIP712 confirm session
 * 4. Deposit wallet 前端自行 approve
 * 5. placeOrder
 * 6. 後端驗證 allowance / approval
 * 7. session signer 簽 CLOB order
 * 8. POST Polymarket CLOB /order
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketOrderService {

    /**
     * USDC 6 decimals。
     */
    private static final BigDecimal ATOMIC_UNIT =
            new BigDecimal("1000000");

    private final PolymarketConfigs polymarketConfigs;

    private final PolymarketClobTradingClient polymarketClobTradingClient;

    private final PolymarketSessionService polymarketSessionService;

    private final PolymarketApprovalService polymarketApprovalService;

    /**
     * 建立真實 Polymarket CLOB order。
     */
    public PolymarketPlaceOrderResponse placeOrder(
            PolymarketPlaceOrderRequest request
    ) {
        String internalOrderId =
                UUID.randomUUID().toString();

        logStart(
                internalOrderId,
                request
        );

        try {
            validate(request);

            /**
             * ACTIVE session。
             */
            PredictionSessionRecord sessionRecord =
                    polymarketSessionService.getActiveSession(
                            request.getSessionId()
                    );

            /**
             * 真正持有資產的 wallet。
             *
             * 正式架構：
             * Deposit Wallet 持有資產
             * Session Signer 只負責簽單
             *
             * TODO:
             * 後續如果：
             * user wallet
             * deposit wallet
             * session signer
             * 分離，
             * 這裡應該改成：
             *
             * sessionRecord.getDepositWalletAddress()
             */
            String assetOwner =
                    sessionRecord.getUserAddress();

            log.info(
                    "[PolymarketOrder] Active session loaded. internalOrderId={}, sessionId={}, userAddress={}, sessionSignerAddress={}, assetOwner={}",
                    internalOrderId,
                    sessionRecord.getSessionId(),
                    sessionRecord.getUserAddress(),
                    sessionRecord.getSessionSignerAddress(),
                    assetOwner
            );

            /**
             * 前端 request
             * -> 標準化 CLOB order
             */
            PolymarketNormalizedClobOrder normalizedOrder =
                    PolymarketOrderMapper.toClobOrder(
                            request
                    );

            log.info(
                    "[PolymarketOrder] Normalized order. internalOrderId={}, tokenId={}, side={}, price={}, size={}, usdtAmount={}, orderType={}, negRisk={}",
                    internalOrderId,
                    normalizedOrder.getTokenId(),
                    normalizedOrder.getSide(),
                    normalizedOrder.getPrice(),
                    normalizedOrder.getSize(),
                    normalizedOrder.getUsdtAmount(),
                    normalizedOrder.getOrderType(),
                    normalizedOrder.getNegRisk()
            );

            /**
             * 下單前驗證：
             * Deposit Wallet allowance / approval。
             */
            checkApprovalBeforeOrder(
                    assetOwner,
                    normalizedOrder
            );

            /**
             * Session signer 簽 CLOB order。
             *
             * 注意：
             * signer 不一定是資產 owner。
             */
            PolymarketClobOrderRequest clobOrderRequest =
                    PolymarketOrderSigner.sign(
                            polymarketConfigs,
                            sessionRecord.getSessionPrivateKey(),
                            normalizedOrder
                    );

            logSignedOrder(
                    internalOrderId,
                    clobOrderRequest
            );

            /**
             * 真正送 Polymarket CLOB。
             */
            PolymarketPlaceOrderResponse response =
                    polymarketClobTradingClient.postOrder(
                            clobOrderRequest
                    );

            fillResponse(
                    response,
                    internalOrderId,
                    normalizedOrder
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

            return PolymarketPlaceOrderResponse.builder()
                    .success(false)
                    .internalOrderId(internalOrderId)
                    .status("EXCEPTION")
                    .errorMsg(e.getMessage())
                    .build();
        }
    }

    /**
     * 下單前檢查 allowance / approval。
     *
     * BUY：
     * ERC20 allowance
     *
     * SELL：
     * ERC1155 approval
     */
    private void checkApprovalBeforeOrder(
            String assetOwner,
            PolymarketNormalizedClobOrder normalizedOrder
    ) {

        /**
         * BUY：
         * 需要 ERC20 allowance。
         */
        if (normalizedOrder.getSide() == PolymarketClobSide.BUY) {

            BigInteger requiredAmountAtomic =
                    toAtomic(
                            normalizedOrder.getPrice()
                                    .multiply(normalizedOrder.getSize())
                    );

            log.info(
                    "[PolymarketOrder] Check collateral allowance. assetOwner={}, tokenId={}, requiredAmountAtomic={}",
                    assetOwner,
                    normalizedOrder.getTokenId(),
                    requiredAmountAtomic
            );

            polymarketApprovalService.requireCollateralAllowance(
                    assetOwner,
                    requiredAmountAtomic
            );

            return;
        }

        /**
         * SELL：
         * 需要 ERC1155 approval。
         */
        log.info(
                "[PolymarketOrder] Check conditional token approval. assetOwner={}, tokenId={}",
                assetOwner,
                normalizedOrder.getTokenId()
        );

        polymarketApprovalService.requireConditionalTokensApproval(
                assetOwner
        );
    }

    /**
     * 轉 atomic amount。
     */
    private BigInteger toAtomic(
            BigDecimal value
    ) {
        return value
                .multiply(ATOMIC_UNIT)
                .toBigInteger();
    }

    /**
     * 基礎 request validation。
     */
    private void validate(
            PolymarketPlaceOrderRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "request is required"
            );
        }

        if (request.getSessionId() == null
                || request.getSessionId().isBlank()) {

            throw new IllegalArgumentException(
                    "sessionId is required"
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

        if (request.getEventSlug() == null
                || request.getEventSlug().isBlank()) {

            throw new IllegalArgumentException(
                    "eventSlug is required"
            );
        }

        if (request.getMarketSlug() == null
                || request.getMarketSlug().isBlank()) {

            throw new IllegalArgumentException(
                    "marketSlug is required"
            );
        }

        if (request.getOutcomeKey() == null
                || request.getOutcomeKey().isBlank()) {

            throw new IllegalArgumentException(
                    "outcomeKey is required"
            );
        }

        if (request.getYesTokenId() == null
                || request.getYesTokenId().isBlank()) {

            throw new IllegalArgumentException(
                    "yesTokenId is required"
            );
        }

        if (request.getNoTokenId() == null
                || request.getNoTokenId().isBlank()) {

            throw new IllegalArgumentException(
                    "noTokenId is required"
            );
        }

        if (request.getYesBuyPrice() == null) {
            throw new IllegalArgumentException(
                    "yesBuyPrice is required"
            );
        }

        if (request.getYesSellPrice() == null) {
            throw new IllegalArgumentException(
                    "yesSellPrice is required"
            );
        }

        if (request.getNoBuyPrice() == null) {
            throw new IllegalArgumentException(
                    "noBuyPrice is required"
            );
        }

        if (request.getNoSellPrice() == null) {
            throw new IllegalArgumentException(
                    "noSellPrice is required"
            );
        }

        validateClobApiConfig();
    }

    /**
     * 驗證 CLOB API config。
     */
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

    /**
     * 回填 response。
     */
    private void fillResponse(
            PolymarketPlaceOrderResponse response,
            String internalOrderId,
            PolymarketNormalizedClobOrder normalizedOrder
    ) {

        response.setInternalOrderId(
                internalOrderId
        );

        response.setTokenId(
                normalizedOrder.getTokenId()
        );

        response.setSide(
                normalizedOrder.getSide()
        );

        response.setPrice(
                normalizedOrder.getPrice()
        );

        response.setSize(
                normalizedOrder.getSize()
        );

        response.setUsdtAmount(
                normalizedOrder.getUsdtAmount()
        );
    }

    /**
     * Start log。
     */
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
                "[PolymarketOrder] Start place order. internalOrderId={}, userId={}, sessionId={}, eventSlug={}, marketSlug={}, outcomeKey={}, direction={}, usdtAmount={}, orderType={}",
                internalOrderId,
                request.getUserId(),
                request.getSessionId(),
                request.getEventSlug(),
                request.getMarketSlug(),
                request.getOutcomeKey(),
                request.getDirection(),
                request.getUsdtAmount(),
                request.getOrderType()
        );
    }

    /**
     * Signed order log。
     */
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

    /**
     * Result log。
     */
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

    /**
     * 避免 log 完整 signature。
     */
    private String maskSignature(
            String signature
    ) {

        if (signature == null
                || signature.length() < 16) {

            return "EMPTY";
        }

        return signature.substring(0, 10)
                + "..."
                + signature.substring(signature.length() - 6);
    }
}