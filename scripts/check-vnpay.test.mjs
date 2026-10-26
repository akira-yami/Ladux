import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { buildSandboxProbe, checkConfiguration, classifyGatewayPage, probeSandbox } from './check-vnpay.mjs';

const merchant = { VNPAY_TMN_CODE: 'TESTTMN1', VNPAY_HASH_SECRET: 'test-secret' };

test('missing settings use Spring Sandbox defaults; explicitly empty URLs are rejected', () => {
  const valid = checkConfiguration(merchant);
  assert.deepEqual(valid.errors, []);
  assert.equal(valid.isSandbox, true);
  assert.ok(valid.warnings.some(warning => warning.includes('localhost')));
  assert.ok(checkConfiguration({ ...merchant, VNPAY_PAY_URL: '' }).errors.some(error => error.includes('VNPAY_PAY_URL')));
  assert.ok(checkConfiguration({ ...merchant, VNPAY_QUERY_URL: '' }).errors.some(error => error.includes('VNPAY_QUERY_URL')));
});

test('rejects placeholder credentials, mixed environments and gateway URLs pointing at Ladux', () => {
  assert.ok(checkConfiguration({ ...merchant, VNPAY_HASH_SECRET: 'REPLACE_WITH_SECRET' }).errors.some(error => error.includes('VNPAY_HASH_SECRET')));
  assert.ok(checkConfiguration({ ...merchant, VNPAY_TMN_CODE: ' TESTTMN1' }).errors.length);
  const live = checkConfiguration({ ...merchant, VNPAY_PAY_URL: 'https://pay.vnpay.vn/vpcpay.html' });
  assert.ok(live.errors.some(error => error.includes('VNPAY_QUERY_URL')));
  assert.ok(live.errors.some(error => error.includes('VNPAY_RETURN_URL')));
  const local = checkConfiguration({ ...merchant, VNPAY_QUERY_URL: 'https://shop.ladux.test/query',
    VNPAY_RETURN_URL: 'https://shop.ladux.test/payment/vnpay/return' });
  assert.ok(local.errors.some(error => error.includes('không trỏ về Ladux')));
  assert.ok(checkConfiguration({ ...merchant, VNPAY_PAY_URL: 'https://sandbox.vnpayment.vn.evil.test/pay' }).errors.length);
});

test('builds a correctly encoded and signed diagnostic with Vietnam dates and no real order', () => {
  const settings = checkConfiguration(merchant).settings;
  const url = buildSandboxProbe(settings, new Date('2026-10-26T17:02:24Z'));
  assert.equal(url.searchParams.get('vnp_CreateDate'), '20261027000224');
  assert.equal(url.searchParams.get('vnp_ExpireDate'), '20261027000724');
  assert.equal(url.searchParams.get('vnp_Amount'), '1000000');
  assert.match(url.searchParams.get('vnp_TxnRef'), /^LDXDIAG[A-Za-z0-9]+$/);
  assert.equal(url.searchParams.get('vnp_ReturnUrl'), settings.VNPAY_RETURN_URL);
  const unsigned = url.search.slice(1).split('&vnp_SecureHash=')[0];
  assert.ok(unsigned.includes('vnp_OrderInfo=Kiem+tra+cau+hinh+Ladux'));
  assert.equal(url.searchParams.get('vnp_SecureHash'), createHmac('sha512', merchant.VNPAY_HASH_SECRET).update(unsigned).digest('hex'));
  assert.ok(!url.href.includes(merchant.VNPAY_HASH_SECRET));
});

test('recognizes merchant approval errors in both languages without treating arbitrary HTML as success', () => {
  assert.equal(classifyGatewayPage('<h1>Website này chưa được phê duyệt</h1>'), 'MERCHANT_NOT_APPROVED');
  assert.equal(classifyGatewayPage('<div>The terminal (website) not approved</div>'), 'MERCHANT_NOT_APPROVED');
  assert.equal(classifyGatewayPage('Invalid checksum'), 'INVALID_SIGNATURE');
  assert.equal(classifyGatewayPage('Sai chữ ký'), 'INVALID_SIGNATURE');
  assert.equal(classifyGatewayPage('<h1>502 Bad Gateway</h1>'), 'UNCONFIRMED');
  assert.equal(classifyGatewayPage('<h1>Select your bank</h1>'), 'UNCONFIRMED');
});

test('never probes live production or follows redirects outside the Sandbox', async t => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    return new Response(null, { status: 302, headers: { location: 'https://other.example/pay' } });
  };
  await assert.rejects(probeSandbox({ ...merchant, VNPAY_PAY_URL: 'https://pay.vnpay.vn/vpcpay.html' }));
  assert.equal(calls, 0);
  await assert.rejects(probeSandbox(checkConfiguration(merchant).settings), /Redirect/);
  assert.equal(calls, 1);
});

test('retains Sandbox session cookies before recognizing the payment selection page', async t => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  const page = new URL('https://sandbox.vnpayment.vn/paymentv2/Transaction/PaymentMethod.html');
  const html = '<title>Chọn phương thức thanh to&#225;n (Test)</title>';
  assert.equal(classifyGatewayPage(html, page), 'PAYMENT_PAGE_REACHED');
  assert.equal(classifyGatewayPage('<title>Error</title>', page), 'UNCONFIRMED');
  assert.equal(classifyGatewayPage(html, new URL('https://other.example/')), 'UNCONFIRMED');
  let calls = 0;
  globalThis.fetch = async (url, options) => {
    if (++calls === 1) {
      assert.equal(options.headers.Cookie, undefined);
      return new Response(null, { status: 302, headers: {
        location: page.href, 'set-cookie': 'session=fixture-session; Path=/; Secure; HttpOnly',
      } });
    }
    assert.equal(url.href, page.href);
    assert.equal(options.headers.Cookie, 'session=fixture-session');
    return new Response(html, { status: 200 });
  };
  assert.equal(await probeSandbox(checkConfiguration(merchant).settings), 'PAYMENT_PAGE_REACHED');
  assert.equal(calls, 2);
});

test('CLI rejects unknown flags and a missing file without printing a stack or secret', () => {
  const script = fileURLToPath(new URL('./check-vnpay.mjs', import.meta.url));
  for (const args of [['--unknown'], ['--config'], ['--config', 'does-not-exist.env']]) {
    const result = spawnSync(process.execPath, [script, ...args], { encoding: 'utf8' });
    assert.equal(result.status, 1, `${args.join(' ')}: ${result.stderr}`);
    assert.ok(!result.stderr.includes('Error:'));
    assert.ok(!result.stderr.includes(merchant.VNPAY_HASH_SECRET));
  }
});
