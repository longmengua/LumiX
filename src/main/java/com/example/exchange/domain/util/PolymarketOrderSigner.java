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
 * Polymarket CLOB order signer。
 *
 * Deposit Wallet + POLY_1271 架構：
 *
 * 1. maker = Deposit Wallet / funder address
 * 2. signer = EOA address derived from signing private key
 * 3. signatureType = config, new API deposit wallet flow should use POLY_1271 = 3
 * 4. REST /order owner = CLOB apiKey owner UUID
 * 5. L2 API credentials must be derived from the same signer private key
 */
public class PolymarketOrderSigner {

    private static final BigDecimal ATOMIC_UNIT =
            new BigDecimal("1000000");

    private static final String ZERO_BYTES32 =
            "0x0000000000000000000000000000000000000000000000000000000000000000";

    private PolymarketOrderSigner() {
    }

    public static PolymarketClobOrderRequest sign(
            PolymarketConfigs polymarketProperties,
            String signingPrivateKey,
            PolymarketNormalizedClobOrder order
    ) {
        validateInputs(
                polymarketProperties,
                signingPrivateKey,
                order
        );

        Credentials credentials =
                Credentials.create(signingPrivateKey);

        String depositWalletAddress =
                polymarketProperties
                        .getWallet()
                        .getFunderAddress();

        String maker =
                depositWalletAddress;

        String signer =
                credentials.getAddress();

        BigInteger salt =
                new BigInteger(
                        256,
                        new SecureRandom()
                );

        String timestamp =
                String.valueOf(
                        Instant.now().toEpochMilli()
                );

        AmountPair amountPair =
                buildAmountPair(order);

        PolymarketClobOrderRequest.SignedOrder unsignedOrder =
                PolymarketClobOrderRequest.SignedOrder.builder()
                        .salt(salt.toString())
                        .maker(maker)
                        .signer(signer)
                        .tokenId(order.getTokenId())
                        .makerAmount(amountPair.makerAmount().toString())
                        .takerAmount(amountPair.takerAmount().toString())
                        .side(toClobSide(order.getSide()))
                        .expiration("0")
                        .timestamp(timestamp)
                        .metadata(ZERO_BYTES32)
                        .builder(ZERO_BYTES32)

                        .signatureType(resolveSignatureType(polymarketProperties))
                        .build();

        String signature =
                PolymarketEip712Signer.signOrder(
                        polymarketProperties,
                        unsignedOrder,
                        credentials,
                        Boolean.TRUE.equals(order.getNegRisk())
                );

        unsignedOrder.setSignature(signature);

        return PolymarketClobOrderRequest.builder()
                .order(unsignedOrder)
                .owner(
                        polymarketProperties
                                .getClob()
                                .getApiKey()
                )
                .orderType(order.getOrderType().name())
                .deferExec(false)
                .build();
    }

    private static AmountPair buildAmountPair(
            PolymarketNormalizedClobOrder order
    ) {
        BigInteger quoteAmount =
                toAtomic(
                        order.getPrice()
                                .multiply(order.getSize())
                );

        BigInteger shareAmount =
                toAtomic(order.getSize());

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

    private static BigInteger toAtomic(
            BigDecimal value
    ) {
        return value
                .multiply(ATOMIC_UNIT)
                .toBigInteger();
    }

    private static String toClobSide(
            PolymarketClobSide side
    ) {
        return side == PolymarketClobSide.BUY
                ? "BUY"
                : "SELL";
    }

    private static void validateInputs(
            PolymarketConfigs polymarketProperties,
            String signingPrivateKey,
            PolymarketNormalizedClobOrder order
    ) {
        if (polymarketProperties == null) {
            throw new IllegalArgumentException(
                    "polymarketProperties is required"
            );
        }

        if (signingPrivateKey == null
                || signingPrivateKey.isBlank()) {
            throw new IllegalArgumentException(
                    "signingPrivateKey is required"
            );
        }

        if (polymarketProperties.getWallet().getFunderAddress() == null
                || polymarketProperties.getWallet().getFunderAddress().isBlank()) {
            throw new IllegalArgumentException(
                    "wallet funderAddress is required"
            );
        }

        if (polymarketProperties.getClob().getApiKey() == null
                || polymarketProperties.getClob().getApiKey().isBlank()) {
            throw new IllegalArgumentException(
                    "clob apiKey is required"
            );
        }

        if (order == null) {
            throw new IllegalArgumentException(
                    "order is required"
            );
        }

        if (order.getTokenId() == null
                || order.getTokenId().isBlank()) {
            throw new IllegalArgumentException(
                    "tokenId is required"
            );
        }

        if (order.getSide() == null) {
            throw new IllegalArgumentException(
                    "side is required"
            );
        }

        if (order.getPrice() == null
                || order.getPrice().signum() <= 0) {
            throw new IllegalArgumentException(
                    "price must be positive"
            );
        }

        if (order.getSize() == null
                || order.getSize().signum() <= 0) {
            throw new IllegalArgumentException(
                    "size must be positive"
            );
        }

        if (order.getOrderType() == null) {
            throw new IllegalArgumentException(
                    "orderType is required"
            );
        }
    }

    private static int resolveSignatureType(
            PolymarketConfigs polymarketProperties
    ) {
        Integer signatureType =
                polymarketProperties.getWallet().getSignatureType();

        return signatureType == null ? 3 : signatureType;
    }

    private record AmountPair(
            BigInteger makerAmount,
            BigInteger takerAmount
    ) {
    }
}
