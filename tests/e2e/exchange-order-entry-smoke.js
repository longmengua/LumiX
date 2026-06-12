// Verifies the prod exchange order-entry UX without requiring a running Spring app.
const { chromium } = require('@playwright/test');
const path = require('path');

const PAGE_URL = `file://${path.resolve('src/main/resources/static/exchange.html')}`;

async function run() {
  const browser = await chromium.launch({ headless: true });
  try {
    await verifyDesktopOrderEntry(browser);
    await verifyMobileLayout(browser);
  } finally {
    await browser.close();
  }
}

async function verifyDesktopOrderEntry(browser) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });
  try {
    await page.goto(PAGE_URL, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(500);
    await page.click('#placeBuy');
    await page.waitForTimeout(300);

    const result = await page.evaluate(() => {
      const book = document.querySelector('.book-panel').getBoundingClientRect();
      const order = document.querySelector('.order-entry').getBoundingClientRect();
      const headers = [...document.querySelectorAll('#orderBook th')].map((el) => el.textContent);
      return {
        profileHidden: document.querySelector('#profilePanel').hidden,
        authCardHidden: document.querySelector('#authCard').hidden,
        activeId: document.activeElement.id,
        bookHeight: Math.round(book.height),
        orderHeight: Math.round(order.height),
        heightDelta: Math.abs(Math.round(book.height - order.height)),
        orderErrorHidden: document.querySelector('#orderError').hidden,
        orderError: document.querySelector('#orderError').textContent,
        headers,
        scrollWidth: document.documentElement.scrollWidth,
        innerWidth: window.innerWidth
      };
    });

    assert(!result.profileHidden, 'profile drawer should open for unauthenticated order entry');
    assert(!result.authCardHidden, 'auth card should be visible in the profile drawer');
    assert(result.activeId === 'authEmail', 'email field should receive focus after prompting login');
    assert(result.heightDelta <= 2, `book/order panels should have matching row height, got ${result.bookHeight}/${result.orderHeight}`);
    assert(!result.orderErrorHidden, 'order error should be visible after unauthenticated order entry');
    assert(result.orderError.includes('Please login'), `unexpected order error: ${result.orderError}`);
    assert(result.headers.includes('Order Value'), `order-book value header should be user-facing, got ${result.headers.join(', ')}`);
    assert(result.scrollWidth <= result.innerWidth + 2, `desktop layout overflowed: ${result.scrollWidth} > ${result.innerWidth}`);

    console.log(`desktop ok: profile open, row height ${result.bookHeight}/${result.orderHeight}, headers ${result.headers.join(' | ')}`);
  } finally {
    await page.close();
  }
}

async function verifyMobileLayout(browser) {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  try {
    await page.goto(PAGE_URL, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(500);
    await page.click('#placeBuy');
    await page.waitForTimeout(300);

    const result = await page.evaluate(() => ({
      profileHidden: document.querySelector('#profilePanel').hidden,
      authCardHidden: document.querySelector('#authCard').hidden,
      scrollWidth: document.documentElement.scrollWidth,
      innerWidth: window.innerWidth
    }));

    assert(!result.profileHidden, 'mobile profile drawer should open for unauthenticated order entry');
    assert(!result.authCardHidden, 'mobile auth card should be visible in the profile drawer');
    assert(result.scrollWidth <= result.innerWidth + 2, `mobile layout overflowed: ${result.scrollWidth} > ${result.innerWidth}`);

    console.log(`mobile ok: profile open, width ${result.innerWidth}/${result.scrollWidth}`);
  } finally {
    await page.close();
  }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

run().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
