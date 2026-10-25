import { describe, expect, it } from 'vitest';
import { parseVNPayReturn, paymentOutcome, requireVNPayUrl } from './paymentFlow';
import type { PaymentCallbackResponse } from '../services/paymentService';

const payment = { id: 1, orderId: 5, provider: 'VNPAY', status: 'PENDING', amount: 100000 } as PaymentCallbackResponse;

describe('VNPay payment outcomes', () => {
    it('never treats a pending payment or a timeout as failure', () => {
        expect(paymentOutcome(payment)).toBe('pending');
        expect(paymentOutcome({ ...payment, orderStatus: 'CANCELLED' })).toBe('pending');
    });
    it('distinguishes receipt requiring review from a successful fulfillable order', () => {
        expect(paymentOutcome({ ...payment, status: 'SUCCESS', requiresReview: true })).toBe('review');
        expect(paymentOutcome({ ...payment, status: 'SUCCESS' })).toBe('success');
        expect(paymentOutcome({ ...payment, status: 'FAILED' })).toBe('failed');
        expect(paymentOutcome({ ...payment, status: 'REFUNDED' })).toBe('refunded');
        expect(paymentOutcome({ ...payment, status: 'REFUNDED', requiresReview: true })).toBe('refunded');
    });
    it('passes decoded values once to backend verification and ignores unrelated params', () => {
        expect(parseVNPayReturn('?vnp_TxnRef=LDX1&vnp_OrderInfo=Don+hang+%2B+1&vnp_SecureHash=abc&utm=foo'))
            .toEqual({ vnp_TxnRef: 'LDX1', vnp_OrderInfo: 'Don hang + 1', vnp_SecureHash: 'abc' });
    });
    it('rejects incomplete and duplicate return parameters', () => {
        expect(() => parseVNPayReturn('?vnp_TxnRef=LDX1')).toThrow();
        expect(() => parseVNPayReturn('?vnp_TxnRef=LDX1&vnp_TxnRef=LDX2&vnp_SecureHash=abc')).toThrow();
    });
    it('only redirects to official HTTPS gateways', () => {
        expect(requireVNPayUrl('https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?a=1')).toContain('vpcpay');
        expect(requireVNPayUrl('https://pay.vnpay.vn/vpcpay.html')).toContain('pay.vnpay.vn');
        for (const url of ['', 'javascript:alert(1)', 'https://sandbox.vnpayment.vn.evil.test/pay', 'http://pay.vnpay.vn/pay'])
            expect(() => requireVNPayUrl(url)).toThrow();
    });
});
