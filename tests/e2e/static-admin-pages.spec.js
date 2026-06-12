import { expect, test } from '@playwright/test';

const ok = (data) => ({ ok: true, data });

test('trading console loads the core workflow controls', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle(/Polymarket Trading/);
  await expect(page.getByRole('heading', { name: 'Polymarket Trading' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Connect Wallet' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Init Session' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Place Order' })).toBeVisible();
});

test('exchange console renders client trading workflow without admin funding controls', async ({ page }) => {
  // Scenario: authenticated client loads markets from admin config and cannot type arbitrary symbols or UIDs.
  await page.addInitScript(() => {
    localStorage.setItem('exchangeAccessToken', 'test-access-token');
    localStorage.setItem('exchangeRefreshToken', 'test-refresh-token');
    // Scenario: the client should multiplex public market and private user subscriptions over one WebSocket.
    window.__exchangeWebSocketUrls = [];
    window.__exchangeWebSocketCommands = [];
    window.WebSocket = class MockExchangeWebSocket {
      constructor(url) {
        this.url = url;
        window.__exchangeWebSocketUrls.push(url);
        this.readyState = 0;
        setTimeout(() => {
          this.readyState = 1;
          this.onopen?.();
        }, 20);
      }

      send(raw) {
        const command = JSON.parse(raw);
        window.__exchangeWebSocketCommands.push(command);
        if (command.type === 'subscribe.market') {
          this.onmessage?.({ data: JSON.stringify({ event: 'subscribed.market', data: { symbol: command.symbol } }) });
          this.onmessage?.({ data: JSON.stringify({ event: 'market-maker.quote', data: { symbol: command.symbol } }) });
        }
        if (command.type === 'subscribe.user') {
          this.onmessage?.({ data: JSON.stringify({ event: 'subscribed.user', data: { uid: command.uid, connectionId: 'ws-e2e-1' } }) });
          setTimeout(() => {
            this.onmessage?.({ data: JSON.stringify({ event: 'order.lifecycle', data: { orderId: 'order-live-refresh' } }) });
          }, 50);
        }
      }

      close() {
        this.readyState = 3;
        this.onclose?.();
      }
    };
  });
  await page.route('**/api/markets', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        markets: [
          { symbol: 'BTCUSDT', tradingEnabled: true },
          { symbol: 'ETHUSDT', tradingEnabled: true },
          { symbol: 'DOGEUSDT', tradingEnabled: false }
        ]
      }))
    });
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: 10001,
        email: 'demo@example.com',
        roles: ['USER'],
        scopes: ['TRADE']
      }))
    });
  });
  await page.route('**/api/depth/BTCUSDT?depth=10', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        symbol: 'BTCUSDT',
        version: 7,
        checksum: 123456,
        bestBid: '99.50',
        bestAsk: '100.50',
        // Scenario: backend or replay order may arrive unsorted; the client should render Binance-style high-to-low ticks.
        bids: [
          { price: '98.70', qty: '0.500' },
          { price: '99.50', qty: '2.000' },
          { price: '99.00', qty: '1.000' }
        ],
        asks: [
          { price: '100.50', qty: '1.500' },
          { price: '101.20', qty: '0.300' },
          { price: '100.80', qty: '0.700' }
        ]
      }))
    });
  });
  await page.route('**/api/market-maker/quotes/active?limit=50', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([
        {
          marketMakerId: 'mm-client-flow',
          uid: 91001,
          symbol: 'BTCUSDT',
          refId: 'mm-flow-e2e-1',
          active: true,
          accepted: true,
          bidPrice: '99.50',
          bidQuantity: '2.000',
          askPrice: '100.50',
          askQuantity: '1.500'
        }
      ]))
    });
  });
  let openOrderRequests = 0;
  await page.route('**/api/order/open?uid=10001&symbol=BTCUSDT', async (route) => {
    openOrderRequests += 1;
    const orders = [
      {
        orderId: 'order-1234567890',
        symbol: 'BTCUSDT',
        side: 'BUY',
        type: 'LIMIT',
        price: '99.50',
        qty: '2.000',
        executedQty: '0.000',
        status: 'NEW'
      }
    ];
    if (openOrderRequests > 1) {
      // Scenario: user WebSocket lifecycle signals should refresh open orders without pressing Reload Orders.
      orders.push({
        orderId: 'order-live-refresh',
        symbol: 'BTCUSDT',
        side: 'SELL',
        type: 'LIMIT',
        price: '100.50',
        qty: '1.500',
        executedQty: '0.000',
        status: 'NEW'
      });
    }
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok(orders))
    });
  });
  await page.route('**/api/margin/account?uid=10001', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: 10001,
        balance: '10000.00',
        available: '9750.00',
        frozen: '250.00',
        orderHold: '250.00',
        positionMargin: '0.00'
      }))
    });
  });
  await page.route('**/api/order/place', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok('accepted'))
    });
  });
  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ revoked: true }))
    });
  });

  await page.goto('/exchange.html');

  await expect(page).toHaveTitle(/Exchange Console/);
  await expect(page.getByRole('heading', { name: 'Exchange Console' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Admin Console' })).toHaveAttribute('href', '/admin-console.html');
  await expect(page.locator('#symbolTitle')).toHaveText('BTCUSDT');
  await expect(page.locator('#symbol')).toHaveValue('BTCUSDT');
  await expect(page.locator('#symbol option')).toHaveText(['BTCUSDT', 'ETHUSDT']);
  await expect(page.locator('#uid')).toHaveAttribute('type', 'hidden');
  await expect(page.locator('#uidDisplay')).toHaveText('10001');
  await expect(page.locator('#authEmail')).toHaveValue('');
  await expect(page.locator('#authPassword')).toHaveValue('');
  await expect(page.locator('#sessionDisplay')).toContainText('demo@example.com');
  await expect(page.getByRole('heading', { name: 'User Account' })).toBeVisible();
  await expect(page.getByRole('cell', { name: '99.5' }).first()).toBeVisible();
  await expect(page.locator('#orderBook tr.depth-row td:first-child')).toHaveText(['101.2', '100.8', '100.5', '99.5', '99']);
  await expect(page.locator('#orderBook tr.depth-row')).toHaveCount(5);
  await expect(page.getByRole('heading', { name: 'Market Maker Flow' })).toBeVisible();
  await expect(page.locator('#mmStatus')).toHaveText('1 active');
  await expect(page.locator('#mmLatestRef')).toHaveText('mm-flow-e2e-1');
  await expect(page.locator('.depth-fill').first()).toBeVisible();
  await expect.poll(() => page.evaluate(() => window.__exchangeWebSocketUrls)).toEqual(
    [expect.stringContaining('/ws/exchange')]
  );
  await expect.poll(() => page.evaluate(() => window.__exchangeWebSocketCommands)).toEqual(
    expect.arrayContaining([
      expect.objectContaining({ type: 'subscribe.market', symbol: 'BTCUSDT' }),
      expect.objectContaining({ type: 'subscribe.user', uid: 10001, cancelOnDisconnect: false })
    ])
  );
  await expect(page.getByRole('cell', { name: 'order-123456' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'order-live-r' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'BTCUSDT' }).first()).toBeVisible();
  await expect(page.locator('#balance')).toContainText('10,000');
  await expect(page.locator('#available')).toContainText('9,750');
  await expect(page.locator('#positionMargin')).toContainText('0');
  await expect(page.getByText('Frozen = order hold + position margin')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Airdrop USDT' })).toHaveCount(0);

  await page.getByRole('button', { name: 'Place Buy' }).click();
  await expect(page.locator('#orderResult')).toContainText('accepted');

  // Logout clears stale account/order state so shared browsers do not display the previous user's snapshot.
  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page.locator('#balance')).toHaveText('-');
  await expect(page.locator('#available')).toHaveText('-');
  await expect(page.locator('#frozen')).toHaveText('-');
  await expect(page.locator('#accountRaw')).toContainText('No account loaded');
  await expect(page.locator('#orders')).toContainText('Login and refresh to load open orders');
  await expect(page.locator('#orderResult')).toContainText('No order submitted');
});

test('admin console groups operator pages behind tabs', async ({ page }) => {
  await page.route('**/api/margin/account?uid=1', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ uid: 1, balance: '0.00', available: '0.00', frozen: '0.00' }))
    });
  });
  await page.route('**/api/admin/test-funds/airdrop', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: 1,
        asset: 'USDT',
        amount: '10000.00',
        status: 'CONFIRMED'
      }))
    });
  });

  await page.goto('/admin-console.html');

  await expect(page).toHaveTitle(/Admin Console/);
  await expect(page.getByRole('heading', { name: 'Admin Console' })).toBeVisible();
  await expect(page.locator('#adminFrame')).toHaveAttribute('src', '/admin-test-funds.html');
  await page.frameLocator('#adminFrame').getByRole('button', { name: 'Airdrop USDT' }).click();
  await expect(page.frameLocator('#adminFrame').locator('#airdropResult')).toContainText('CONFIRMED');

  // Funding, market settings, risk, market makers, and DLQ stay discoverable from the single admin page shell.
  await page.getByRole('button', { name: 'Market Config' }).click();
  await expect(page.locator('#adminFrame')).toHaveAttribute('src', '/admin-market-config.html');
  await page.getByRole('button', { name: 'Risk Parameters' }).click();
  await expect(page.locator('#adminFrame')).toHaveAttribute('src', '/admin-risk-parameters.html');
  await page.getByRole('button', { name: 'Market Makers' }).click();
  await expect(page.locator('#adminFrame')).toHaveAttribute('src', '/admin-market-maker.html');
  await page.getByRole('button', { name: 'DLQ' }).click();
  await expect(page.locator('#adminFrame')).toHaveAttribute('src', '/admin-dlq.html');
});

