import { expect, test } from '@playwright/test';

const ok = (data) => ({ ok: true, data });

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
  await page.route('**/api/depth/BTCUSDT?depth=*', async (route) => {
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
  let clientActiveQuoteRequests = 0;
  await page.route('**/api/market-maker/quotes/active?limit=50', async (route) => {
    clientActiveQuoteRequests += 1;
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([]))
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
  // Scenario: the prod-facing client shell must not expose privileged admin navigation.
  await expect(page.getByRole('link', { name: 'Admin Console' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Trade' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.locator('.order-entry #symbol')).toHaveValue('BTCUSDT');
  await expect(page.locator('.order-entry #symbol option')).toHaveText(['BTCUSDT', 'ETHUSDT']);
  await expect(page.getByRole('button', { name: 'Reload Market' })).toHaveCount(0);
  await expect(page.getByText('Checksum')).toHaveCount(0);
  await expect(page.getByText('Book Version')).toHaveCount(0);
  await expect(page.locator('#uid')).toHaveAttribute('type', 'hidden');
  await expect(page.locator('#uidDisplay')).toHaveText('10001');
  await expect(page.locator('#authEmail')).toHaveValue('');
  await expect(page.locator('#authPassword')).toHaveValue('');
  await expect(page.locator('#sessionDisplay')).toContainText('demo@example.com');
  // Scenario: account data refreshes from auth/session flow, so the prod-facing account panel has no manual reload button.
  await expect(page.getByRole('button', { name: 'Reload Account' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Reload Orders' }).locator('svg')).toBeVisible();
  await expect(page.getByRole('cell', { name: '99.5' }).first()).toBeVisible();
  await expect(page.locator('#orderBook tr.depth-row td:first-child')).toHaveText(['101.2', '100.8', '100.5', '99.5', '99', '98.7']);
  await expect(page.locator('#orderBook tr.depth-row')).toHaveCount(6);
  // Scenario: market-maker telemetry is operator-only and must not appear or be fetched on the customer exchange.
  await expect(page.getByRole('heading', { name: 'Market Maker Flow' })).toHaveCount(0);
  expect(clientActiveQuoteRequests).toBe(0);
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
  // Scenario: the profile drawer exposes real account/order snapshots without customer-facing section toggles.
  await page.getByRole('button', { name: 'Open Profile' }).click();
  await expect(page.getByText('Sign in to view balances')).toHaveCount(0);
  await expect(page.locator('#authCard')).toBeHidden();
  await expect(page.locator('#accountSummary')).toBeVisible();
  await expect(page.locator('#profileContent')).toBeVisible();
  await expect(page.locator('#profileBalance')).toContainText('10,000');
  await expect(page.locator('#profileFrozen')).toContainText('250');
  await expect(page.locator('#profileOrderHold')).toContainText('250');
  await expect(page.locator('#profilePositionMargin')).toContainText('0');
  await expect(page.locator('#balance')).toHaveCount(0);
  await expect(page.locator('#available')).toHaveCount(0);
  await expect(page.locator('#frozen')).toHaveCount(0);
  await expect(page.locator('#positionMargin')).toHaveCount(0);
  await expect(page.locator('[data-profile-panel="orders"]')).toContainText('order-live-r');
  await expect(page.locator('[data-profile-panel="heldPositions"]')).toBeVisible();
  await expect(page.locator('[data-profile-panel="positionHistory"]')).toBeVisible();
  await expect(page.locator('[data-profile-panel="categoryInfo"]')).toHaveCount(0);
  await expect(page.locator('[data-profile-panel="frozen"]')).toHaveCount(0);
  await expect(page.locator('#profileSectionSummary')).toHaveCount(0);
  await expect(page.locator('#profileSelectAll')).toHaveCount(0);
  await page.locator('#profileClose').click();
  await expect(page.locator('#profilePanel')).toBeHidden();
  await expect(page.locator('[data-tab="account"]')).toHaveCount(0);
  // Layout guard: client trading keeps book/order entry first, then open orders, with account details behind a tab.
  const layout = await page.evaluate(() => {
    const rect = (selector) => {
      const box = document.querySelector(selector).getBoundingClientRect();
      return { top: Math.round(box.top), left: Math.round(box.left), width: Math.round(box.width) };
    };
    return {
      innerWidth,
      scrollWidth: document.documentElement.scrollWidth,
      book: rect('.book-panel'),
      entry: rect('.order-entry'),
      orders: rect('.orders-panel')
    };
  });
  expect(layout.scrollWidth).toBeLessThanOrEqual(layout.innerWidth + 2);
  if (layout.innerWidth >= 900) {
    expect(Math.abs(layout.book.top - layout.entry.top)).toBeLessThanOrEqual(2);
    expect(layout.entry.left).toBeGreaterThan(layout.book.left);
  }
  expect(layout.orders.top).toBeGreaterThan(Math.max(layout.book.top, layout.entry.top));
  await expect(page.getByRole('button', { name: 'Airdrop USDT' })).toHaveCount(0);

  await page.getByRole('button', { name: 'Place Buy' }).click();
  await expect(page.locator('#orderResult')).toContainText('accepted');

  // Logout clears stale account/order state so shared browsers do not display the previous user's snapshot.
  await page.getByRole('button', { name: 'Open Profile' }).click();
  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page.locator('#authCard')).toBeVisible();
  await expect(page.locator('#accountSummary')).toBeHidden();
  await expect(page.locator('#profileBalance')).toHaveText('-');
  await expect(page.locator('#authState')).toBeHidden();
  await expect(page.locator('#accountRaw')).toBeHidden();
  await expect(page.locator('#orders')).toContainText('Login and refresh to load open orders');
  await expect(page.locator('#orderResult')).toContainText('No order submitted');
  await expect(page.locator('#authCard')).toBeVisible();
  await expect(page.locator('#profileContent')).toBeHidden();

  // Locale switching should translate the client console without changing the default English test flow.
  await page.locator('#language').selectOption('zh-TW');
  await expect(page.getByRole('heading', { name: '交易所前台' })).toBeVisible();
  await page.locator('#language').selectOption('ms');
  await expect(page.getByRole('heading', { name: 'Konsol Bursa' })).toBeVisible();
  await page.locator('#language').selectOption('ko');
  await expect(page.getByRole('heading', { name: '거래소 콘솔' })).toBeVisible();
  await page.locator('#language').selectOption('en');
  await expect(page.getByRole('heading', { name: 'Exchange Console' })).toBeVisible();
});

test('exchange console registers with on-screen email code before login', async ({ page }) => {
  // Scenario: registration and login are the customer P0 path; the email link is backup, while the visible code field is primary.
  await page.addInitScript(() => {
    window.WebSocket = class NoopExchangeWebSocket {
      constructor() {
        this.readyState = 1;
        setTimeout(() => this.onopen?.(), 10);
      }

      send() {
      }

      close() {
        this.readyState = 3;
        this.onclose?.();
      }
    };
  });
  let configRequests = 0;
  await page.route('**/api/auth/config', async (route) => {
    configRequests += 1;
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        humanVerificationEnabled: false,
        humanVerificationProvider: 'turnstile',
        humanVerificationSiteKey: '',
        emailVerificationEnabled: true
      }))
    });
  });
  await page.route('**/api/markets', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ markets: [{ symbol: 'BTCUSDT', tradingEnabled: true }] }))
    });
  });
  await page.route('**/api/depth/BTCUSDT?depth=*', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ symbol: 'BTCUSDT', bestBid: '0', bestAsk: '0', bids: [], asks: [] }))
    });
  });
  await page.route('**/api/order/open?uid=10011&symbol=BTCUSDT', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok([])) });
  });
  await page.route('**/api/margin/account?uid=10011', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: 10011,
        balance: '0.00',
        available: '0.00',
        frozen: '0.00',
        orderHold: '0.00',
        positionMargin: '0.00'
      }))
    });
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null)) });
  });

  let registerPayload;
  let loginPayload;
  await page.route('**/api/auth/register', async (route) => {
    registerPayload = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: 10011,
        email: 'new-user@example.com',
        emailVerificationRequired: true,
        verificationUrl: 'http://127.0.0.1:8080/exchange.html?verifyEmailToken=backup-token',
        expiresAt: '2026-06-13T00:00:00Z'
      }))
    });
  });
  let verifyPayload;
  await page.route('**/api/auth/verify-email', async (route) => {
    verifyPayload = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        tokenType: 'Bearer',
        accessToken: 'access-token-after-verification',
        accessTokenExpiresAt: '2026-06-13T00:00:00Z',
        refreshToken: 'refresh-token-after-verification',
        refreshTokenExpiresAt: '2026-07-13T00:00:00Z',
        user: {
          uid: 10011,
          email: 'new-user@example.com',
          roles: 'USER',
          scopes: 'trade funds:write user:read',
          preferredLanguage: 'zh-TW'
        }
      }))
    });
  });
  let loginRequests = 0;
  await page.route('**/api/auth/login', async (route) => {
    loginRequests += 1;
    loginPayload = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        tokenType: 'Bearer',
        accessToken: 'access-token-after-login',
        accessTokenExpiresAt: '2026-06-13T00:00:00Z',
        refreshToken: 'refresh-token-after-login',
        refreshTokenExpiresAt: '2026-07-13T00:00:00Z',
        user: {
          uid: 10011,
          email: 'new-user@example.com',
          roles: 'USER',
          scopes: 'trade funds:write user:read',
          preferredLanguage: 'zh-TW'
        }
      }))
    });
  });
  let languagePayload;
  await page.route('**/api/auth/language', async (route) => {
    languagePayload = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: 10011,
        email: 'new-user@example.com',
        roles: 'USER',
        scopes: 'trade funds:write user:read',
        preferredLanguage: languagePayload.preferredLanguage
      }))
    });
  });

  await page.goto('/exchange.html');
  await expect.poll(() => configRequests).toBeGreaterThan(0);
  await page.locator('#language').selectOption('zh-TW');
  await page.locator('#profileToggle').click();
  await expect(page.getByText('Sign in to view balances')).toHaveCount(0);
  await page.locator('#authEmail').fill('new-user@example.com');
  await page.locator('#authPassword').fill('correct-password');
  await page.locator('#register').click();
  await expect.poll(() => registerPayload).toMatchObject({
    email: 'new-user@example.com',
    password: 'correct-password',
    humanVerificationToken: '',
    preferredLanguage: 'zh-TW',
    timeZone: expect.any(String)
  });
  await expect(page.locator('#emailVerificationStep')).toBeVisible();
  await expect(page.locator('#emailVerificationCode')).toBeFocused();
  await expect(page.locator('#login')).toBeHidden();
  await expect(page.locator('#register')).toBeHidden();
  await expect(page.locator('#authNotice')).toContainText('請輸入信箱驗證碼');
  await expect(page.locator('#authNotice')).not.toContainText('verifyEmailToken');

  await page.locator('#emailVerificationCode').fill('123456');
  await page.locator('#verifyEmailCode').click();
  await expect.poll(() => verifyPayload).toMatchObject({
    email: 'new-user@example.com',
    code: '123456'
  });
  await expect(page.locator('#emailVerificationStep')).toBeHidden();
  await expect(page.locator('#authNotice')).toContainText('註冊已完成驗證');
  await expect(page.locator('#profileContent')).toBeVisible();
  await expect(page.locator('#sessionDisplay')).toContainText('new-user@example.com');
  await expect(page.locator('#uidDisplay')).toHaveText('10011');
  expect(loginRequests).toBe(0);
  await page.locator('#language').selectOption('ms');
  await expect.poll(() => languagePayload).toMatchObject({ preferredLanguage: 'ms' });
});

