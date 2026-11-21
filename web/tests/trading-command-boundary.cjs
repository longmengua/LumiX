const assert = require('node:assert/strict');
const { chromium } = require('playwright');

// 僅測試 source 的 HTTP fixture；不會被 Vite application import，也不代表真實雙帳戶交易驗收。
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.LUMIX_CHROMIUM_PATH });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    const keys = [], errors = [];
    let requests = 0, futuresEnabled = false, futuresReduceOnly = false, futuresCommand = null;
    page.on('pageerror', error => errors.push(error.message));
    const order = { orderId: 'test-order-1-long-identifier-000000000000000', marketSymbol: 'BTC-USDT', side: 'BUY', price: '100', quantity: '0.1', filledQuantity: '0', remainingQuantity: '0.1', status: 'OPEN' };
    await page.route('**/api/**', async route => {
      const request = route.request(), path = new URL(request.url()).pathname;
      let value, status = 200;
      if (path.endsWith('/workspace')) value = { balances: [{ asset: 'USDT', total: '100', available: '100', locked: '0' }], fills: [], bids: [], asks: [], positions: [] };
      else if (path.endsWith('/configuration')) value = { ordersEnabled: !path.includes('/futures/') || futuresEnabled, reason: path.includes('/futures/') && !futuresEnabled ? 'FUTURES_RISK_RUNTIME_UNAVAILABLE' : futuresReduceOnly && path.includes('/futures/') ? 'REDUCE_ONLY' : 'AVAILABLE', makerFeeRate: '0.001', takerFeeRate: '0.002' };
      else if (path.endsWith('/reference')) value = { source: 'BINANCE_USDSM_PREMIUM_INDEX', markPrice: '100', indexPrice: '100', sourceAt: new Date().toISOString() };
      else if (path.endsWith('/orders') && request.method() === 'POST') {
        keys.push(request.headers()['idempotency-key']); requests++;
        assert.equal(request.headers()['x-lumix-intent'], 'trading-command');
        assert.equal(typeof request.postDataJSON().price, 'string');
        if (path.includes('/futures/')) futuresCommand = request.postDataJSON();
        status = requests === 1 ? 503 : 200; value = status === 200 ? order : {};
      } else if (path.endsWith('/orders')) value = { items: requests > 1 ? [order] : [] };
      else { status = 401; value = {}; }
      await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(value) });
    });
    await page.goto('http://127.0.0.1:8088/spot/BTC-USDT');
    const form = page.locator('form.trading-form');
    await form.locator('input').nth(0).fill('100');
    await form.locator('input').nth(1).fill('0.1');
    await form.locator('button[type=submit]').click();
    await form.locator('p[role=status]').filter({ hasText: /尚未確認|unconfirmed/ }).waitFor();
    await form.locator('button[type=submit]').click();
    await form.locator('p[role=status]').filter({ hasText: /test-order-1/ }).waitFor();
    assert.equal(keys.length, 2); assert.equal(keys[0], keys[1]);
    assert.equal(errors.length, 0);
    for (const width of [1440, 768, 390]) {
      await page.setViewportSize({ width, height: 900 });
      const layout = await page.evaluate(() => ({ width: innerWidth, scroll: document.documentElement.scrollWidth }));
      assert.ok(layout.scroll <= width, `overflow ${width}: ${JSON.stringify(layout)}`);
      const table = page.locator('.trading-account-table').last();
      const bounds = await table.evaluate(node => ({ width: node.clientWidth, scroll: node.scrollWidth, overflow: getComputedStyle(node).overflowX }));
      assert.equal(bounds.overflow, 'auto');
      if (width === 390) assert.ok(bounds.scroll > bounds.width, 'mobile table must scroll to every column');
    }
    await page.goto('http://127.0.0.1:8088/futures/BTC-USDT');
    await page.locator('form.trading-form button[type=submit]').waitFor();
    await page.locator('.trading-form').getByText(/合約保證金|Futures margin/).waitFor();
    assert.equal(await page.locator('form.trading-form button[type=submit]').isDisabled(), true);
    assert.equal(await page.locator('.trading-form').innerText().then(value => /合約保證金|Futures margin/.test(value)), true);
    futuresEnabled = true;
    await page.reload();
    const futuresForm = page.locator('form.trading-form');
    await futuresForm.locator('button[type=submit]:enabled').waitFor();
    await futuresForm.getByRole('button', { name: 'SHORT' }).click();
    await futuresForm.locator('input').nth(1).fill('100');
    await futuresForm.locator('input').nth(2).fill('0.1');
    await futuresForm.locator('button[type=submit]').click();
    await futuresForm.locator('p[role=status]').filter({ hasText: /test-order-1/ }).waitFor();
    assert.equal(futuresCommand.positionSide, 'SHORT');
    assert.equal(futuresCommand.side, 'SELL');
    assert.equal(futuresCommand.reduceOnly, false);
    assert.equal(futuresCommand.leverage, '5');
    assert.equal(errors.length, 0);
    futuresReduceOnly = true;
    await page.reload();
    await futuresForm.getByText(/風險處置中|Risk halt/).waitFor();
    assert.equal(await futuresForm.locator('button[type=submit]').isDisabled(), true);
    await futuresForm.locator('select').nth(0).selectOption('CLOSE');
    assert.equal(await futuresForm.locator('button[type=submit]').isEnabled(), true);
    console.log(JSON.stringify({ retryPreservedIdempotencyKey: true, decimalStringCommand: true, viewports: [1440, 768, 390], futuresFailClosed: true, futuresCommand: true, futuresReduceOnly: true, pageErrors: errors }));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