test('admin test funds page issues airdrop and refreshes account snapshot', async ({ page }) => {
  await page.route('**/api/margin/account?uid=1', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ uid: 1, balance: '0.00', available: '0.00', frozen: '0.00' }))
    });
  });
  await page.route('**/api/admin/test-funds/airdrop', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: 1,
        asset: 'USDT',
        amount: '10000.00',
        status: 'CONFIRMED'
      }))
    });
  });

  await page.goto('/admin-test-funds.html');

  await expect(page).toHaveTitle(/Admin Test Funds/);
  await expect(page.getByRole('heading', { name: 'Admin Test Funds' })).toBeVisible();
  await page.getByRole('button', { name: 'Airdrop USDT' }).click();
  await expect(page.locator('#airdropResult')).toContainText('CONFIRMED');
});

test('admin market config page renders mocked market data and supports filtering', async ({ page }) => {
  // Mock both the read model and fee-write endpoint so the static admin page can be verified without a live DB.
  let feeWriteSeen = false;
  await page.route('**/api/admin/market-config', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        capabilities: { writesEnabled: true, disabledActions: ['EDIT_CORE_LIMITS', 'MANUAL_SUSPENSION'] },
        markets: [
          {
            symbol: 'BTCUSDT',
            status: 'TRADING_ENABLED',
            baseAsset: 'BTC',
            quoteAsset: 'USDT',
            priceTick: '0.10',
            lotSize: '0.001',
            minQty: '0.001',
            minNotional: '10',
            maxLeverage: '10',
            tradingMode: 'CONTINUOUS',
            sessionWindow: '24x7',
            matchingEnabled: true,
            marketDataEnabled: true,
            maxOpenOrders: 200,
            maxOrderNotional: '100000',
            maxPositionNotional: '500000',
            makerFeeRate: '0.001',
            takerFeeRate: '0.002',
            initialMarginRate: '0.10',
            maintenanceMarginRate: '0.05',
            riskTiers: [{ tier: 1, maxPositionNotional: '500000', initialMarginRate: '0.10', maintenanceMarginRate: '0.05', maxLeverage: '10' }]
          },
          {
            symbol: 'ETHUSDT',
            status: 'TRADING_ENABLED',
            baseAsset: 'ETH',
            quoteAsset: 'USDT',
            priceTick: '0.01',
            lotSize: '0.01',
            minQty: '0.01',
            minNotional: '10',
            maxLeverage: '8',
            tradingMode: 'CONTINUOUS',
            sessionWindow: '24x7',
            matchingEnabled: true,
            marketDataEnabled: true,
            maxOpenOrders: 200,
            maxOrderNotional: '80000',
            maxPositionNotional: '300000',
            makerFeeRate: '0.001',
            takerFeeRate: '0.002',
            initialMarginRate: '0.125',
            maintenanceMarginRate: '0.06',
            riskTiers: []
          }
        ]
      }))
    });
  });
  await page.route('**/api/admin/market-config/BTCUSDT/fees', async (route) => {
    feeWriteSeen = true;
    const body = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        symbol: 'BTCUSDT',
        oldMakerFeeRate: '0.001',
        oldTakerFeeRate: '0.002',
        newMakerFeeRate: body.makerFeeRate,
        newTakerFeeRate: body.takerFeeRate,
        operatorId: body.operatorId,
        reason: body.reason
      }))
    });
  });

  await page.goto('/admin-market-config.html');

  await expect(page.getByRole('heading', { name: 'Admin Market Config' })).toBeVisible();
  await expect(page.getByText('Fee edits apply to new orders only')).toBeVisible();
  await expect(page.getByRole('cell', { name: 'BTCUSDT' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'BTCUSDT' })).toBeVisible();
  await page.getByLabel('Maker Fee Rate').fill('0.0015');
  await page.getByLabel('Taker Fee Rate').fill('0.0025');
  await page.getByRole('button', { name: 'Save Fee Settings' }).click();
  await expect.poll(() => feeWriteSeen).toBe(true);

  await page.getByLabel('Filter').fill('ETH');
  await expect(page.getByRole('cell', { name: 'ETHUSDT' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'BTCUSDT' })).toHaveCount(0);
});

