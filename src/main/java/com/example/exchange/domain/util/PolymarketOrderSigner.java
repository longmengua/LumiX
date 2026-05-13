package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.PolymarketClobOrderRequest;
import com.example.exchange.domain.model.dto.PolymarketNormalizedClobOrder;
import com.example.exchange.domain.model.enums.PolymarketClobSide;
import com.example.exchange.infra.config.PolymarketConfigs;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Polymarket CLOB order signer.
 *
 * 職責：
 * 1. 將 NormalizedClobOrder 轉成 Polymarket CLOB SignedOrder
 * 2. 根據 BUY / SELL 計算 makerAmount / takerAmount
 * 3. 使用 session signer private key 做 EIP-712 簽名
 * 4. 組成 POST /order request body
 *
 * 正式方向：
 * - 不再使用 config 裡固定 wallet.privateKey
 * - 改用 session signer private key
 * - 每個使用者 / session 可以有自己的 signer
 *
 * 注意：
 * sessionPrivateKey 應由 PolymarketSessionService 從 ACTIVE session 取得。
 */
public class PolymarketOrderSigner {

    /**
     * Polymarket price / size atomic unit。
     *
     * CLOB 金額使用 6 decimals。
     */
    private static final BigDecimal ATOMIC_UNIT = new BigDecimal("1000000");

    /**
     * bytes32 zero。
     *
     * metadata 目前沒有額外資料時使用。
     */
    private static final String ZERO_BYTES32 =
            "0x0000000000000000000000000000000000000000000000000000000000000000";

    private PolymarketOrderSigner() {
    }

    /**
     * 使用 session signer private key 簽 Polymarket CLOB order。
     *
     * @param polymarketProperties Polymarket config
     * @param sessionPrivateKey    ACTIVE session signer private key
     * @param order                標準化後的 CLOB order
     * @return Polymarket POST /order request
     */
    public static PolymarketClobOrderRequest sign(
            PolymarketConfigs polymarketProperties,
            String sessionPrivateKey,
            PolymarketNormalizedClobOrder order
    ) {
        validateInputs(polymarketProperties, sessionPrivateKey, order);

        Credentials credentials = Credentials.create(sessionPrivateKey);

        /**
         * EOA / session signer address。
         *
         * 目前 EOA POC / session signer POC：
         * maker = signer = session signer address
         *
         * TODO:
         * 如果切到 Deposit Wallet / POLY_1271：
         * maker / signer 可能要改成 depositWalletAddress，
         * signatureType 也要改成 3。
         */
        String maker = credentials.getAddress();
        String signer = credentials.getAddress();

        BigInteger salt = new BigInteger(256, new SecureRandom());

        /**
         * CLOB V2 signed order 使用 timestamp。
         */
        String timestamp = String.valueOf(Instant.now().toEpochMilli());

        AmountPair amountPair = buildAmountPair(order);

        PolymarketClobOrderRequest.SignedOrder unsignedOrder =
                PolymarketClobOrderRequest.SignedOrder.builder()
                        .salt(salt.toString())
                        .maker(maker)
                        .signer(signer)
                        .tokenId(order.getTokenId())
                        .makerAmount(amountPair.makerAmount().toString())
                        .takerAmount(amountPair.takerAmount().toString())

                        /**
                         * POST /order body 使用 BUY / SELL。
                         * EIP-712 signer 內部會轉成 0 / 1。
                         */
                        .side(toClobSide(order.getSide()))

                        /**
                         * 0 = no expiration。
                         */
                        .expiration("0")
                        .timestamp(timestamp)
                        .metadata(ZERO_BYTES32)

                        /**
                         * builder attribution。
                         *
                         * TODO:
                         * 如果你有 Polymarket Builder Code，
                         * 這裡改成 polymarketProperties.getBuilder().getBuilderCode()
                         */
                        .builder(ZERO_BYTES32)

                        /**
                         * EOA = 0。
                         *
                         * TODO:
                         * Deposit Wallet / POLY_1271 = 3。
                         */
                        .signatureType(polymarketProperties.getWallet().getSignatureType())
                        .build();

        /**
         * 真正 EIP-712 簽名。
         */
        String signature = PolymarketEip712Signer.signOrder(
                polymarketProperties,
                unsignedOrder,
                credentials,
                Boolean.TRUE.equals(order.getNegRisk())
        );

        unsignedOrder.setSignature(signature);

        return PolymarketClobOrderRequest.builder()
                .order(unsignedOrder)

                /**
                 * TODO:
                 * 目前沿用你原本寫法：owner = apiKey。
                 *
                 * 若實測 CLOB 回 owner/funder 相關錯誤，
                 * 這裡需要依官方 schema 調整成 funderAddress / wallet address。
                 */
                .owner(polymarketProperties.getClob().getApiKey())
                .orderType(order.getOrderType().name())
                .deferExec(false)
                .build();
    }

    /**
     * BUY / SELL amount mapping。
     *
     * BUY：
     * - makerAmount = quote amount，也就是支付 USDC
     * - takerAmount = share amount，也就是取得 shares
     *
     * SELL：
     * - makerAmount = share amount，也就是賣出 shares
     * - takerAmount = quote amount，也就是取得 USDC
     */
    private static AmountPair buildAmountPair(
            PolymarketNormalizedClobOrder order
    ) {
        BigInteger quoteAmount = toAtomic(
                order.getPrice().multiply(order.getSize())
        );

        BigInteger shareAmount = toAtomic(
                order.getSize()
        );

        if (order.getSide() == PolymarketClobSide.BUY) {
            return new AmountPair(
                    quoteAmount,
                    shareAmount
            );
        }

        return new AmountPair(
                shareAmount,
                quoteAmount
        );
    }

    /**
     * 轉 6 decimals atomic amount。
     */
    private static BigInteger toAtomic(BigDecimal value) {
        return value
                .multiply(ATOMIC_UNIT)
                .toBigInteger();
    }

    /**
     * CLOB body 使用 BUY / SELL。
     */
    private static String toClobSide(
            PolymarketClobSide side
    ) {
        return side == PolymarketClobSide.BUY
                ? "BUY"
                : "SELL";
    }

    /**
     * 基礎防呆。
     */
    private static void validateInputs(
            PolymarketConfigs polymarketProperties,
            String sessionPrivateKey,
            PolymarketNormalizedClobOrder order
    ) {
        if (polymarketProperties == null) {
            throw new IllegalArgumentException("polymarketProperties is required");
        }

        if (sessionPrivateKey == null || sessionPrivateKey.isBlank()) {
            throw new IllegalArgumentException("sessionPrivateKey is required");
        }

        if (order == null) {
            throw new IllegalArgumentException("order is required");
        }

        if (order.getTokenId() == null || order.getTokenId().isBlank()) {
            throw new IllegalArgumentException("tokenId is required");
        }

        if (order.getSide() == null) {
            throw new IllegalArgumentException("side is required");
        }

        if (order.getPrice() == null || order.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }

        if (order.getSize() == null || order.getSize().signum() <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        if (order.getOrderType() == null) {
            throw new IllegalArgumentException("orderType is required");
        }
    }

    private record AmountPair(
            BigInteger makerAmount,
            BigInteger takerAmount
    ) {
    }
}