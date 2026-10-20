const { chromium } = require('C:/Users/ADMIN/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs/promises');
(async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const results = [], errors = [], remoteFonts = [];
  try {
    for (const dark of [true, false]) {
      const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
      await context.addInitScript(dark => localStorage.setItem('ladux-theme', dark ? 'dark' : 'light'), dark);
      await context.route(/https:\/\/fonts\.(googleapis|gstatic)\.com\//, route => { remoteFonts.push(route.request().url()); return route.abort(); });
      const page = await context.newPage(); page.on('pageerror', error => errors.push(error.message));
      await page.goto('http://localhost:5173/products', { waitUntil: 'networkidle' });
      await page.getByRole('textbox', { name: 'Tìm nhanh', exact: true }).waitFor();
      await page.evaluate(() => document.fonts.ready);
      const result = await page.locator('#catalog-filters > div').first().evaluate(section => {
        const label = section.querySelector('label'), input = section.querySelector('input');
        return { background: getComputedStyle(section).backgroundColor, radius: getComputedStyle(section).borderRadius, labelFont: getComputedStyle(label).fontFamily, inputFont: getComputedStyle(input).fontFamily, labelHeight: label.getBoundingClientRect().height, labelFontSize: parseFloat(getComputedStyle(label).fontSize), localFontLoaded: [...document.fonts].some(face => face.family.replaceAll('"', '') === 'Inter' && face.status === 'loaded') };
      });
      assert.equal(result.background, 'rgba(0, 0, 0, 0)'); assert.equal(result.radius, '0px');
      assert.match(result.labelFont, /^Inter/); assert.match(result.inputFont, /^Inter/);
      assert.ok(result.labelHeight >= result.labelFontSize); assert.ok(result.localFontLoaded);
      await page.locator('#catalog-filters > div').first().screenshot({ path: path.join(__dirname, 'filter-fixed-production-' + (dark ? 'dark' : 'light') + '.png') });
      results.push({ theme: dark ? 'dark' : 'light', ...result });
      await page.setViewportSize({ width: 390, height: 844 });
      await page.getByRole('button', { name: 'Bộ lọc sản phẩm' }).click();
      await page.getByRole('textbox', { name: 'Tìm nhanh', exact: true }).waitFor();
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false);
      assert.equal(await page.locator('a[title="Hỗ trợ qua Zalo"]').isVisible(), false);
      assert.equal(await page.locator('a[title="Hỗ trợ qua Telegram"]').isVisible(), false);
      await context.close();
    }
    assert.deepEqual(errors, []); assert.deepEqual(remoteFonts, []);
    await fs.writeFile(path.join(__dirname, 'production-font-result.json'), JSON.stringify({ passed: true, results, errors, remoteFonts }, null, 2));
    console.log('PASS: production filters, local Vietnamese font, light/dark, mobile, no Google Fonts dependency');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