test('admin risk parameters page renders switches, symbols, and detail panel', async ({ page }) => {
  await page.route('**/api/admin/risk-parameters', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        capabilities: { disabledActions: ['UPDATE_SWITCHES', 'SUSPEND_SYMBOL'] },
        switches: {
          orderEntryHalt: false,
          reduceOnlyMode: false,
          withdrawalHalt: false,
          liquidationHalt: false,
          liquidationManualReview: true,
          marketMakerHedgeExecutionHalt: false,
          orderEntryFrequencyLimitEnabled: true,
          liquidationScanBatchSize: 100,
          suspendedSymbols: ['DOGEUSDT']
        },
        symbols: [
          {
            symbol: 'BTCUSDT',
            status: 'ACTIVE',
            suspended: false,
            maxLeverage: '10',
            maxOrderNotional: '100000',
            maxPositionNotional: '500000',
            initialMarginRate: '0.10',
            maintenanceMarginRate: '0.05',
            priceBandRate: '0.05',
            oracle: {
              stale: false,
              status: 'FRESH',
              markPrice: '65000',
              indexPrice: '65001',
              source: 'MOCK',
              updatedAt: '2026-06-11T00:00:00Z'
            },
            riskTiers: [{ tier: 1, maxPositionNotional: '500000', initialMarginRate: '0.10', maintenanceMarginRate: '0.05', maxLeverage: '10' }]
          }
        ]
      }))
    });
  });

  await page.goto('/admin-risk-parameters.html');

  await expect(page.getByRole('heading', { name: 'Admin Risk Parameters' })).toBeVisible();
  await expect(page.getByText('Liquidation Review')).toBeVisible();
  await expect(page.getByRole('cell', { name: 'BTCUSDT' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'BTCUSDT' })).toBeVisible();
  await expect(page.getByText('Suspended symbols: DOGEUSDT')).toBeVisible();
});

