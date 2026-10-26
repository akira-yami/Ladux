import fs from 'node:fs';
import path from 'node:path';
import { createHmac, randomUUID } from 'node:crypto';
import { parseEnv } from 'node:util';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const sandbox = 'https://sandbox.vnpayment.vn';
const defaults = {
  VNPAY_PAY_URL: `${sandbox}/paymentv2/vpcpay.html`,
  VNPAY_QUERY_URL: `${sandbox}/merchant_webapi/api/transaction`,
  VNPAY_REFUND_URL: `${sandbox}/merchant_webapi/api/transaction`,
  VNPAY_RETURN_URL: 'http://localhost:5173/payment/vnpay/return',
  VNPAY_IPN_URL: 'http://localhost:8080/api/v1/payments/vnpay-webhook',
};

export function checkConfiguration(values) {
  const settings = { ...defaults, ...values };
  const errors = [];
  const warnings = [];
  for (const key of ['VNPAY_TMN_CODE', 'VNPAY_HASH_SECRET']) {
    if (!settings[key] || /REPLACE_|YOUR_|\$\{|[<>\s]/i.test(settings[key])) errors.push(`Chưa cấu hình đúng ${key}.`);
  }
  if (!/^[A-Za-z0-9]{8}$/.test(settings.VNPAY_TMN_CODE ?? '')) errors.push('VNPAY_TMN_CODE phải gồm 8 ký tự chữ/số do VNPAY cấp.');
  const urls = {};
  for (const key of Object.keys(defaults)) {
    try {
      const url = new URL(settings[key]);
      if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.hash || url.search) throw new Error();
      urls[key] = url;
    } catch { errors.push(`${key} phải là URL hợp lệ, không chứa thông tin đăng nhập, query hoặc fragment.`); }
  }
  const pay = urls.VNPAY_PAY_URL;
  const isSandbox = pay?.origin === sandbox;
  if (pay && ![`${sandbox}/paymentv2/vpcpay.html`, 'https://pay.vnpay.vn/vpcpay.html'].includes(pay.href))
    errors.push('VNPAY_PAY_URL phải là endpoint thanh toán HTTPS chính thức của VNPAY.');
  for (const key of ['VNPAY_QUERY_URL', 'VNPAY_REFUND_URL']) {
    const url = urls[key];
    if (!url) continue;
    if (url.protocol !== 'https:' || (url.origin === sandbox) !== isSandbox)
      errors.push(`${key} phải dùng HTTPS và cùng môi trường Sandbox/production với VNPAY_PAY_URL.`);
    if (url.origin === urls.VNPAY_RETURN_URL?.origin || url.origin === urls.VNPAY_IPN_URL?.origin)
      errors.push(`${key} phải trỏ tới VNPAY, không trỏ về Ladux.`);
  }
  for (const [key, route] of [['VNPAY_RETURN_URL', '/payment/vnpay/return'], ['VNPAY_IPN_URL', '/api/v1/payments/vnpay-webhook']]) {
    const url = urls[key];
    if (!url) continue;
    const local = ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname);
    if (url.pathname !== route) errors.push(`${key} phải dùng route ${route}.`);
    if (url.protocol !== 'https:' && !(isSandbox && local)) errors.push(`${key} cần HTTPS; chỉ Sandbox localhost được dùng HTTP.`);
    if (!isSandbox && local) errors.push(`${key} production không được dùng localhost.`);
    if (key === 'VNPAY_IPN_URL' && local) warnings.push('VNPAY không gọi IPN được về localhost. Local có QueryDR; triển khai cần đăng ký IPN HTTPS công khai với VNPAY.');
  }
  return { settings, errors: [...new Set(errors)], warnings, isSandbox };
}

export function buildSandboxProbe(settings, now = new Date()) {
  // No DB order, bank details, OTP, refund or payment confirmation is submitted.
  if (settings.VNPAY_PAY_URL !== defaults.VNPAY_PAY_URL) throw new Error('Chỉ kiểm tra gateway Sandbox.');
  const date = value => {
    const parts = Object.fromEntries(new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23',
    }).formatToParts(value).map(part => [part.type, part.value]));
    return ['year', 'month', 'day', 'hour', 'minute', 'second'].map(key => parts[key]).join('');
  };
  const fields = {
    vnp_Version: '2.1.0', vnp_Command: 'pay', vnp_TmnCode: settings.VNPAY_TMN_CODE,
    vnp_Amount: '1000000', vnp_CurrCode: 'VND', vnp_TxnRef: `LDXDIAG${randomUUID().replaceAll('-', '')}`,
    vnp_OrderInfo: 'Kiem tra cau hinh Ladux', vnp_OrderType: 'other', vnp_Locale: 'vn',
    vnp_ReturnUrl: settings.VNPAY_RETURN_URL, vnp_IpAddr: '127.0.0.1',
    vnp_CreateDate: date(now), vnp_ExpireDate: date(new Date(now.getTime() + 5 * 60_000)),
  };
  const query = new URLSearchParams(Object.entries(fields).sort(([a], [b]) => a < b ? -1 : a > b ? 1 : 0)).toString();
  const signature = createHmac('sha512', settings.VNPAY_HASH_SECRET).update(query).digest('hex');
  return new URL(`${settings.VNPAY_PAY_URL}?${query}&vnp_SecureHash=${signature}`);
}

