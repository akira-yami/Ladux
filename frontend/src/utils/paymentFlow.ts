import type { PaymentCallbackResponse } from '../services/paymentService';

export function parseVNPayReturn(search: string): Record<string, string> {
    const query = new URLSearchParams(search);
    const fields: Record<string, string> = {};
    query.forEach((value, key) => {
        if (!key.startsWith('vnp_')) return;
        if (key in fields) throw new Error('URL VNPay có tham số trùng lặp. Hãy xem trạng thái trong đơn hàng.');
        fields[key] = value;
    });
    if (!fields.vnp_TxnRef || !fields.vnp_SecureHash)
        throw new Error('Liên kết kết quả VNPay thiếu mã giao dịch hoặc chữ ký xác thực.');
    return fields;
}

export function paymentOutcome(payment: PaymentCallbackResponse) {
    if (payment.status === 'REFUNDED') return 'refunded';
    if (payment.requiresReview) return 'review';
    if (payment.status === 'SUCCESS') return 'success';
    if (payment.status === 'FAILED' || payment.status === 'CANCELLED') return 'failed';
    return 'pending';
}

export function requireVNPayUrl(value: string | null | undefined): string {
    if (!value) throw new Error('Máy chủ chưa trả về liên kết thanh toán VNPay.');
    const url = new URL(value);
    if (url.protocol !== 'https:' || !(url.hostname === 'sandbox.vnpayment.vn' || url.hostname === 'pay.vnpay.vn'))
        throw new Error('Liên kết cổng thanh toán VNPay không hợp lệ.');
    return url.href;
}
