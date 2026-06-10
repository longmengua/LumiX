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

test('admin market config page renders mocked market data and supports filtering', async ({ page }) => {
  await page.route('**/api/admin/market-config', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        capabilities: { disabledActions: ['EDIT_CONFIG', 'MANUAL_SUSPENSION'] },
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

  await page.goto('/admin-market-config.html');

  await expect(page.getByRole('heading', { name: 'Admin Market Config' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'BTCUSDT' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'BTCUSDT' })).toBeVisible();

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
