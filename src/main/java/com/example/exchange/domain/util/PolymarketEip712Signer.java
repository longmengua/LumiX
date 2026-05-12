package com.example.exchange.domain.util;

import com.example.exchange.domain.model.dto.PolymarketClobOrderRequest;
import com.example.exchange.infra.config.PolymarketConfigs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PolymarketEip712Signer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PolymarketEip712Signer() {
    }

    public static String signOrder(
            PolymarketConfigs configs,
            PolymarketClobOrderRequest.SignedOrder order,
            Credentials credentials,
            boolean negRisk
    ) {
        try {
            String verifyingContract = negRisk
                    ? configs.getChain().getNegRiskExchangeV2()
                    : configs.getChain().getExchangeV2();

            Map<String, Object> typedData = buildTypedData(
                    configs,
                    order,
                    verifyingContract
            );

            String json = OBJECT_MAPPER.writeValueAsString(typedData);

            StructuredDataEncoder encoder = new StructuredDataEncoder(json);
            byte[] hash = encoder.hashStructuredData();

            Sign.SignatureData signatureData = Sign.signMessage(
                    hash,
                    credentials.getEcKeyPair(),
                    false
            );

            return toSignatureHex(signatureData);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Polymarket EIP-712 order signing failed",
                    e
            );
        }
    }

    private static Map<String, Object> buildTypedData(
            PolymarketConfigs configs,
            PolymarketClobOrderRequest.SignedOrder order,
            String verifyingContract
    ) {
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("name", "Polymarket CTF Exchange");
        domain.put("version", "2");
        domain.put("chainId", configs.getChain().getChainId());
        domain.put("verifyingContract", verifyingContract);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("salt", order.getSalt());
        message.put("maker", order.getMaker());
        message.put("signer", order.getSigner());
        message.put("tokenId", order.getTokenId());
        message.put("makerAmount", order.getMakerAmount());
        message.put("takerAmount", order.getTakerAmount());
        message.put("side", toSideNumber(order.getSide()));
        message.put("signatureType", order.getSignatureType());
        message.put("timestamp", order.getTimestamp());
        message.put("metadata", order.getMetadata());
        message.put("builder", order.getBuilder());

        Map<String, Object> types = new LinkedHashMap<>();
        types.put("EIP712Domain", List.of(
                field("name", "string"),
                field("version", "string"),
                field("chainId", "uint256"),
                field("verifyingContract", "address")
        ));
        types.put("Order", List.of(
                field("salt", "uint256"),
                field("maker", "address"),
                field("signer", "address"),
                field("tokenId", "uint256"),
                field("makerAmount", "uint256"),
                field("takerAmount", "uint256"),
                field("side", "uint8"),
                field("signatureType", "uint8"),
                field("timestamp", "uint256"),
                field("metadata", "bytes32"),
                field("builder", "bytes32")
        ));

        Map<String, Object> typedData = new LinkedHashMap<>();
        typedData.put("types", types);
        typedData.put("primaryType", "Order");
        typedData.put("domain", domain);
        typedData.put("message", message);

        return typedData;
    }

    private static Map<String, String> field(String name, String type) {
        Map<String, String> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("type", type);
        return field;
    }

    private static int toSideNumber(String side) {
        if ("BUY".equalsIgnoreCase(side)) {
            return 0;
        }

        if ("SELL".equalsIgnoreCase(side)) {
            return 1;
        }

        throw new IllegalArgumentException("Unsupported CLOB side: " + side);
    }

    private static String toSignatureHex(Sign.SignatureData signatureData) {
        String r = Numeric.toHexStringNoPrefixZeroPadded(
                new BigInteger(1, signatureData.getR()),
                64
        );

        String s = Numeric.toHexStringNoPrefixZeroPadded(
                new BigInteger(1, signatureData.getS()),
                64
        );

        int v = signatureData.getV()[0] & 0xFF;

        if (v < 27) {
            v += 27;
        }

        String vHex = Integer.toHexString(v);
        if (vHex.length() == 1) {
            vHex = "0" + vHex;
        }

        return "0x" + r + s + vHex;
    }
}