import { expect, test } from '@playwright/test';

test.describe('exchange responsive layout', () => {
  test('activity tabs and profile auth controls stay within the viewport', async ({ page }, testInfo) => {
    // Scenario: recurring UI edits must keep trading activity tabs and profile auth controls inside mobile/desktop viewports.
    await page.goto('/exchange.html');
    await page.locator('#profileToggle').click();
    await expect(page.locator('#rememberLogin')).toBeVisible();

    for (const tab of ['positions', 'tradeHistory', 'orders']) {
      await page.locator(`[data-segmented-tabs="tradingActivity"] [data-segmented-tab="${tab}"]`).click();
      const metrics = await page.evaluate(() => ({
        innerWidth: window.innerWidth,
        scrollWidth: document.documentElement.scrollWidth,
        bodyScrollWidth: document.body.scrollWidth,
        activityTabsWidth: document.querySelector('[data-segmented-tabs="tradingActivity"]')?.getBoundingClientRect().width || 0,
        profilePanelWidth: document.querySelector('#profilePanel')?.getBoundingClientRect().width || 0,
        rememberLoginWidth: document.querySelector('.remember-login')?.getBoundingClientRect().width || 0
      }));

      expect(metrics.scrollWidth, `${testInfo.project.name} ${tab} document width`).toBeLessThanOrEqual(metrics.innerWidth + 2);
      expect(metrics.bodyScrollWidth, `${testInfo.project.name} ${tab} body width`).toBeLessThanOrEqual(metrics.innerWidth + 2);
      if (testInfo.project.name.includes('desktop')) {
        expect(metrics.activityTabsWidth, 'desktop activity tabs should not become full-width').toBeLessThanOrEqual(430);
      }
      expect(metrics.profilePanelWidth, 'profile panel should fit viewport').toBeLessThanOrEqual(metrics.innerWidth);
      expect(metrics.rememberLoginWidth, 'keep-login row should fit profile panel').toBeLessThanOrEqual(metrics.profilePanelWidth);
    }
  });

  test('exchange admin shell and embedded tools stay within the viewport', async ({ page }, testInfo) => {
    // Scenario: the production admin entry owns navigation while embedded tools must not create nested horizontal overflow.
    await page.goto('/exchange-admin.html');

    for (const tabName of ['Test Funds', 'Market Config', 'Risk Parameters', 'Market Makers', 'DLQ']) {
      await page.getByRole('button', { name: tabName }).click();
      await expect(page.frameLocator('#adminFrame').locator('header')).toBeHidden();
      const metrics = await page.evaluate(() => ({
        innerWidth: window.innerWidth,
        scrollWidth: document.documentElement.scrollWidth,
        bodyScrollWidth: document.body.scrollWidth,
        tabsWidth: document.querySelector('.tabs')?.getBoundingClientRect().width || 0,
        frameWidth: document.querySelector('#adminFrame')?.getBoundingClientRect().width || 0
      }));

      expect(metrics.scrollWidth, `${testInfo.project.name} ${tabName} admin document width`).toBeLessThanOrEqual(metrics.innerWidth + 2);
      expect(metrics.bodyScrollWidth, `${testInfo.project.name} ${tabName} admin body width`).toBeLessThanOrEqual(metrics.innerWidth + 2);
      expect(metrics.tabsWidth, 'admin tabs should fit viewport').toBeLessThanOrEqual(metrics.innerWidth);
      expect(metrics.frameWidth, 'admin iframe should fit viewport').toBeLessThanOrEqual(metrics.innerWidth);
    }
  });
});