test('exchange console shows generic login error for unknown accounts', async ({ page }) => {
  // Scenario: login must not reveal whether an email exists, but it still needs a readable customer message.
  await page.route('**/api/auth/config', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        humanVerificationEnabled: false,
        humanVerificationProvider: 'turnstile',
        humanVerificationSiteKey: '',
        emailVerificationEnabled: true
      }))
    });
  });
  await page.route('**/api/markets', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ markets: [{ symbol: 'BTCUSDT', tradingEnabled: true }] }))
    });
  });
  await page.route('**/api/depth/BTCUSDT?depth=*', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({ symbol: 'BTCUSDT', bestBid: '0', bestAsk: '0', bids: [], asks: [] }))
    });
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null)) });
  });
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'AUTH_INVALID_CREDENTIAL',
        message: '發生異常',
        traceId: 'test-trace-id'
      })
    });
  });
  let pendingRegisterPayload;
  let resendPayload;
  await page.route('**/api/auth/register', async (route) => {
    pendingRegisterPayload = route.request().postDataJSON();
    await route.fulfill({
      status: 409,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'AUTH_REGISTRATION_PENDING',
        message: '發生異常',
        traceId: 'test-trace-id'
      })
    });
  });
  await page.route('**/api/auth/resend-verification', async (route) => {
    resendPayload = route.request().postDataJSON();
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok({
        uid: null,
        email: resendPayload.email,
        emailVerificationRequired: true,
        verificationUrl: null,
        expiresAt: '2026-06-13T00:00:00Z'
      }))
    });
  });

  await page.goto('/exchange.html');
  await page.getByRole('button', { name: 'Open Profile' }).click();
  await page.locator('#authEmail').fill('missing@example.com');
  await page.locator('#authPassword').fill('wrong-password');
  await page.getByRole('button', { name: 'Login' }).click();

  await expect(page.locator('#authError')).toContainText('Account not found or password is incorrect.');
  await expect(page.locator('#authError')).not.toContainText('HTTP');
  await expect(page.locator('#authError')).not.toContainText('發生異常');

  await page.locator('#register').click();
  await expect.poll(() => pendingRegisterPayload).toMatchObject({
    email: 'missing@example.com',
    password: 'wrong-password',
    preferredLanguage: 'en',
    timeZone: expect.any(String)
  });
  await expect(page.locator('#authError')).toBeHidden();
  await expect(page.locator('#emailVerificationStep')).toBeVisible();
  await expect(page.locator('#emailVerificationCode')).toBeFocused();
  await expect(page.locator('#authNotice')).toContainText('Enter the email verification code');
  await page.locator('#resendEmailCode').click();
  await expect.poll(() => resendPayload).toMatchObject({
    email: 'missing@example.com',
    preferredLanguage: 'en',
    timeZone: expect.any(String)
  });
  await expect(page.locator('#authNotice')).toContainText('A new verification code has been sent');

  // Scenario: reload returns to a clean auth form; submitting registration again routes known pending users to code entry.
  await page.reload();
  await page.getByRole('button', { name: 'Open Profile' }).click();
  await expect(page.locator('#emailVerificationStep')).toBeHidden();
  await page.locator('#authEmail').fill('missing@example.com');
  await page.locator('#authPassword').fill('wrong-password');
  await page.locator('#register').click();
  await expect(page.locator('#emailVerificationStep')).toBeVisible();
  await expect(page.locator('#resendEmailCode')).toBeVisible();
});
