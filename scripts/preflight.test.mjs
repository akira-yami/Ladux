import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const template = fs.readFileSync(path.join(root, '.env.production.example'), 'utf8');
const fixture = template.split(/\r?\n/).map(line => {
  if (!line || line.startsWith('#') || !line.includes('=')) return line;
  const [key, ...rest] = line.split('=');
  let value = rest.join('=').replaceAll('shop.example.com', 'shop.ladux.vn').replaceAll('api.example.com', 'api.ladux.vn').replaceAll('@example.com', '@ladux.vn');
  if (value.startsWith('REPLACE_')) value = 'fake-fixture-value-for-tests-only';
  if (key === 'JWT_SECRET') value = Buffer.alloc(32, 42).toString('base64');
  if (key === 'VNPAY_REFUND_URL' || key === 'VNPAY_QUERY_URL') value = 'https://gateway.vnpay.vn/merchant_webapi/api/transaction';
  return `${key}=${value}`;
}).join('\n');

function run(env, extra = []) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'ladux-preflight-'));
  const file = path.join(directory, '.env.fixture');
  try {
    fs.writeFileSync(file, env);
    const result = spawnSync(process.execPath, [path.join(root, 'scripts/preflight.mjs'), '--env-file', file, '--skip-docker', ...extra], { encoding: 'utf8' });
    return { code: result.status, output: result.stdout + result.stderr };
  } finally {
    // directory is the exact private directory returned by mkdtemp above.
    fs.rmSync(directory, { recursive: true, force: true });
  }
}

test('valid configuration passes without exposing secrets', () => {
  const result = run(fixture);
  assert.equal(result.code, 0, result.output);
  assert.ok(!result.output.includes('fake-fixture-value-for-tests-only'));
});
test('template placeholders prevent deployment', () => { assert.notEqual(run(template).code, 0); });
test('weak JWT secret prevents deployment', () => { assert.notEqual(run(fixture.replace(/^JWT_SECRET=.*$/m, 'JWT_SECRET=weak')).code, 0); });
test('refund callback cannot point back to the shop', () => {
  const result = run(fixture.replace(/^VNPAY_REFUND_URL=.*$/m, 'VNPAY_REFUND_URL=https://shop.ladux.vn/api/refund'));
  assert.notEqual(result.code, 0);
});
test('duplicate secrets are rejected without printing either value', () => {
  const result = run(fixture + '\nDB_PASSWORD=do-not-print-this-secret');
  assert.notEqual(result.code, 0);
  assert.ok(!result.output.includes('do-not-print-this-secret'));
});
test('sandbox is opt-in for staging', () => {
  const sandbox = fixture.replace(/^VNPAY_PAY_URL=.*$/m, 'VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html');
  assert.notEqual(run(sandbox).code, 0);
  assert.equal(run(sandbox, ['--allow-sandbox']).code, 0);
});
