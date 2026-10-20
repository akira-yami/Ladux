// Full UI audit with browser-only fixtures for protected pages. No server writes.
process.env.LADUX_PREVIEW_URL ||= 'http://127.0.0.1:5180';
const base = process.env.LADUX_PREVIEW_URL;
const { chromium } = require('C:/Users/ADMIN/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const { adminFixture, pageData, orders } = require('./visual-qa.cjs');
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const out = path.join(__dirname, 'audit');
const user = { id: 1, fullName: 'Nguyễn Minh', username: 'qa-minh', email: 'qa@example.test', phone: '0901234567', roles: ['ROLE_CUSTOMER'], active: true, createdAt: '2026-01-10T09:00:00', emailVerified: true, phoneVerified: false };
const address = { id: 1, userId: 1, receiverName: 'Nguyễn Minh', phone: '0901234567', street: '12 Đường Kiểm Thử', ward: 'Phường 1', district: 'Quận 1', city: 'Hồ Chí Minh', isDefault: true };
let product;
const publicCache = new Map();
function orderData() { return { ...orders[0], shippingAddress: address, orderItems: [{ id: 1, orderId: orders[0].id, product, productVariantId: product.variants[0]?.id, quantity: 1, priceAtPurchase: 24990000 }], subTotal: 24990000, discountAmount: 0, shippingFee: 0, finalAmount: 24990000, trackingNumber: null, paymentExpiresAt: null, paymentProvider: 'COD', couponCode: null }; }
function fixture(p, admin) {
  if (p.endsWith('/auth/refresh')) return { accessToken: 'browser-fixture-only' };
  if (p.endsWith('/admin/auth/me')) return { ...user, roles: ['ROLE_ADMIN'] };
  if (p.endsWith('/users/me')) return user;
  if (admin) {
    if (p.endsWith('/roles')) return [{ id: 1, name: 'ADMIN' }, { id: 2, name: 'CUSTOMER' }];
    if (p.match(/\/products\/\d+$/)) return product;
    if (p.match(/\/orders\/\d+$/)) return orderData();
    if (p.match(/\/purchase-orders\/\d+$/)) return { id: 48, supplierId: 1, supplierName: 'Nhà cung cấp QA', totalAmount: 0, status: 'PENDING', items: [], createdAt: '2026-10-20T09:00:00' };
    if (p.match(/\/customers\/\d+$/)) return { id: 1, user, fullName: user.fullName, level: 'REGULAR', totalSpent: 0 };
    if (p.match(/\/users\/\d+$/)) return user;
    if (p.match(/\/suppliers\/\d+$/)) return { id: 1, name: 'Nhà cung cấp QA', email: 'supplier@example.test', phone: '0901234567', active: true };
    if (p.match(/\/payments\/\d+$/)) return { id: 1, orderId: 1048, amount: 24990000, provider: 'COD', status: 'PENDING', createdAt: '2026-10-20T09:00:00' };
    if (p.includes('/product-suppliers/')) return [];
    if (p.includes('/products')) return pageData([product], 1);
    if (p.includes('/orders') && !p.includes('/status/')) return pageData([orderData()], 1);
    return adminFixture(new URL('http://localhost' + p));
  }
  if (p.endsWith('/cart')) return { id: 1, userId: 1, items: [{ id: 1, product, productVariant: product.variants[0], quantity: 1 }], totalPrice: product.variants[0]?.price || 24990000, shippingFee: 0 };
  if (p.includes('/wishlists')) return [{ id: 1, product }];
  if (p.includes('/user-addresses/default')) return [address];
  if (p.includes('/user-addresses/user')) return [address];
  if (p.includes('/orders/user')) return pageData([orderData()]);
  if (p.includes('/order-histories')) return pageData([]);
  if (p.includes('/notifications/unread-count')) return 0;
  if (p.includes('/notifications')) return pageData([]);
  return undefined;
}
async function routes(context, admin) {
  await context.route('http://localhost:8080/**', async route => {
    const request = route.request(); const url = new URL(request.url());
    const headers = { 'access-control-allow-origin': base, 'access-control-allow-credentials': 'true', 'access-control-allow-headers': request.headers()['access-control-request-headers'] || 'authorization,content-type', 'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS' };
    if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers });
    const data = url.pathname.startsWith('/api/') ? fixture(url.pathname, admin) : undefined;
    if (data !== undefined) return route.fulfill({ status: 200, contentType: 'application/json', headers, body: JSON.stringify(data) });
    if (request.method() !== 'GET') return route.fulfill({ status: 400, contentType: 'application/json', headers, body: '{"message":"No server writes during visual audit"}' });
    const key = request.url();
    if (!publicCache.has(key)) {
      const response = await route.fetch({ headers: { ...request.headers(), origin: 'http://localhost:5173' }, timeout: 15000 });
      publicCache.set(key, { status: response.status(), body: await response.body(), headers: response.headers() });
    }
    const cached = publicCache.get(key);
    return route.fulfill({ ...cached, headers: { ...cached.headers, ...headers } });
  });
}
async function audit(page, label) {
  await page.evaluate(() => document.fonts.ready);
  const result = await page.evaluate(() => {
    const canvas = document.createElement('canvas'); canvas.width = canvas.height = 1;
    const painter = canvas.getContext('2d', { willReadFrequently: true }), colors = new Map();
    const parse = value => {
      if (colors.has(value)) return colors.get(value);
      painter.clearRect(0, 0, 1, 1); painter.fillStyle = value; painter.fillRect(0, 0, 1, 1);
      const bytes = [...painter.getImageData(0, 0, 1, 1).data], color = [...bytes.slice(0, 3), bytes[3] / 255]; colors.set(value, color); return color;
    };
    const blend = (fg, bg) => [...fg.slice(0, 3).map((x, i) => x * fg[3] + bg[i] * (1 - fg[3])), 1];
    const luminance = color => color.slice(0, 3).map(v => { v /= 255; return v <= .04045 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4; }).reduce((s, v, i) => s + v * [.2126, .7152, .0722][i], 0);
    const contrast = (a, b) => { const x = luminance(a), y = luminance(b); return (Math.max(x, y) + .05) / (Math.min(x, y) + .05); };
    const issues = [], fonts = new Set();
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    let node;
    while (node = walker.nextNode()) {
      const text = node.textContent.trim(), element = node.parentElement;
      if (!text || !element || element.closest('script,style,svg,code,pre,[aria-hidden="true"],[disabled]')) continue;
      const rect = element.getBoundingClientRect(), style = getComputedStyle(element);
      if (!rect.width || !rect.height || style.visibility !== 'visible' || style.opacity === '0') continue;
      fonts.add(style.fontFamily);
      const chain = []; let ancestor = element, invisible = false;
      while (ancestor) { const css = getComputedStyle(ancestor); if (css.opacity === '0' || css.visibility === 'hidden') invisible = true; chain.unshift(css.backgroundColor); ancestor = ancestor.parentElement; }
      if (invisible) continue;
      const background = chain.reduce((color, value) => blend(parse(value), color), [255, 255, 255, 1]);
      const foreground = blend(parse(style.color), background);
      const ratio = contrast(foreground, background);
      const large = parseFloat(style.fontSize) >= 24 || (parseFloat(style.fontSize) >= 18.66 && Number(style.fontWeight) >= 700);
      if (ratio < (large ? 3 : 4.5) - .1) issues.push({ text: text.slice(0, 72), ratio: +ratio.toFixed(2), color: style.color, background: background.slice(0, 3).map(Math.round), font: style.fontSize, class: element.className });
    }
    return { overflow: document.documentElement.scrollWidth > innerWidth + 1, fonts: [...fonts], fontLoaded: [...document.fonts].some(face => face.family.replaceAll('"', '') === 'Inter' && face.status === 'loaded') && document.fonts.check('16px Inter', 'Tiếng Việt: Nguyễn Đặng Trường'), contrast: issues };
  });
  assert.ok(result.fontLoaded, label + ': local Vietnamese font loads');
  assert.equal(result.overflow, false, label + ': horizontal overflow');
  return { label, ...result };
}
async function main() {
  await fs.mkdir(out, { recursive: true });
  const response = await fetch('http://localhost:8080/api/v1/products?page=0&size=1');
  product = (await response.json()).content[0]; assert.ok(product?.variants?.length, 'Public product fixture');
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const results = [], errors = [];
  const userPaths = ['/', '/products', '/products/' + product.id, '/about', '/contact', '/login', '/register', '/cart', '/checkout', '/orders', '/wishlist', '/account', '/addresses'];
  const adminPaths = ['/admin/dashboard', '/admin/orders', '/admin/orders/1048', '/admin/payments', '/admin/payments/1', '/admin/order-histories', '/admin/order-items', '/admin/products', '/admin/products/new', '/admin/products/' + product.id, '/admin/products/' + product.id + '/edit', '/admin/brands', '/admin/categories', '/admin/colors', '/admin/coupons', '/admin/customers', '/admin/users', '/admin/user-addresses', '/admin/reviews', '/admin/suppliers', '/admin/product-suppliers', '/admin/purchase-orders', '/admin/purchase-orders/new', '/admin/purchase-orders/48', '/admin/stock-movements', '/admin/notifications', '/admin/chatbot-index'];
  try {
    for (const theme of ['light', 'dark']) for (const mobile of [false, true]) {
      for (const admin of [false, true]) {
        const context = await browser.newContext({ viewport: mobile ? { width: 390, height: 844 } : { width: 1440, height: 1000 }, locale: 'vi-VN', timezoneId: 'Asia/Bangkok' });
        await context.addInitScript(theme => localStorage.setItem('ladux-theme', theme), theme);
        await routes(context, admin);
        const page = await context.newPage(); let currentPath = '';
        page.on('pageerror', error => errors.push({ path: currentPath, theme, mobile, error: error.message }));
        for (const routePath of admin ? adminPaths : userPaths) {
          currentPath = routePath;
          await page.goto(base + routePath, { waitUntil: 'networkidle' });
          await page.locator('main').first().waitFor();
          const label = (admin ? 'admin' : 'user') + '-' + (routePath.replaceAll('/', '-').slice(1) || 'home') + '-' + theme + (mobile ? '-mobile' : '-desktop');
          results.push(await audit(page, label));
          if (['/products', '/account', '/checkout', '/cart', '/admin/dashboard', '/admin/products/new'].includes(routePath)) await page.screenshot({ path: path.join(out, label + '.png'), animations: 'disabled' });
          if (routePath === '/products') {
            if (mobile) await page.getByRole('button', { name: 'Bộ lọc sản phẩm' }).click();
            const search = page.getByRole('textbox', { name: 'Tìm nhanh', exact: true }); await search.waitFor();
            const regression = await search.evaluate(input => {
              const section = input.closest('#catalog-filters').firstElementChild;
              return { background: getComputedStyle(section).backgroundColor, font: getComputedStyle(document.querySelector('label[for="catalog-search"]')).fontFamily, radius: getComputedStyle(section).borderRadius };
            });
            assert.equal(regression.background, 'rgba(0, 0, 0, 0)', 'Filter must not inherit product image background');
            assert.equal(regression.radius, '0px', 'Filter must not inherit product image rounding');
            assert.match(regression.font, /Inter/, 'Filter label uses the same font');
            await page.locator('#catalog-filters > div').first().scrollIntoViewIfNeeded();
            await page.evaluate(() => window.scrollBy(0, -160));
            await page.locator('#catalog-filters > div').first().screenshot({ path: path.join(out, 'filters-' + theme + (mobile ? '-mobile' : '-desktop') + '.png') });
          }
          console.log('AUDIT', label);
        }
        if (admin) {
          await page.goto(base + '/admin/brands', { waitUntil: 'networkidle' });
          await page.getByRole('button', { name: /Tạo mới|Thêm/ }).first().click();
          await page.getByRole('dialog').waitFor();
          results.push(await audit(page, 'brand-dialog-' + theme + (mobile ? '-mobile' : '-desktop')));
          await page.screenshot({ path: path.join(out, 'brand-dialog-' + theme + (mobile ? '-mobile' : '-desktop') + '.png') });
          await page.keyboard.press('Escape');
          if (!mobile) {
            await page.getByRole('button', { name: 'Tạo mới', exact: true }).hover();
            await page.waitForTimeout(200);
            const hover = await audit(page, 'admin-primary-hover-' + theme);
            assert.ok(!hover.contrast.some(issue => issue.text === 'Tạo mới'), 'Primary button must remain legible on hover');
            results.push(hover);
          }
        }
        await context.close();
      }
      await fs.writeFile(path.join(out, 'audit-report.json'), JSON.stringify({ views: results.length, errors, results }, null, 2));
    }
    assert.deepEqual(errors, [], 'No uncaught errors in audited routes');
    console.log('AUDITED', results.length, 'views');
  } finally { await browser.close(); await fs.writeFile(path.join(out, 'audit-report.json'), JSON.stringify({ views: results.length, errors, results }, null, 2)); }
}
main().catch(error => { console.error(error); process.exit(1); });
