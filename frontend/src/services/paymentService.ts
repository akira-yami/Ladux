import apiClient from './apiClient';
import { PaymentProvider } from '../types';

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'REFUNDED';
export type { PaymentProvider };

export interface PaymentCallbackResponse {
  id: number;
  orderId: number | null;
  provider: PaymentProvider;
  merchantTxnRef: string | null;
  transactionNo: string | null;
  amount: number;
  status: PaymentStatus;
  paymentUrl: string | null;
  createdAt: string;
  orderStatus?: string;
  paymentExpiresAt?: string | null;
  canRetry?: boolean;
  requiresReview?: boolean;
}

export interface PaymentCreateRequest {
  orderId: number;
  provider: PaymentProvider;
}

export const paymentService = {
  /**
   * Lấy lịch sử giao dịch thanh toán của tôi
   * GET /api/v1/payments/my
   */
  getMyPayments: (params?: { page?: number; size?: number }): Promise<any> => {
    return apiClient.get('/payments/my', { params });
  },

  /**
   * Lấy giao dịch thanh toán theo Order ID
   * GET /api/v1/payments/my/order/{orderId}
   */
  getMyPaymentsByOrderId: (orderId: number, params?: { page?: number; size?: number }): Promise<any> => {
    return apiClient.get(`/payments/my/order/${orderId}`, { params });
  },

  /**
   * Lấy lịch sử giao dịch thanh toán theo trạng thái (SUCCESS, PENDING, FAILED...)
   * GET /api/v1/payments/my/status/{status}
   */
  getMyPaymentsByStatus: (status: PaymentStatus, params?: { page?: number; size?: number }): Promise<any> => {
    return apiClient.get(`/payments/my/status/${status}`, { params });
  },

  /**
   * Khởi tạo giao dịch thanh toán trực tuyến (VNPAY, ZALOPAY, MOMO...)
   * POST /api/v1/payments
   */
  createPayment: (data: PaymentCreateRequest): Promise<PaymentCallbackResponse> => {
    return apiClient.post('/payments', data);
  },

  /**
   * Tra cứu trạng thái thanh toán bằng merchantTxnRef (dành cho return URL)
   * GET /api/v1/payments/my/txn-ref/{merchantTxnRef}
   */
  getMyPaymentByTxnRef: (txnRef: string): Promise<PaymentCallbackResponse> => {
    return apiClient.get(`/payments/my/txn-ref/${encodeURIComponent(txnRef)}`);
  },

  verifyVNPayReturn: (params: Record<string, string>, signal?: AbortSignal): Promise<PaymentCallbackResponse> =>
    apiClient.post('/payments/vnpay/verify-return', params, { signal }),

  reconcileVNPay: (txnRef: string, signal?: AbortSignal): Promise<PaymentCallbackResponse> =>
    apiClient.post(`/payments/my/txn-ref/${encodeURIComponent(txnRef)}/reconcile`, undefined, { signal }),
};