test('admin market maker page manages strategy and hedge operations', async ({ page }) => {
  // Scenario: operator edits one market-maker strategy row, sees quote/hedge status, then runs guarded hedge actions.
  let savedProfile = null;
  let hedgeExecutionSeen = false;
  let idempotencyReconcileSeen = false;
  const profile = {
    marketMakerId: 'mm-alpha',
    uid: 90001,
    enabled: true,
    riskLimits: [
      {
        symbol: 'BTCUSDT',
        maxLongNotional: '50000',
        maxShortNotional: '50000',
        maxOrderNotional: '10000',
        maxSlippageRate: '0.005',
        killSwitch: false
      }
    ]
  };

  await page.route('**/api/market-maker/profiles/enabled', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([savedProfile || profile]))
    });
  });
  await page.route('**/api/market-maker/profiles', async (route) => {
    savedProfile = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok(savedProfile))
    });
  });
  await page.route('**/api/market-maker/quotes/active?limit=50', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([
        {
          marketMakerId: 'mm-alpha',
          uid: 90001,
          symbol: 'BTCUSDT',
          active: true,
          accepted: true,
          bidOrderId: 'bid-1',
          askOrderId: 'ask-1'
        }
      ]))
    });
  });
  await page.route('**/api/market-maker/quotes/reconciliation?limit=50', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ issueCount: 1, issues: [{ symbol: 'BTCUSDT', reason: 'MISSING_ASK' }] }))
    });
  });
  await page.route('**/api/market-maker/hedge-idempotency/unresolved?limit=50', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ issueCount: 2, issues: [] }))
    });
  });
  await page.route('**/api/market-maker/profiles/mm-alpha/hedge-reconciliation?limit=50', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ marketMakerId: 'mm-alpha', checkedDecisions: 3, issueCount: 1, issues: [] }))
    });
  });
  await page.route('**/api/market-maker/profiles/mm-alpha/hedge-fills?limit=20', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([
        { symbol: 'BTCUSDT', side: 'BUY', quantity: '0.25', price: '100.5', refId: 'manual-1' }
      ]))
    });
  });
  await page.route('**/api/market-maker/profiles/mm-alpha/hedge-execution?refPrefix=*', async (route) => {
    hedgeExecutionSeen = true;
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        marketMakerId: 'mm-alpha',
        exposureCount: 1,
        plannedCount: 1,
        routedCount: 1,
        strategyDecisions: [],
        hedgeDecisions: []
      }))
    });
  });
  await page.route('**/api/market-maker/hedge-idempotency/reconcile?limit=50', async (route) => {
    idempotencyReconcileSeen = true;
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ issueCount: 0, issues: [] }))
    });
  });

  await page.goto('/admin-market-maker.html');

  await expect(page.getByRole('heading', { name: 'Admin Market Maker' })).toBeVisible();
  await expect(page.locator('#profiles').getByRole('cell', { name: 'mm-alpha' })).toBeVisible();
  await expect(page.locator('#activeQuoteCount')).toHaveText('1');
  await expect(page.locator('#quoteIssueCount')).toHaveText('1');
  await expect(page.locator('#hedgeIssueCount')).toHaveText('1');
  await expect(page.locator('#idempotencyIssueCount')).toHaveText('2');
  await expect(page.getByRole('cell', { name: 'manual-1' })).toBeVisible();

  await page.getByLabel('Max Order Notional').fill('12000');
  await page.getByRole('button', { name: 'Save Strategy' }).click();
  await expect.poll(() => savedProfile?.riskLimits?.[0]?.maxOrderNotional).toBe('12000');
  await expect(page.locator('#profileResult')).toContainText('"maxOrderNotional": "12000"');

  await page.getByRole('button', { name: 'Run Hedge' }).click();
  await expect.poll(() => hedgeExecutionSeen).toBe(true);
  await expect(page.locator('#opsResult')).toContainText('"routedCount": 1');

  await page.getByRole('button', { name: 'Reconcile Idempotency' }).click();
  await expect.poll(() => idempotencyReconcileSeen).toBe(true);
  await expect(page.locator('#opsResult')).toContainText('"issueCount": 0');
});

