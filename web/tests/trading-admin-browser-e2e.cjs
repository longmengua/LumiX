const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { chromium } = require('playwright');

// 隔離資料庫的管理 session 只供本機頁面驗收，不含政策預設值或正式資金。
(async () => {
  const cookies = JSON.parse(readFileSync(join(tmpdir(), 'lumix-e2e-cookies.json'), 'utf8'));
  const browser = await chromium.launch({ headless: true, executablePath: process.env.LUMIX_CHROMIUM_PATH });
  const errors = [];
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    await context.addCookies([{ name: 'LUMIX_ADMIN_SESSION', value: cookies.admin, domain: '127.0.0.1', path: '/' }]);
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message));
    await page.goto('http://127.0.0.1:8089/admin/futures/configuration');
    await page.getByText(/合約風險政策|Futures risk policy/).waitFor({ timeout: 15000 });
    await page.getByText(/ACTIVE/).first().waitFor();
    assert.match(await page.locator('textarea').inputValue(), /"collateralAsset": "USDT"/);
    await page.goto('http://127.0.0.1:8089/admin/spot/policies');
    await page.getByText(/現貨交易政策|Spot trading policy/).waitFor();
    await page.waitForFunction(() => document.querySelector('textarea')?.value.includes('SPOT:BTCUSDT'));
    assert.match(await page.locator('textarea').inputValue(), /"instrumentId": "SPOT:BTCUSDT"/);
    assert.deepEqual(errors, []);
    console.log(JSON.stringify({ adminSpotPolicy: true, adminFuturesPolicy: true, browser: 'Google Chrome', pageErrors: errors }));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
