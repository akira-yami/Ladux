import { afterEach, describe, expect, it, vi } from 'vitest';
import { redirectToVNPay } from './vnpayNavigation';

afterEach(() => vi.unstubAllGlobals());

describe('leaving Ladux for VNPay', () => {
    it('replaces checkout with orders before leaving, so Back can resume the existing order', () => {
        const history = ['/products', '/cart', '/checkout'];
        const navigate = vi.fn((path: unknown, options?: { replace?: boolean }) => {
            if (options?.replace) history[history.length - 1] = String(path);
            else history.push(String(path));
        });
        const gatewayUrl = 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=LDX1';
        const assign = vi.fn((url: string) => {
            expect(history[history.length - 1]).toBe('/orders');
            history.push(url);
        });
        vi.stubGlobal('window', { location: { assign } });
        redirectToVNPay(gatewayUrl, navigate);
        history.pop(); // Browser Back from the gateway.
        expect(history[history.length - 1]).toBe('/orders');
        expect(history).not.toContain('/checkout');
        expect(assign).toHaveBeenCalledWith(gatewayUrl);
    });

    it('preserves the current page if the gateway URL is missing or invalid', () => {
        const navigate = vi.fn();
        const assign = vi.fn();
        vi.stubGlobal('window', { location: { assign } });
        for (const url of [null, '', 'https://other.example/pay']) {
            expect(() => redirectToVNPay(url, navigate)).toThrow();
        }
        expect(navigate).not.toHaveBeenCalled();
        expect(assign).not.toHaveBeenCalled();
    });
});
