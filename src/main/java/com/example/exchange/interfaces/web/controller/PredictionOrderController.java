/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.dto.PolymarketPlaceOrderRequest;
import com.example.exchange.domain.model.dto.PolymarketPlaceOrderResponse;
import com.example.exchange.domain.model.dto.PolymarketApiCredentialsResponse;
import com.example.exchange.domain.model.dto.PolymarketUserWsStatusResponse;
import com.example.exchange.domain.model.dto.PredictionPriceRefreshResult;
import com.example.exchange.domain.model.dto.PredictionSyncResult;
import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.service.PolymarketApprovalService;
import com.example.exchange.domain.service.PolymarketClobAuthService;
import com.example.exchange.domain.service.PolymarketDiscoveryService;
import com.example.exchange.domain.service.PolymarketMarketService;
import com.example.exchange.domain.service.PolymarketOrderService;
import com.example.exchange.domain.service.PolymarketOrderTrackingService;
import com.example.exchange.domain.service.PolymarketPriceService;
import com.example.exchange.domain.service.PolymarketSessionService;
import com.example.exchange.domain.service.PolymarketSyncService;
import com.example.exchange.domain.service.PolymarketUserWebSocketService;
import com.example.exchange.interfaces.web.dto.PredictionMarketResponse;
import com.example.exchange.interfaces.web.dto.SessionConfirmRequest;
import com.example.exchange.interfaces.web.dto.SessionConfirmResponse;
import com.example.exchange.interfaces.web.dto.SessionInitRequest;
import com.example.exchange.interfaces.web.dto.SessionInitResponse;
import com.example.exchange.interfaces.web.dto.SessionListResponse;
import com.example.exchange.interfaces.web.dto.SessionRevokeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Prediction Market API Controller。
 *
 * 正式架構：
 *
 * Deposit Wallet：
 * - 真正持有資產
 * - 前端 MetaMask 自行 approve
 *
 * Session Signer：
 * - 後端生成
 * - 只負責簽 CLOB order
 * - 不持有資產
 * - 可 revoke / expire
 *
 * 後端不代送 approve tx。
 */
@RestController
@RequestMapping("/api/prediction")
@RequiredArgsConstructor
public class PredictionOrderController {

    private final PolymarketMarketService predictionMarketService;

    private final PolymarketDiscoveryService predictionMarketDiscoveryService;

    private final PolymarketSyncService predictionMarketFullSyncService;

    private final PolymarketPriceService predictionMarketPriceRefreshService;

    private final PolymarketOrderService polymarketOrderService;

    private final PolymarketClobAuthService polymarketClobAuthService;

    private final PolymarketApprovalService polymarketApprovalService;

    private final PolymarketSessionService polymarketSessionService;

    private final PolymarketUserWebSocketService polymarketUserWebSocketService;

    private final PolymarketOrderTrackingService polymarketOrderTrackingService;

    /**
     * POST /api/prediction/markets/discover
     */
    @PostMapping("/markets/discover")
    public String discoverMarkets() {
        return predictionMarketDiscoveryService.discoverWorldCupMarkets();
    }

    /**
     * POST /api/prediction/markets/sync
     */
    @PostMapping("/markets/sync")
    public PredictionSyncResult syncMarkets() {
        return predictionMarketFullSyncService.syncResume();
    }

    /**
     * POST /api/prediction/markets/sync-reset
     */
    @PostMapping("/markets/sync-reset")
    public PredictionSyncResult resetAndSyncMarkets() {
        return predictionMarketFullSyncService.resetAndSync();
    }

    /**
     * GET /api/prediction/markets/sync-progress
     */
    @GetMapping("/markets/sync-progress")
    public Object syncProgress() {
        return predictionMarketFullSyncService.getProgress();
    }

    /**
     * POST /api/prediction/markets/retry/{eventSlug}
     */
    @PostMapping("/markets/retry/{eventSlug}")
    public String retryMarket(
            @PathVariable String eventSlug
    ) {
        return predictionMarketFullSyncService.retryEvent(eventSlug);
    }

    /**
     * POST /api/prediction/markets/price-refresh
     */
    @PostMapping("/markets/price-refresh")
    public PredictionPriceRefreshResult refreshPrices() {
        return predictionMarketPriceRefreshService.refreshPrices(true);
    }

    /**
     * GET /api/prediction/markets
     */
    @GetMapping("/markets")
    public List<PredictionMarketResponse> getMarkets() {
        return predictionMarketService.getMarkets();
    }

    /**
     * 建立 Polymarket CLOB API credentials。
     *
     * POST /api/prediction/clob/api-key/create?nonce=0
     */
    @PostMapping("/clob/api-key/create")
    public PolymarketApiCredentialsResponse createClobApiKey(
            @RequestParam(defaultValue = "0") BigInteger nonce
    ) {
        return polymarketClobAuthService.createApiKey(nonce);
    }

    /**
     * 取回同 nonce 對應的 Polymarket CLOB API credentials。
     *
     * GET /api/prediction/clob/api-key/derive?nonce=0
     */
    @GetMapping("/clob/api-key/derive")
    public PolymarketApiCredentialsResponse deriveClobApiKey(
            @RequestParam(defaultValue = "0") BigInteger nonce
    ) {
        return polymarketClobAuthService.deriveApiKey(nonce);
    }

