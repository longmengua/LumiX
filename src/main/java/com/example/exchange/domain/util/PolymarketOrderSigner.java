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

public class PolymarketOrderSigner {

    private static final BigDecimal ATOMIC_UNIT = new BigDecimal("1000000");
    private static final String ZERO_BYTES32 =
            "0x0000000000000000000000000000000000000000000000000000000000000000";

    public static PolymarketClobOrderRequest sign(PolymarketConfigs polymarketProperties, PolymarketNormalizedClobOrder order) {
        Credentials credentials = Credentials.create(
                polymarketProperties.getWallet().getPrivateKey()
        );

        String maker = credentials.getAddress();
        String signer = credentials.getAddress();

        BigInteger salt = new BigInteger(256, new SecureRandom());
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
                        .side(toClobSide(order.getSide()))
                        .expiration("0")
                        .timestamp(timestamp)
                        .metadata(ZERO_BYTES32)
                        .builder(ZERO_BYTES32)
                        .signatureType(polymarketProperties.getWallet().getSignatureType())
                        .build();

//        String signature = signTypedDataV2(unsignedOrder, credentials);
        String signature = PolymarketEip712Signer.signOrder(
                polymarketProperties,
                unsignedOrder,
                credentials,
                Boolean.TRUE.equals(order.getNegRisk())
        );

        unsignedOrder.setSignature(signature);

        return PolymarketClobOrderRequest.builder()
                .order(unsignedOrder)
                .owner(polymarketProperties.getClob().getApiKey())
                .orderType(order.getOrderType().name())
                .deferExec(false)
                .build();
    }

    private static AmountPair buildAmountPair(PolymarketNormalizedClobOrder order) {
        BigInteger quoteAmount = toAtomic(order.getPrice().multiply(order.getSize()));
        BigInteger shareAmount = toAtomic(order.getSize());

        if (order.getSide() == PolymarketClobSide.BUY) {
            return new AmountPair(quoteAmount, shareAmount);
        }

        return new AmountPair(shareAmount, quoteAmount);
    }

    private static BigInteger toAtomic(BigDecimal value) {
        return value
                .multiply(ATOMIC_UNIT)
                .toBigInteger();
    }

    private static String toClobSideNumber(PolymarketClobSide side) {
        return side == PolymarketClobSide.BUY ? "0" : "1";
    }

    private static String toClobSide(PolymarketClobSide side) {
        return side == PolymarketClobSide.BUY ? "BUY" : "SELL";
    }

    /**
     * TODO:
     * 這裡要補真正 CLOB V2 EIP-712 簽名。
     *
     * 目前先讓你整條 Java 流程串起來：
     *
     * Request
     * -> Mapper
     * -> NormalizedClobOrder
     * -> Signer
     * -> ClobTradingClient
     * -> POST /order
     *
     * 下一步我再給你專門的 EIP712 signing helper。
     */
    private static String signTypedDataV2(
            PolymarketClobOrderRequest.SignedOrder order,
            Credentials credentials
    ) {
        return "TODO_EIP712_SIGNATURE";
    }

    private record AmountPair(
            BigInteger makerAmount,
            BigInteger takerAmount
    ) {
    }
}
