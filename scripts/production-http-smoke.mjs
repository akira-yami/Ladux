import { createHmac, randomBytes } from 'node:crypto';
import assert from 'node:assert/strict';

// Run only inside the disposable ladux-audit-smoke Docker network, using fake keys.
if (process.env.LADUX_SMOKE_API !== 'http://app:8080' || process.env.DB_USERNAME !== 'ladux_fixture'
    || process.env.JWT_SECRET !== Buffer.alloc(32, 42).toString('base64')) {
  throw new Error('This script is restricted to the isolated fixture stack');
}
const origin = process.env.LADUX_SMOKE_API;
const completed = [];
function jwt(username, id, role) {
  const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  const data = `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode({ sub: username, userId: id, roles: [role], type: 'access', tokenVersion: 0, iat: now, exp: now + 900 })}`;
  return `${data}.${createHmac('sha256', Buffer.from(process.env.JWT_SECRET, 'base64')).update(data).digest('base64url')}`;
}
const admin = jwt(process.env.BOOTSTRAP_ADMIN_USERNAME, 1, 'ADMIN');
async function call(method, route, data, token, expected = 200) {
  const response = await fetch(origin + route, { method, signal: AbortSignal.timeout(30000),
    headers: { Accept: 'application/json', ...(data === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: data === undefined ? undefined : JSON.stringify(data) });
  const text = await response.text();
  assert.equal(response.status, expected, `${method} ${route}: ${response.status} ${text.slice(0, 500)}`);
  completed.push(`${method} ${route}: ${expected}`);
  return text ? JSON.parse(text) : null;
}

await call('GET', '/actuator/health/readiness');
await call('GET', '/api/v1/admin/payments', undefined, undefined, 401);
await call('GET', '/actuator/prometheus', undefined, undefined, 401);
const metrics = await fetch(origin + '/actuator/prometheus', { headers: { Authorization: `Bearer ${admin}` } });
assert.equal(metrics.status, 200);
assert.ok((await metrics.text()).includes('jvm_memory_used_bytes'));
completed.push('Authenticated Prometheus metrics: 200');
const unsigned = await call('GET', '/api/v1/payments/vnpay-webhook');
assert.equal(unsigned.RspCode, '97');
const colors = await call('GET', '/api/v1/admin/colors', undefined, admin);
const adminProfile = await call('GET', '/api/v1/users/me', undefined, admin);
await call('DELETE', `/api/v1/admin/users/${adminProfile.id}`, undefined, admin, 400);
const color = colors.content[0] ?? await call('POST', '/api/v1/admin/colors', { name: 'Smoke ' + Date.now(), hexCode: '#' + randomBytes(3).toString('hex') }, admin, 201);
const suffix = Date.now().toString();
const brand = await call('POST', '/api/v1/admin/brands', { name: 'Smoke brand ' + suffix }, admin, 201);
const category = await call('POST', '/api/v1/admin/categories', { name: 'Smoke category ' + suffix }, admin, 201);
const product = await call('POST', '/api/v1/admin/products', {
  brandId: brand.id, categoryId: category.id, name: 'Smoke laptop ' + suffix,
  variants: [{ colorId: color.id, ram: '16GB', rom: '512GB', price: 20000000, discountPrice: 19000000, stockQuantity: 2, isActive: true }],
}, admin, 201);
const variantId = product.variants[0].id;
const username = 'smoke_' + suffix;
const account = await call('POST', '/api/v1/auth/register', { fullName: 'Smoke customer', username, password: 'FixturePassword123!' }, undefined, 201);
const customer = jwt(username, account.id, 'CUSTOMER');
await call('GET', '/api/v1/admin/payments', undefined, customer, 403);
await call('GET', '/api/v1/users/me', undefined, customer);
await call('POST', '/api/v1/cart/items', { productId: variantId, quantity: 1 }, customer, 201);
const cart = await call('GET', '/api/v1/cart', undefined, customer);
assert.equal(Number(cart.totalPrice), 19000000);
const checkout = { paymentProvider: 'COD', expectedAmount: 19030000,
  shippingAddress: { receiverName: 'Smoke customer', phone: '0912345678', street: 'Fixture street', ward: 'Fixture ward', district: 'Fixture district', city: 'Fixture city' } };
await call('POST', '/api/v1/orders', { ...checkout, expectedAmount: 1 }, customer, 400);
assert.equal((await call('GET', '/api/v1/cart', undefined, customer)).items.length, 1);
const order = await call('POST', '/api/v1/orders', checkout, customer, 201);
assert.equal(Number(order.finalAmount), 19030000);
assert.equal((await call('GET', '/api/v1/cart', undefined, customer)).items.length, 0);
await call('PATCH', `/api/v1/admin/orders/${order.id}/status`, { status: 'CONFIRMED' }, admin);
await call('PATCH', `/api/v1/admin/orders/${order.id}/status`, { status: 'SHIPPED' }, admin);
await call('PATCH', `/api/v1/admin/orders/${order.id}/status`, { status: 'DELIVERED' }, admin, 400);
const payment = (await call('GET', `/api/v1/admin/payments?orderId=${order.id}`, undefined, admin)).content[0];
await call('PUT', `/api/v1/admin/payments/${payment.id}`, { orderId: order.id, provider: 'COD', status: 'SUCCESS', transactionNo: 'SMOKE-COD-' + suffix }, admin);
await call('PATCH', `/api/v1/admin/orders/${order.id}/status`, { status: 'DELIVERED' }, admin);
const deliveredProfile = await call('GET', '/api/v1/users/me', undefined, customer);
assert.equal(Number(deliveredProfile.totalSpent), 19030000);
await call('POST', `/api/v1/orders/${order.id}/request-return`, { reason: 'Fixture return' }, customer);
await call('PATCH', `/api/v1/admin/orders/${order.id}/status`, { status: 'RETURNED' }, admin);
const refund = await call('POST', `/api/v1/admin/payments/order/${order.id}/refund`, { paymentId: payment.id, reason: 'Fixture manual cash returned' }, admin);
assert.equal(refund.status, 'REFUNDED');
assert.equal(Number((await call('GET', '/api/v1/users/me', undefined, customer)).totalSpent), 0);
const finalProduct = await call('GET', `/api/v1/products/${product.id}`);
assert.equal(finalProduct.variants.find(value => value.id === variantId).stockQuantity, 2);
console.log(JSON.stringify({ passed: completed.length, completed, orderId: order.id, productId: product.id,
  note: 'Fixture JWTs exercise HTTP authorization/business flows; real login/CAPTCHA/OAuth/SMTP/VNPay are not exercised.' }, null, 2));