export function classifyGatewayPage(html, pageUrl) {
  const decoded = html.replace(/&#(x[0-9a-f]+|[0-9]+);/gi, (entity, value) => {
    const point = value[0].toLowerCase() === 'x' ? parseInt(value.slice(1), 16) : Number(value);
    return point <= 0x10ffff ? String.fromCodePoint(point) : entity;
  });
  const text = decoded.replace(/<[^>]*>/g, ' ').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replaceAll('đ', 'd');
  if (/not approved|chua duoc phe duyet/.test(text)) return 'MERCHANT_NOT_APPROVED';
  if (/invalid checksum|invalid signature|sai chu ky|chu ky khong hop le/.test(text)) return 'INVALID_SIGNATURE';
  if (pageUrl?.origin === sandbox && pageUrl.pathname === '/paymentv2/Transaction/PaymentMethod.html'
      && /chon phuong thuc thanh toan/.test(text)) return 'PAYMENT_PAGE_REACHED';
  return 'UNCONFIRMED';
}

export async function probeSandbox(settings) {
  let url = buildSandboxProbe(settings);
  const signal = AbortSignal.timeout(20_000);
  // VNPAY keeps the payment selection state in a session cookie. Retain it
  // across same-origin redirects, in memory only, as a browser would.
  const cookies = new Map();
  for (let redirects = 0; redirects <= 5; redirects++) {
    const headers = cookies.size ? { Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; ') } : {};
    const response = await fetch(url, { redirect: 'manual', signal, headers });
    for (const cookie of response.headers.getSetCookie()) {
      const pair = cookie.split(';', 1)[0];
      const equals = pair.indexOf('=');
      if (equals > 0) cookies.set(pair.slice(0, equals), pair.slice(equals + 1));
    }
    if (response.status >= 300 && response.status < 400 && response.headers.has('location')) {
      const next = new URL(response.headers.get('location'), url);
      // Never follow a return URL or pass signed parameters outside the Sandbox.
      if (next.origin !== sandbox || next.username || next.password) throw new Error('Redirect không thuộc Sandbox.');
      await response.body?.cancel();
      url = next;
      continue;
    }
    if (!response.ok) throw new Error('Gateway không trả HTTP thành công.');
    return classifyGatewayPage(await response.text(), url);
  }
  throw new Error('Gateway chuyển hướng quá nhiều lần.');
}

async function main(args) {
  if (args.includes('--help')) {
    console.log('node scripts/check-vnpay.mjs [--config backend/.env] [--gateway]\nMặc định chỉ kiểm tra file. --gateway gửi URL chẩn đoán tới Sandbox, không thanh toán.');
    return 0;
  }
  const index = args.indexOf('--config');
  if ((index >= 0 && (!args[index + 1] || args[index + 1].startsWith('--')))
      || args.some((arg, i) => !['--config', '--gateway'].includes(arg) && !(index >= 0 && i === index + 1))) {
    console.error('Tham số không hợp lệ. Xem --help.');
    return 1;
  }
  const envFile = path.resolve(root, index >= 0 ? args[index + 1] : 'backend/.env');
  let values;
  try { values = parseEnv(fs.readFileSync(envFile, 'utf8')); }
  catch { console.error('Không đọc được file .env đã chọn.'); return 1; }
  const result = checkConfiguration(values);
  const overrides = Object.keys({ ...defaults, VNPAY_TMN_CODE: '', VNPAY_HASH_SECRET: '' })
    .filter(key => process.env[key] !== undefined && process.env[key] !== result.settings[key]);
  if (overrides.length) console.warn(`Shell có giá trị khác cho: ${overrides.join(', ')}. Công cụ chỉ kiểm tra file đã chọn; khi chạy trực tiếp/Compose production cần kiểm tra biến shell.`);
  for (const warning of result.warnings) console.warn(`Lưu ý: ${warning}`);
  if (result.errors.length) { result.errors.forEach(error => console.error(`- ${error}`)); return 1; }
  console.log(`Cấu hình file hợp lệ (${result.isSandbox ? 'Sandbox' : 'production'}). Chưa xác nhận merchant được duyệt hoặc thanh toán thành công.`);
  if (!args.includes('--gateway')) return 0;
  if (!result.isSandbox) { console.error('--gateway chỉ hỗ trợ Sandbox.'); return 1; }
  let status;
  try { status = await probeSandbox(result.settings); }
  catch { console.error('Chưa kiểm tra được gateway (kết nối, HTTP hoặc redirect). Không suy ra merchant/secret đúng hay sai.'); return 2; }
  console.log(`Gateway: ${status}`);
  if (status === 'PAYMENT_PAGE_REACHED') {
    console.log('Đã mở được trang chọn phương thức thanh toán Sandbox. Chưa thực hiện thanh toán hoặc xác minh IPN/QueryDR.');
    return 0;
  }
  if (status === 'MERCHANT_NOT_APPROVED') console.error('VNPAY báo website chưa được phê duyệt. Chủ merchant cần yêu cầu VNPAY kích hoạt đúng TmnCode Sandbox; sửa mã nguồn không thể tự duyệt merchant.');
  if (status === 'INVALID_SIGNATURE') console.error('Kiểm tra TmnCode/HashSecret được cấp cùng merchant và cùng môi trường; dán nguyên giá trị, rồi tạo lại URL thanh toán.');
  if (status === 'UNCONFIRMED') console.error('Không nhận diện được kết quả rõ ràng. Cần kiểm tra trên trang Sandbox; đây không phải bằng chứng thanh toán thành công.');
  return status === 'UNCONFIRMED' ? 2 : 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  process.exitCode = await main(process.argv.slice(2));
}
