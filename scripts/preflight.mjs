import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const args = process.argv.slice(2);
const envIndex = args.indexOf('--env-file');
const envFile = path.resolve(root, envIndex >= 0 ? args[envIndex + 1] : '.env.production');
const errors = [];
let content;
try { content = fs.readFileSync(envFile, 'utf8'); }
catch { console.error('Không đọc được file cấu hình production.'); process.exit(1); }
const settings = {};
for (const raw of content.split(/\r?\n/)) {
  const line = raw.trim();
  if (!line || line.startsWith('#')) continue;
  const match = line.match(/^([A-Z][A-Z0-9_]*)\s*=\s*(.*)$/);
  if (!match) { errors.push('File môi trường có dòng không hợp lệ.'); continue; }
  let value = match[2];
  if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) value = value.slice(1, -1);
  else value = value.replace(/\s+#.*$/, '').trim();
  if (settings[match[1]] !== undefined) errors.push(`Biến ${match[1]} bị khai báo lặp.`);
  settings[match[1]] = value;
}
const required = ['LADUX_WEB_DOMAIN', 'LADUX_API_DOMAIN', 'CADDY_EMAIL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET',
  'GOOGLE_CLIENT_ID', 'GOOGLE_CLIENT_SECRET', 'OAUTH2_SUCCESS_REDIRECT', 'OAUTH2_FAILURE_REDIRECT',
  'MAIL_USERNAME', 'MAIL_PASSWORD', 'CAPTCHA_SECRET', 'VITE_RECAPTCHA_SITE_KEY', 'VNPAY_TMN_CODE',
  'VNPAY_HASH_SECRET', 'VNPAY_PAY_URL', 'VNPAY_RETURN_URL', 'VNPAY_REFUND_URL', 'VNPAY_QUERY_URL',
  'VNPAY_IPN_URL', 'DEEPSEEK_API_KEY', 'GOOGLE_GENAI_API_KEY', 'APP_CORS_ALLOWED_ORIGINS'];
for (const key of required) {
  if (!settings[key] || /REPLACE_|\$\{|example\.com/i.test(settings[key])) errors.push(`Chưa cấu hình ${key}.`);
}
for (const key of ['LADUX_WEB_DOMAIN', 'LADUX_API_DOMAIN']) {
  if (!/^(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,}$/i.test(settings[key] ?? '')) errors.push(`${key} phải là hostname, không chứa scheme/path.`);
}
if (settings.LADUX_WEB_DOMAIN === settings.LADUX_API_DOMAIN) errors.push('Dùng hai hostname riêng cho WEB_DOMAIN và API_DOMAIN.');
const webOrigin = `https://${settings.LADUX_WEB_DOMAIN}`;
const apiOrigin = `https://${settings.LADUX_API_DOMAIN}`;
for (const key of ['OAUTH2_SUCCESS_REDIRECT', 'OAUTH2_FAILURE_REDIRECT', 'VNPAY_PAY_URL', 'VNPAY_RETURN_URL', 'VNPAY_REFUND_URL', 'VNPAY_QUERY_URL', 'VNPAY_IPN_URL']) {
  try {
    const url = new URL(settings[key]);
    if (url.protocol !== 'https:' || url.username || url.password || url.hash) throw new Error();
    if (!args.includes('--allow-sandbox') && url.hostname.includes('sandbox')) errors.push(`${key} còn dùng sandbox.`);
    if (['VNPAY_REFUND_URL', 'VNPAY_QUERY_URL'].includes(key) && [webOrigin, apiOrigin].includes(url.origin)) errors.push(`${key} phải trỏ tới gateway, không trỏ về Ladux.`);
  } catch { errors.push(`${key} phải là HTTPS URL hợp lệ.`); }
}
if (settings.OAUTH2_SUCCESS_REDIRECT !== `${webOrigin}/auth/oauth2/success`) errors.push('OAUTH2_SUCCESS_REDIRECT không khớp route storefront.');
if (settings.VNPAY_RETURN_URL !== `${webOrigin}/payment/vnpay/return`) errors.push('VNPAY_RETURN_URL không khớp route storefront.');
if (settings.VNPAY_IPN_URL !== `${apiOrigin}/api/v1/payments/vnpay-webhook`) errors.push('VNPAY_IPN_URL không khớp endpoint IPN.');
if (!settings.APP_CORS_ALLOWED_ORIGINS?.split(',').map(s => s.trim()).includes(webOrigin)) errors.push('CORS phải cho phép storefront HTTPS origin.');
for (const origin of settings.APP_CORS_ALLOWED_ORIGINS?.split(',') ?? []) {
  try { const url = new URL(origin.trim()); if (url.protocol !== 'https:' || url.origin !== origin.trim()) throw new Error(); }
  catch { errors.push('CORS phải chứa các HTTPS origin chính xác.'); }
}
if (!/^[A-Za-z0-9+/]+={0,2}$/.test(settings.JWT_SECRET ?? '') || Buffer.from(settings.JWT_SECRET ?? '', 'base64').length < 32) errors.push('JWT_SECRET cần Base64 của ít nhất 32 byte ngẫu nhiên.');
if ((settings.DB_PASSWORD?.length ?? 0) < 16) errors.push('DB_PASSWORD cần ít nhất 16 ký tự.');
if (settings.BOOTSTRAP_ADMIN_ENABLED === 'true') {
  if (!/^[A-Za-z0-9_.-]{3,60}$/.test(settings.BOOTSTRAP_ADMIN_USERNAME ?? '')) errors.push('Bootstrap username không hợp lệ.');
  const length = Buffer.byteLength(settings.BOOTSTRAP_ADMIN_PASSWORD ?? '');
  if (length < 16 || length > 72 || settings.BOOTSTRAP_ADMIN_PASSWORD?.startsWith('REPLACE_')) errors.push('Bootstrap password cần 16–72 byte UTF-8.');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(settings.BOOTSTRAP_ADMIN_EMAIL ?? '')) errors.push('Bootstrap email không hợp lệ.');
}
if (errors.length) {
  console.error([...new Set(errors)].map(error => `- ${error}`).join('\n'));
  process.exit(1);
}
if (!args.includes('--skip-docker')) {
  const result = spawnSync('docker', ['compose', '--env-file', envFile, '-f', path.join(root, 'backend/docker-compose.prod.yml'), 'config', '--quiet'],
    { cwd: root, encoding: 'utf8', stdio: 'pipe' });
  if (result.status !== 0) {
    console.error('Docker Compose config chưa hợp lệ hoặc chưa cài Docker. Chạy config --quiet tại máy triển khai để chẩn đoán.');
    process.exit(1);
  }
}
console.log(`Preflight cấu hình đạt.${args.includes('--skip-docker') ? ' Chưa kiểm tra Compose.' : ' Compose config đạt.'} DNS/TLS và dịch vụ thật cần smoke test sau deploy.`);
