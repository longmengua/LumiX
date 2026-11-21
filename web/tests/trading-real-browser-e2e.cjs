const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { chromium } = require('playwright');

// 僅對隔離的 lumix_e2e 資料庫執行；session 與所有測試資金由 test source fixture 建立。
const cookies = JSON.parse(readFileSync(join(tmpdir(), 'lumix-e2e-cookies.json'), 'utf8'));
const api = 'http://127.0.0.1:8080';
const market = 'BTC-USDT';
const rules = (kind, fees) => ({
  instrumentId: `${kind === 'SPOT' ? 'SPOT' : 'PERPETUAL'}:BTCUSDT`, kind, marketSymbol: market,
  baseAsset: 'BTC', quoteAsset: 'USDT', policyVersion: 1, priceScale: 2, quantityScale: 4, quoteScale: 6,
  priceTick: '0.01', quantityStep: '0.0001', minimumQuantity: '0.0001', maximumQuantity: '100',
  minimumNotional: '1', maximumNotional: '10000000', makerFeeRate: fees[0], takerFeeRate: fees[1],
  feeRounding: 'DOWN',
});
const execution = (kind, fees, feeAccountId) => ({
  rules: rules(kind, fees), feeAccountId, maximumOpenOrders: 100,
  maximumSlippage: '0.05', feeAccrual: 'CUMULATIVE_QUOTE', counterpartyModel: 'CLIENT_TO_CLIENT',
});
async function adminPost(path, body) {
  const response = await fetch(api + path, { method: 'POST', headers: {
    'Content-Type': 'application/json', Origin: 'http://127.0.0.1:8089',
    'X-LumiX-Intent': 'trading-policy', Cookie: `LUMIX_ADMIN_SESSION=${cookies.admin}`,
  }, body: JSON.stringify(body) });
  const value = await response.text();
  assert.equal(response.status, 200, `${path}: HTTP ${response.status} ${value.slice(0, 400)}`);
  return JSON.parse(value);
}
async function clientGet(path, owner) {
  const response = await fetch(api + path, { headers: { Cookie: `LUMIX_SESSION=${cookies[owner]}` } });
  const value = await response.text();
  assert.equal(response.status, 200, `${path}: HTTP ${response.status} ${value.slice(0, 400)}`);
  return JSON.parse(value);
}
async function submit(page, { kind, direction, action, price, quantity }) {
  const form = page.locator('form.trading-form');
  if (kind === 'spot') await form.getByRole('button', { name: direction === 'BUY' ? /買入|Buy/ : /賣出|Sell/ }).click();
  else {
    await form.getByRole('button', { name: direction }).click();
    await form.locator('select').nth(0).selectOption(action);
  }
  const inputs = form.locator('input');
  await inputs.nth(kind === 'spot' ? 0 : 1).fill(price);
  await inputs.nth(kind === 'spot' ? 1 : 2).fill(quantity);
  await form.locator('button[type=submit]').click();
  await form.locator('p[role=status]').filter({ hasText: /委託 ord_|Order ord_/ }).waitFor({ timeout: 15000 });
}
(async () => {
  await adminPost('/api/admin/v1/trading/spot/policies', execution('SPOT', ['0.001', '0.002'], 'fees'));
  const anchor = new Date(Math.floor(Date.now() / 1000) * 1000 - 8 * 3600 * 1000).toISOString();
  const futures = { execution: execution('PERPETUAL', ['0', '0'], 'fees'), collateralAsset: 'USDT',
    collateralHaircut: '0', maximumLeverage: '5', maintenanceMarginRate: '0.05',
    maximumPositionNotionalUsdt: '1000000', markPriceSource: 'BINANCE_USDSM_PREMIUM_INDEX',
    indexPriceSource: 'BINANCE_USDSM_PREMIUM_INDEX', maximumPriceAge: 'PT30S',
    fundingScheduleAnchor: anchor, fundingInterval: 'PT8H', maximumAbsoluteFundingRate: '0.01',
    maximumConversionExposureUsdt: '100000', settlementRounding: 'DOWN',
    marginPoolAccountId: 'margin', insuranceAccountId: 'insurance', conversionPoolAccountId: 'conversion' };
  await adminPost('/api/admin/v1/trading/futures/policies', futures);
  await adminPost('/api/admin/v1/trading/futures/instruments/status', { marketSymbol: market, status: 'ACTIVE' });
  const reference = await clientGet('/api/v1/trading/futures/reference?market=BTC-USDT', 'alice');
  assert.equal(reference.source, 'BINANCE_USDSM_PREMIUM_INDEX');
  const futuresPrice = Number(reference.markPrice).toFixed(2);
  const browser = await chromium.launch({ headless: true, executablePath: process.env.LUMIX_CHROMIUM_PATH });
  const errors = [];
  try {
    const contexts = {};
    const pages = {};
    for (const owner of ['alice', 'bob']) {
      contexts[owner] = await browser.newContext({ viewport: { width: 1440, height: 900 } });
      await contexts[owner].addCookies([{ name: 'LUMIX_SESSION', value: cookies[owner], domain: '127.0.0.1', path: '/' }]);
      pages[owner] = await contexts[owner].newPage();
      pages[owner].on('pageerror', error => errors.push(`${owner}: ${error.message}`));
      await pages[owner].goto('http://127.0.0.1:8088/spot/BTC-USDT');
      await pages[owner].locator('form.trading-form button[type=submit]:enabled').waitFor({ timeout: 15000 });
    }
    await submit(pages.bob, { kind: 'spot', direction: 'SELL', price: '100', quantity: '0.1' });
    await submit(pages.alice, { kind: 'spot', direction: 'BUY', price: '100', quantity: '0.1' });
    const spotAlice = await clientGet('/api/v1/trading/spot/workspace?market=BTC-USDT', 'alice');
    const spotBob = await clientGet('/api/v1/trading/spot/workspace?market=BTC-USDT', 'bob');
    assert.equal(spotAlice.fills.length, 1);
    assert.equal(spotBob.fills.length, 1);
    assert.equal(Number(spotAlice.balances.find(row => row.asset === 'BTC').total), 2.1);
    assert.equal(Number(spotBob.balances.find(row => row.asset === 'BTC').total), 1.9);
    for (const owner of ['alice', 'bob']) {
      await pages[owner].goto('http://127.0.0.1:8088/futures/BTC-USDT');
      await pages[owner].locator('form.trading-form button[type=submit]:enabled').waitFor({ timeout: 15000 });
    }
    await submit(pages.bob, { kind: 'futures', direction: 'SHORT', action: 'OPEN', price: futuresPrice, quantity: '0.1' });
    await submit(pages.alice, { kind: 'futures', direction: 'LONG', action: 'OPEN', price: futuresPrice, quantity: '0.1' });
    const opened = await clientGet('/api/v1/trading/futures/workspace?market=BTC-USDT', 'alice');
    assert.equal(opened.positions.length, 1);
    assert.equal(opened.positions[0].positionSide, 'LONG');
    await submit(pages.alice, { kind: 'futures', direction: 'LONG', action: 'CLOSE', price: futuresPrice, quantity: '0.1' });
    await submit(pages.bob, { kind: 'futures', direction: 'SHORT', action: 'CLOSE', price: futuresPrice, quantity: '0.1' });
    const closedAlice = await clientGet('/api/v1/trading/futures/workspace?market=BTC-USDT', 'alice');
    const closedBob = await clientGet('/api/v1/trading/futures/workspace?market=BTC-USDT', 'bob');
    assert.equal(closedAlice.positions.length, 0);
    assert.equal(closedBob.positions.length, 0);
    assert.equal(closedAlice.fills.length, 2);
    assert.equal(closedBob.fills.length, 2);
    assert.deepEqual(errors, []);
    console.log(JSON.stringify({ spotTwoAccounts: true, futuresOpenCloseTwoAccounts: true,
      futuresReference: reference.source, browser: 'Google Chrome', pageErrors: errors }));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
