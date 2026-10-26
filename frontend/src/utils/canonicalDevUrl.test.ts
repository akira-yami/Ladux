import { describe, expect, it } from 'vitest';
import { canonicalDevUrl } from './canonicalDevUrl';

const local = 'http://localhost:5173';
const tunnel = 'https://shop.ngrok-free.dev';

describe('local development origin', () => {
    it('preserves signed VNPay return fields byte-for-byte while returning to the login origin', () => {
        const path = '/payment/vnpay/return?vnp_TxnRef=LDX1&vnp_OrderInfo=Don+hang+%2B+1&vnp_SecureHash=ab%2Bcd';
        expect(canonicalDevUrl(true, local, tunnel + path)).toBe(local + path);
    });
    it('recovers a protected payment return from a login history entry', () => {
        const path = '/payment/vnpay/return?vnp_TxnRef=LDX1&vnp_SecureHash=abc';
        expect(canonicalDevUrl(true, local, tunnel + '/login', path)).toBe(local + path);
    });
    it('never redirects production, an unconfigured dev app, or the canonical origin', () => {
        expect(canonicalDevUrl(false, local, tunnel + '/orders')).toBeNull();
        expect(canonicalDevUrl(true, '', tunnel + '/orders')).toBeNull();
        expect(canonicalDevUrl(true, local, local + '/orders')).toBeNull();
    });
    it('does not follow external destinations from login history', () => {
        for (const path of ['https://evil.test', '//evil.test', '/\\evil.test']) {
            expect(canonicalDevUrl(true, local, tunnel + '/login', path)).toBe(local + '/login');
        }
    });
    it('rejects malformed or unsafe configured origins', () => {
        for (const origin of ['javascript:alert(1)', 'http://other.test', local + '/orders', local + '?next=evil', 'https://user:pass@shop.test']) {
            expect(() => canonicalDevUrl(true, origin, tunnel + '/')).toThrow();
        }
    });
});