test('admin DLQ page renders rows and opens sanitized detail', async ({ page }) => {
  await page.route('**/api/admin/dlq?limit=*', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        capabilities: { actionRequirements: ['operator reason', 'confirmation', 'audit trail'] },
        items: [
          {
            dlqId: 'dlq-1',
            outboxId: 'outbox-1',
            status: 'FAILED',
            eventType: 'TradeExecuted',
            eventKey: 'BTCUSDT',
            topic: 'trade.executed',
            attempts: 3,
            errorSummary: 'Broker unavailable',
            payloadPreview: '{"symbol":"BTCUSDT"}',
            headerPreview: '{"requestId":"req-1"}',
            requestId: 'req-1',
            correlationId: 'corr-1',
            replayEligible: true,
            compensationEligible: false,
            createdAt: '2026-06-11T00:00:00Z'
          }
        ]
      }))
    });
  });

  await page.goto('/admin-dlq.html');

  await expect(page.getByRole('heading', { name: 'Admin DLQ' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'TradeExecuted' })).toBeVisible();

  await page.getByRole('cell', { name: 'TradeExecuted' }).click();

  await expect(page.getByRole('heading', { name: 'TradeExecuted' })).toBeVisible();
  await expect(page.locator('#detail').getByText('Broker unavailable')).toBeVisible();
  await expect(page.locator('#detail').getByText('{"symbol":"BTCUSDT"}')).toBeVisible();
});
