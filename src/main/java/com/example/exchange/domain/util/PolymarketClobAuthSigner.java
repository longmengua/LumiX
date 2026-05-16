package com.example.exchange.domain.util;

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

/**
 * Polymarket CLOB L1 auth signer.
 *
 * Used for:
 * - POST /auth/api-key
 * - GET /auth/derive-api-key
 */
public class PolymarketClobAuthSigner {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private static final String AUTH_MESSAGE =
            "This message attests that I control the given wallet";

    private PolymarketClobAuthSigner() {
    }

    public static String sign(
            PolymarketConfigs configs,
            Credentials credentials,
            String timestamp,
            BigInteger nonce
    ) {
        try {
            Map<String, Object> typedData =
                    buildTypedData(
                            configs,
                            credentials.getAddress(),
                            timestamp,
                            nonce
                    );

            String json =
                    OBJECT_MAPPER.writeValueAsString(typedData);

            StructuredDataEncoder encoder =
                    new StructuredDataEncoder(json);

            byte[] hash =
                    encoder.hashStructuredData();

            Sign.SignatureData signatureData =
                    Sign.signMessage(
                            hash,
                            credentials.getEcKeyPair(),
                            false
                    );

            return toSignatureHex(signatureData);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Polymarket CLOB auth signing failed",
                    e
            );
        }
    }

    private static Map<String, Object> buildTypedData(
            PolymarketConfigs configs,
            String signingAddress,
            String timestamp,
            BigInteger nonce
    ) {
        Map<String, Object> domain =
                new LinkedHashMap<>();

        domain.put("name", "ClobAuthDomain");
        domain.put("version", "1");
        domain.put("chainId", configs.getChain().getChainId());

        Map<String, Object> message =
                new LinkedHashMap<>();

        message.put("address", signingAddress);
        message.put("timestamp", timestamp);
        message.put("nonce", nonce);
        message.put("message", AUTH_MESSAGE);

        Map<String, Object> types =
                new LinkedHashMap<>();

        types.put(
                "EIP712Domain",
                List.of(
                        field("name", "string"),
                        field("version", "string"),
                        field("chainId", "uint256")
                )
        );

        types.put(
                "ClobAuth",
                List.of(
                        field("address", "address"),
                        field("timestamp", "string"),
                        field("nonce", "uint256"),
                        field("message", "string")
                )
        );

        Map<String, Object> typedData =
                new LinkedHashMap<>();

        typedData.put("types", types);
        typedData.put("primaryType", "ClobAuth");
        typedData.put("domain", domain);
        typedData.put("message", message);

        return typedData;
    }

    private static Map<String, String> field(
            String name,
            String type
    ) {
        Map<String, String> field =
                new LinkedHashMap<>();

        field.put("name", name);
        field.put("type", type);

        return field;
    }

    private static String toSignatureHex(
            Sign.SignatureData signatureData
    ) {
        String r =
                Numeric.toHexStringNoPrefixZeroPadded(
                        new BigInteger(1, signatureData.getR()),
                        64
                );

        String s =
                Numeric.toHexStringNoPrefixZeroPadded(
                        new BigInteger(1, signatureData.getS()),
                        64
                );

        int v =
                signatureData.getV()[0] & 0xFF;

        if (v < 27) {
            v += 27;
        }

        String vHex =
                Integer.toHexString(v);

        if (vHex.length() == 1) {
            vHex = "0" + vHex;
        }

        return "0x" + r + s + vHex;
    }
}
