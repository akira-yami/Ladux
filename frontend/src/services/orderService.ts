import apiClient from './apiClient';
import { PageParams, PageResponse, ProductResponse } from './productService';
import { PaymentProvider } from './paymentService';

import { OrderStatus } from '../types';

export type { OrderStatus };

export interface ShippingAddressRequest {
  receiverName: string;
  phone: string;
  street: string;
  ward: string;
  district: string;
  city: string;
}

export interface ShippingAddressResponse {
  id?: number;
  receiverName: string;
  phone: string;
  street: string;
  ward: string;
  district: string;
  city: string;
}

export interface OrderRequest {
  expectedAmount?: number;
  couponCode?: string;
  paymentProvider: PaymentProvider;
  shippingAddress: ShippingAddressRequest;
}

export interface OrderItemResponse {
  id: number;
  orderId: number | null;
  product: ProductResponse | null;
  productVariantId: number | null;
  quantity: number;
  priceAtPurchase: number; // khớp với backend OrderItemResponse.priceAtPurchase
}

export interface OrderResponse {
  id: number;
  userId: number | null;
  couponCode: string | null;
  subTotal: number;
  discountAmount: number;
  shippingFee?: number;
  finalAmount: number;
  status: OrderStatus;
  shippingAddress: ShippingAddressResponse;
  trackingNumber: string | null;
  createdAt: string;
  paymentExpiresAt: string | null;
  orderItems: OrderItemResponse[];
  paymentProvider: PaymentProvider | null;
  returnReason?: string | null;
}

export interface OrderPaymentRetryResponse {
  paymentUrl?: string;
  status: string;
  message?: string;
}

export const orderService = {
  /**
   * Lấy chi tiết đơn hàng theo Order ID
   * GET /api/v1/orders/{orderId}
   */
  getOrderById: (orderId: number): Promise<OrderResponse> => {
    return apiClient.get(`/orders/${orderId}`);
  },

  /**
   * Lấy danh sách lịch sử đơn hàng của người dùng đang đăng nhập
   * GET /api/v1/orders/user
   */
  getOrdersByUser: (params?: PageParams): Promise<PageResponse<OrderResponse>> => {
    return apiClient.get('/orders/user', { params });
  },

  /**
   * Tạo đơn hàng mới từ giỏ hàng hiện tại
   * POST /api/v1/orders
   */
  createOrder: (data: OrderRequest): Promise<OrderResponse> => {
    return apiClient.post('/orders', data);
  },

  /**
   * Hủy đơn hàng của người dùng đang ở trạng thái PENDING
   * DELETE /api/v1/orders/{orderId}
   */
  cancelOrder: (orderId: number): Promise<void> => {
    return apiClient.delete(`/orders/${orderId}`);
  },

  /**
   * Thực hiện thanh toán lại cho đơn hàng chưa hoàn tất thanh toán
   * POST /api/v1/orders/{orderId}/payments/retry
   */
  retryPayment: (orderId: number): Promise<OrderPaymentRetryResponse> => {
    return apiClient.post(`/orders/${orderId}/payments/retry`);
  },

  /**
   * Yêu cầu trả hàng cho đơn đã giao thành công (DELIVERED)
   * POST /api/v1/orders/{orderId}/request-return
   */
  requestReturn: (orderId: number, reason?: string): Promise<OrderResponse> => {
    return apiClient.post(`/orders/${orderId}/request-return`, { reason });
  },

  /**
   * [Admin] Lấy tất cả các sản phẩm trong đơn hàng (Có phân trang)
   * GET /api/v1/admin/order-items
   */
  getAllOrderItems: (params?: PageParams): Promise<PageResponse<OrderItemResponse>> => {
    return apiClient.get('/admin/order-items', { params });
  },

  /**
   * [Admin] Lấy chi tiết chi tiết sản phẩm đơn hàng theo ID
   * GET /api/v1/admin/order-items/{id}
   */
  getOrderItemById: (id: number): Promise<OrderItemResponse> => {
    return apiClient.get(`/admin/order-items/${id}`);
  },

  /**
   * [Admin] Lấy danh sách sản phẩm theo Order ID
   * GET /api/v1/admin/order-items/order/{orderId}
   */
  getOrderItemsByOrderId: (orderId: number, params?: PageParams): Promise<PageResponse<OrderItemResponse>> => {
    return apiClient.get(`/admin/order-items/order/${orderId}`, { params });
  },
};
