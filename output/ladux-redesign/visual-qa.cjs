// Local visual QA. Admin responses are isolated browser fixtures; no server auth is changed.
const { chromium } = require('C:/Users/ADMIN/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const base = process.env.LADUX_PREVIEW_URL || 'http://localhost:5173';
const out = __dirname;
const pageData = (content, total = content.length) => ({ content, totalElements: total, totalPages: Math.ceil(total / 20), size: 20, number: 0, first: true, last: true, empty: content.length === 0 });
const orders = [
  { id: 1048, userId: 'KH-021', finalAmount: 32990000, status: 'PENDING', createdAt: '2026-10-20T09:35:00' },
  { id: 1047, userId: 'KH-018', finalAmount: 24990000, status: 'CONFIRMED', createdAt: '2026-10-20T09:12:00' },
  { id: 1046, userId: 'KH-016', finalAmount: 19990000, status: 'SHIPPED', createdAt: '2026-10-20T08:40:00' },
  { id: 1045, userId: 'KH-009', finalAmount: 42990000, status: 'DELIVERED', createdAt: '2026-10-19T16:25:00' },
  { id: 1044, userId: 'KH-012', finalAmount: 28990000, status: 'DELIVERED', createdAt: '2026-10-19T14:10:00' },
];
const products = [{ id: 101, name: 'Ladux Studio 14', slug: 'ladux-studio-14', description: 'Laptop sáng tạo', brand: { id: 1, name: 'Ladux' }, category: { id: 1, name: 'Laptop' }, images: [], variants: [{ id: 1, ram: '16GB', rom: '512GB', price: 24990000, stockQuantity: 3 }], active: true }];
function adminFixture(url) {
  const p = url.pathname;
  if (p.endsWith('/admin/auth/me')) return { id: 'qa-user', fullName: 'Nguyễn Minh', username: 'minh', roles: ['ROLE_ADMIN'], avatar: null };
  if (p.includes('/notifications')) return pageData([]);
  if (p.includes('/orders/status/')) return pageData([], { PENDING: 12, CONFIRMED: 23, SHIPPED: 18 }[p.split('/').pop()] || 0);
  if (p.includes('/orders')) return pageData(orders, 128);
  if (p.includes('/purchase-orders')) return pageData([{ id: 48, supplierId: 1, supplierName: 'Ladux Distribution', totalAmount: 215000000, status: 'PENDING' }, { id: 47, supplierId: 2, supplierName: 'Tech Supply', totalAmount: 136000000, status: 'RECEIVED' }], 16);
  if (p.includes('/payments')) return pageData([], 7);
  if (p.includes('/customers')) return pageData([], 246);
  if (p.includes('/users')) return pageData([], 271);
  if (p.includes('/products')) return pageData(products, 64);
  if (p.includes('/suppliers')) return pageData([], 8);
  if (p.includes('/brands')) return pageData([{ id: 1, name: 'Ladux', slug: 'ladux' }]);
  if (p.includes('/categories')) return pageData([{ id: 1, name: 'Laptop', slug: 'laptop' }]);
  return pageData([]);
}
async function installRoutes(context, admin = false) {
  await context.route('http://localhost:8080/**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = { 'access-control-allow-origin': base, 'access-control-allow-credentials': 'true', 'access-control-allow-headers': request.headers()['access-control-request-headers'] || 'authorization,content-type', 'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS' };
    if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers });
    if (admin) return route.fulfill({ status: 200, contentType: 'application/json', headers, body: JSON.stringify(adminFixture(url)) });
    if (url.pathname.includes('/auth/') || url.pathname.endsWith('/users/me')) return route.fulfill({ status: 401, contentType: 'application/json', headers, body: JSON.stringify({ message: 'QA anonymous session' }) });
    try {
      const response = await route.fetch({ headers: { ...request.headers(), origin: 'http://localhost:5173' }, timeout: 20000 });
      return route.fulfill({ response, headers: { ...response.headers(), ...headers } });
    } catch (error) { return route.fulfill({ status: 503, contentType: 'application/json', headers, body: JSON.stringify({ message: error.message }) }); }
  });
}
async function capture(page, name) {
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: path.join(out, name + '.png'), animations: 'disabled' });
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1);
  assert.equal(overflow, false, name + ': horizontal overflow');
  console.log('PASS', name);
}
async function setTheme(page, value) {
  const current = await page.locator('html').getAttribute('data-theme');
  if (current !== value) await page.getByRole('button', { name: value === 'dark' ? 'Bật giao diện tối' : 'Bật giao diện sáng', exact: true }).click();
  assert.equal(await page.locator('html').getAttribute('data-theme'), value);
}
async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const errors = [];
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'vi-VN', timezoneId: 'Asia/Bangkok' });
  await installRoutes(context);
  const page = await context.newPage();
  page.on('pageerror', error => errors.push(error.message));
  await page.goto(base + '/');
  await page.locator('#store-hero-title').waitFor();
  await page.locator('.store-card').first().waitFor({ timeout: 30000 });
  await page.locator('.store-hero__image').evaluate(image => image.decode());
  await capture(page, 'storefront-light');
  await page.getByRole('button', { name: 'Tìm cùng trợ lý AI' }).click();
  await page.getByRole('heading', { name: 'Hỗ trợ trực tuyến' }).waitFor();
  await page.keyboard.press('Escape');
  await setTheme(page, 'dark');
  await capture(page, 'storefront-dark');
  await page.reload();
  await page.locator('#store-hero-title').waitFor();
  assert.equal(await page.locator('html').getAttribute('data-theme'), 'dark', 'Theme persists after reload');
  await page.getByRole('link', { name: 'Sản phẩm', exact: true }).click();
  await page.locator('.store-card').first().waitFor();
  await capture(page, 'products-dark');
  await page.goto(base + '/');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.locator('#store-hero-title').waitFor();
  await setTheme(page, 'light');
  await capture(page, 'storefront-mobile');
  await page.goto(base + '/login');
  await page.getByRole('heading', { name: 'Đăng nhập', exact: true }).waitFor();
  await page.setViewportSize({ width: 1440, height: 1000 });
  await capture(page, 'user-login-light');
  await setTheme(page, 'dark');
  await capture(page, 'user-login-dark');
  await setTheme(page, 'light');
  await page.goto(base + '/admin/dashboard');
  await page.waitForURL('**/admin/login');
  await page.getByRole('heading', { name: 'Chào mừng trở lại.' }).waitFor();
  await page.setViewportSize({ width: 1440, height: 1000 });
  await capture(page, 'admin-login-light');
  await page.getByRole('button', { name: 'Đăng nhập workspace' }).click();
  await page.getByText('Vui lòng nhập tên đăng nhập', { exact: true }).waitFor();
  await setTheme(page, 'dark');
  await capture(page, 'admin-login-dark');
  const adminContext = await browser.newContext({ viewport: { width: 1440, height: 1050 }, locale: 'vi-VN', timezoneId: 'Asia/Bangkok' });
  await installRoutes(adminContext, true);
  const admin = await adminContext.newPage();
  admin.on('pageerror', error => errors.push(error.message));
  await admin.goto(base + '/admin/dashboard');
  await admin.getByRole('heading', { name: 'Tổng quan vận hành', exact: true }).waitFor();
  await admin.locator('.dashboard-donut').waitFor();
  await capture(admin, 'admin-dashboard-light');
  await setTheme(admin, 'dark');
  await capture(admin, 'admin-dashboard-dark');
  await admin.getByRole('button', { name: 'Thu gọn sidebar' }).click();
  assert.ok(await admin.locator('.admin-workspace').evaluate(element => element.classList.contains('sidebar-collapsed')));
  await admin.getByRole('button', { name: 'Mở rộng sidebar' }).click();
  await admin.getByRole('textbox', { name: 'Tìm trang quản trị' }).fill('Sản phẩm');
  await admin.locator('.admin-search-results a').first().click();
  await admin.waitForURL('**/admin/products');
  await capture(admin, 'admin-products-dark');
  await admin.goto(base + '/admin/products/new');
  await admin.getByRole('heading').first().waitFor();
  await capture(admin, 'admin-product-editor-dark');
  await admin.goto(base + '/admin/dashboard');
  await admin.setViewportSize({ width: 390, height: 844 });
  await admin.getByRole('heading', { name: 'Tổng quan vận hành', exact: true }).waitFor();
  await capture(admin, 'admin-mobile-dark');
  await admin.getByRole('button', { name: 'Mở menu', exact: true }).click();
  await admin.getByRole('dialog').waitFor();
  await admin.keyboard.press('Escape');
  await admin.getByRole('dialog').waitFor({ state: 'hidden' });
  assert.deepEqual(errors, [], 'No uncaught browser errors');
  await fs.writeFile(path.join(out, 'qa-result.json'), JSON.stringify({ passed: true, uncaughtErrors: errors, adminUsesBrowserFixtures: true, checks: ['light/dark', 'theme persistence', 'mobile overflow', 'AI assistant', 'auth guard', 'login validation', 'admin sidebar', 'admin search', 'product editor', 'mobile dialog keyboard'] }, null, 2));
  await browser.close();
  console.log('ALL VISUAL CHECKS PASSED');
}
if (require.main === module) main().catch(error => { console.error(error); process.exit(1); });
module.exports = { installRoutes, adminFixture, products, orders, pageData };