    /**
     * 初始化 Session Signer。
     *
     * POST /api/prediction/session/init
     */
    @PostMapping("/session/init")
    public SessionInitResponse initSession(
            @Valid @RequestBody SessionInitRequest request
    ) {
        return polymarketSessionService.initSession(request);
    }

    /**
     * 確認 Session Signer。
     *
     * POST /api/prediction/session/confirm
     */
    @PostMapping("/session/confirm")
    public SessionConfirmResponse confirmSession(
            @Valid @RequestBody SessionConfirmRequest request
    ) {
        return polymarketSessionService.confirmSession(request);
    }

    /**
     * 查詢使用者 sessions。
     *
     * GET /api/prediction/session/list?userAddress=0x...
     */
    @GetMapping("/session/list")
    public List<SessionListResponse> listSessions(
            @RequestParam String userAddress
    ) {
        return polymarketSessionService.listSessions(userAddress);
    }

    /**
     * 撤銷單一 session。
     *
     * POST /api/prediction/session/revoke
     */
    @PostMapping("/session/revoke")
    public String revokeSession(
            @Valid @RequestBody SessionRevokeRequest request
    ) {
        return polymarketSessionService.revokeSession(request);
    }

    /**
     * 撤銷使用者全部 ACTIVE sessions。
     *
     * POST /api/prediction/session/revoke-all?userAddress=0x...
     */
    @PostMapping("/session/revoke-all")
    public String revokeAllSessions(
            @RequestParam String userAddress
    ) {
        return polymarketSessionService.revokeAllSessions(userAddress);
    }

    /**
     * 真實下單。
     *
     * POST /api/prediction/orders
     */
    @PostMapping("/orders")
    public PolymarketPlaceOrderResponse placeOrder(
            @Valid @RequestBody PolymarketPlaceOrderRequest request
    ) {
        return polymarketOrderService.placeOrder(request);
    }

    /**
     * 查內部 Polymarket orders。
     *
     * GET /api/prediction/orders/local
     */
    @GetMapping("/orders/local")
    public List<PredictionPolymarketOrder> listLocalOrders() {
        return polymarketOrderTrackingService.listLocalOrders();
    }

    /**
     * 查單一內部 Polymarket order。
     *
     * GET /api/prediction/orders/local/{internalOrderId}
     */
    @GetMapping("/orders/local/{internalOrderId}")
    public PredictionPolymarketOrder getLocalOrder(
            @PathVariable String internalOrderId
    ) {
        return polymarketOrderTrackingService.getLocalOrder(internalOrderId);
    }

    /**
     * 從 CLOB 同步單一 order 狀態。
     *
     * POST /api/prediction/orders/local/{internalOrderId}/sync
     */
    @PostMapping("/orders/local/{internalOrderId}/sync")
    public PredictionPolymarketOrder syncLocalOrder(
            @PathVariable String internalOrderId
    ) {
        return polymarketOrderTrackingService.syncOrder(internalOrderId);
    }

    /**
     * 取消單一 CLOB order。
     *
     * POST /api/prediction/orders/local/{internalOrderId}/cancel
     */
    @PostMapping("/orders/local/{internalOrderId}/cancel")
    public PredictionPolymarketOrder cancelLocalOrder(
            @PathVariable String internalOrderId
    ) {
        return polymarketOrderTrackingService.cancelOrder(internalOrderId);
    }

    /**
     * 對帳本地未終態 orders。
     *
     * POST /api/prediction/orders/reconcile
     */
    @PostMapping("/orders/reconcile")
    public Map<String, Object> reconcileOrders() {
        return polymarketOrderTrackingService.reconcileOpenOrders();
    }

    /**
     * 查 CLOB trades。
     *
     * GET /api/prediction/orders/trades
     */
    @GetMapping("/orders/trades")
    public Map<String, Object> getTrades() {
        return polymarketOrderTrackingService.getTrades();
    }

    /**
     * 啟動 Polymarket user WebSocket。
     *
     * POST /api/prediction/ws/user/start
     */
    @PostMapping("/ws/user/start")
    public PolymarketUserWsStatusResponse startUserWebSocket() {
        return polymarketUserWebSocketService.startUserChannel();
    }

    /**
     * 停止 Polymarket user WebSocket。
     *
     * POST /api/prediction/ws/user/stop
     */
    @PostMapping("/ws/user/stop")
    public PolymarketUserWsStatusResponse stopUserWebSocket() {
        return polymarketUserWebSocketService.stopUserChannel();
    }

    /**
     * 查 Polymarket user WebSocket 狀態。
     *
     * GET /api/prediction/ws/user/status
     */
    @GetMapping("/ws/user/status")
    public PolymarketUserWsStatusResponse userWebSocketStatus() {
        return polymarketUserWebSocketService.status();
    }

    /**
     * 查 ERC20 allowance。
     *
     * GET /api/prediction/approve/collateral/allowance?owner=0x...
     */
    @GetMapping("/approve/collateral/allowance")
    public String getCollateralAllowance(
            @RequestParam String owner
    ) {
        BigInteger allowance =
                polymarketApprovalService.getCollateralAllowance(owner);

        return allowance.toString();
    }

    /**
     * 查 ERC1155 approve status。
     *
     * GET /api/prediction/approve/conditional-tokens/status?owner=0x...
     */
    @GetMapping("/approve/conditional-tokens/status")
    public Boolean isConditionalTokensApproved(
            @RequestParam String owner
    ) {
        return polymarketApprovalService.isConditionalTokensApproved(owner);
    }
}
