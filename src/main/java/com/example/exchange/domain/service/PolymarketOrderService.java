package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.*;
import com.example.exchange.domain.util.PolymarketOrderMapper;
import com.example.exchange.domain.util.PolymarketOrderSigner;
import com.example.exchange.infra.config.PolymarketConfigs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolymarketOrderService {

    private final PolymarketConfigs polymarketConfigs;
    private final PolymarketClobTradingClient polymarketClobTradingClient;

    public PolymarketPlaceOrderResponse placeOrder(PolymarketPlaceOrderRequest request) {
        String internalOrderId = UUID.randomUUID().toString();

        log.info("[PolymarketOrder] Start place order. internalOrderId={}, userId={}, eventSlug={}, marketSlug={}, outcomeKey={}, direction={}, usdtAmount={}, orderType={}",
                internalOrderId,
                request.getUserId(),
                request.getEventSlug(),
                request.getMarketSlug(),
                request.getOutcomeKey(),
                request.getDirection(),
                request.getUsdtAmount(),
                request.getOrderType()
        );

        try {
            validate(request);

            PolymarketNormalizedClobOrder normalizedOrder =
                    PolymarketOrderMapper.toClobOrder(request);

            log.info("[PolymarketOrder] Normalized order. internalOrderId={}, tokenId={}, side={}, price={}, size={}, usdtAmount={}, orderType={}, negRisk={}",
                    internalOrderId,
                    normalizedOrder.getTokenId(),
                    normalizedOrder.getSide(),
                    normalizedOrder.getPrice(),
                    normalizedOrder.getSize(),
                    normalizedOrder.getUsdtAmount(),
                    normalizedOrder.getOrderType(),
                    normalizedOrder.getNegRisk()
            );

            PolymarketClobOrderRequest clobOrderRequest =
                    PolymarketOrderSigner.sign(polymarketConfigs, normalizedOrder);

            log.info("[PolymarketOrder] Signed CLOB order. internalOrderId={}, owner={}, orderType={}, deferExec={}, maker={}, signer={}, tokenId={}, makerAmount={}, takerAmount={}, side={}, signatureType={}, signaturePrefix={}",
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
                    maskSignature(clobOrderRequest.getOrder().getSignature())
            );

            PolymarketPlaceOrderResponse response =
                    polymarketClobTradingClient.postOrder(clobOrderRequest);

            response.setInternalOrderId(internalOrderId);
            response.setTokenId(normalizedOrder.getTokenId());
            response.setSide(normalizedOrder.getSide());
            response.setPrice(normalizedOrder.getPrice());
            response.setSize(normalizedOrder.getSize());
            response.setUsdtAmount(normalizedOrder.getUsdtAmount());

            if (Boolean.TRUE.equals(response.getSuccess())) {
                log.info("[PolymarketOrder] Place order success. internalOrderId={}, clobOrderId={}, status={}",
                        internalOrderId,
                        response.getClobOrderId(),
                        response.getStatus()
                );
            } else {
                log.warn("[PolymarketOrder] Place order failed. internalOrderId={}, status={}, errorMsg={}",
                        internalOrderId,
                        response.getStatus(),
                        response.getErrorMsg()
                );
            }

            return response;
        } catch (Exception e) {
            log.error("[PolymarketOrder] Place order exception. internalOrderId={}, userId={}, marketSlug={}, direction={}",
                    internalOrderId,
                    request.getUserId(),
                    request.getMarketSlug(),
                    request.getDirection(),
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

    private void validate(PolymarketPlaceOrderRequest request) {
        if (request.getDirection() == null) {
            throw new IllegalArgumentException("direction is required");
        }

        if (request.getUsdtAmount() == null || request.getUsdtAmount().signum() <= 0) {
            throw new IllegalArgumentException("usdtAmount must be positive");
        }

        if (polymarketConfigs.getWallet().getPrivateKey() == null
                || polymarketConfigs.getWallet().getPrivateKey().isBlank()) {
            throw new IllegalStateException("polymarket wallet private key is empty");
        }

        if (polymarketConfigs.getClob().getApiKey() == null
                || polymarketConfigs.getClob().getApiKey().isBlank()) {
            throw new IllegalStateException("polymarket clob api key is empty");
        }

        if (polymarketConfigs.getClob().getApiSecret() == null
                || polymarketConfigs.getClob().getApiSecret().isBlank()) {
            throw new IllegalStateException("polymarket clob api secret is empty");
        }

        if (polymarketConfigs.getClob().getApiPassphrase() == null
                || polymarketConfigs.getClob().getApiPassphrase().isBlank()) {
            throw new IllegalStateException("polymarket clob api passphrase is empty");
        }
    }

    private String maskSignature(String signature) {
        if (signature == null || signature.length() < 16) {
            return "EMPTY";
        }
        return signature.substring(0, 10) + "..." + signature.substring(signature.length() - 6);
    }
}