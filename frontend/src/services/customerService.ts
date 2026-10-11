import apiClient from './apiClient';
import { PageParams, PageResponse } from './productService';
import type { UserResponse } from './userService';

export type CustomerLevel = 'BROWSER' | 'SILVER' | 'GOLD' | 'RUBY';

export interface CustomerResponse {
  id: number;
  userId: number;
  email?: string;
  username?: string;
  fullName?: string;
  phone?: string;
  avatarUrl?: string;
  loyaltyPoints: number;
  level: CustomerLevel;
  totalSpent: number;
}

export interface EmailOtpSendResponse {
    verificationId: string;
    maskedEmail: string;
    expiresAt: string;
    resendAfterSeconds: number;
}

export interface EmailOtpVerifyRequest {
    verificationId: string;
    otp: string;
}

export interface CustomerUpdateRequest {
  fullName?: string;
  phone?: string;
  avatarUrl?: string;
  level?: CustomerLevel;
  loyaltyPoints?: number;
  totalSpent?: number;
}

export interface PersonalInformationUpdateRequest {
  fullName: string;
}

export const customerService = {
  updatePersonalInformation: (
    data: PersonalInformationUpdateRequest,
  ): Promise<UserResponse> => {
    return apiClient.put('/customers/me/information-personal', data);
  },

  sendEmailOtp: (email: string): Promise<EmailOtpSendResponse> => {
    return apiClient.post('/customers/me/email/otp', { email });
  },

  verifyEmailOtp: (data: EmailOtpVerifyRequest): Promise<UserResponse> => {
    return apiClient.post('/customers/me/email/verify', data);
  },

  updatePhone: (phone: string): Promise<UserResponse> => {
    return apiClient.put('/customers/me/phone', { phone });
  },

  /**
   * Lấy tất cả thông tin khách hàng (Có phân trang)
   * GET /api/v1/admin/customers
   */
  getAllCustomers: (params?: PageParams): Promise<PageResponse<CustomerResponse>> => {
    return apiClient.get('/admin/customers', { params });
  },

  /**
   * Tìm kiếm khách hàng theo tên / số điện thoại
   * GET /api/v1/admin/customers/search
   */
  searchCustomers: (name?: string, phone?: string, params?: PageParams): Promise<PageResponse<CustomerResponse>> => {
    return apiClient.get('/admin/customers/search', { params: { name, phone, ...params } });
  },

  /**
   * Lọc khách hàng theo cấp độ (Level)
   * GET /api/v1/admin/customers/level/{level}
   */
  getCustomersByLevel: (level: CustomerLevel, params?: PageParams): Promise<PageResponse<CustomerResponse>> => {
    return apiClient.get(`/admin/customers/level/${level}`, { params });
  },

  /**
   * Lấy thông tin chi tiết khách hàng theo User ID
   * GET /api/v1/admin/customers/{customerId}
   */
  getCustomerByUserId: (customerId: number): Promise<CustomerResponse> => {
    return apiClient.get(`/admin/customers/${customerId}`);
  },

  /**
   * Cập nhật thông tin khách hàng (Cấp độ, điểm thưởng...)
   * PUT /api/v1/admin/customers/{customerId}
   */
  updateCustomer: (customerId: number, data: CustomerUpdateRequest): Promise<CustomerResponse> => {
    return apiClient.put(`/admin/customers/${customerId}`, data);
  },
};
