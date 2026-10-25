import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Local isolated container smoke only. These fixture values are public test data.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const target = path.join(root, 'output/.env.production.smoke');
const overrides = {
  LADUX_WEB_DOMAIN: 'shop.ladux.test', LADUX_API_DOMAIN: 'api.ladux.test', CADDY_EMAIL: 'ops@ladux.test',
  DB_USERNAME: 'ladux_fixture', DB_PASSWORD: 'fixture-database-password-only',
  JWT_SECRET: Buffer.alloc(32, 42).toString('base64'),
  OAUTH2_SUCCESS_REDIRECT: 'https://shop.ladux.test/auth/oauth2/success',
  OAUTH2_FAILURE_REDIRECT: 'https://shop.ladux.test/login?oauth2Error=true',
  CAPTCHA_EXPECTED_HOSTNAME: 'shop.ladux.test',
  VNPAY_RETURN_URL: 'https://shop.ladux.test/payment/vnpay/return',
  VNPAY_IPN_URL: 'https://api.ladux.test/api/v1/payments/vnpay-webhook',
  VNPAY_QUERY_URL: 'https://gateway.ladux.test/transaction', VNPAY_REFUND_URL: 'https://gateway.ladux.test/transaction',
  APP_CORS_ALLOWED_ORIGINS: 'https://shop.ladux.test',
  BOOTSTRAP_ADMIN_ENABLED: 'true', BOOTSTRAP_ADMIN_USERNAME: 'smoke_admin',
  BOOTSTRAP_ADMIN_EMAIL: 'owner@ladux.test', BOOTSTRAP_ADMIN_PASSWORD: 'fixture-admin-password-only',
  MAIL_USERNAME: 'fixture@ladux.test',
};
const template = fs.readFileSync(path.join(root, '.env.production.example'), 'utf8');
const fixture = template.split(/\r?\n/).map(line => {
  const match = line.match(/^([A-Z][A-Z0-9_]*)=(.*)$/);
  if (!match) return line;
  return `${match[1]}=${overrides[match[1]] ?? (match[2].startsWith('REPLACE_') ? 'fixture-test-value-only' : match[2])}`;
}).join('\n');
fs.mkdirSync(path.dirname(target), { recursive: true });
fs.writeFileSync(target, fixture, { mode: 0o600 });
console.log('Created output/.env.production.smoke with fake credentials for isolated smoke tests.');
