import type { AxiosRequestConfig } from "axios";
import apiClient from "./adminApiClient";
import type {
  BrandRequest,
  BrandResponse,
  CategoryRequest,
  CategoryResponse,
  ColorRequest,
  ColorResponse,
  CouponRequest,
  CouponResponse,
  CustomerLevel,
  CustomerResponse,
  CustomerUpdateRequest,
  NotificationRequest,
  NotificationResponse,
  OrderHistoryResponse,
  OrderItemResponse,
  OrderResponse,
  OrderStatus,
  OrderStatusUpdateRequest,
  PageParams,
  PageResponse,
  PaymentResponse,
  PaymentStatus,
  PaymentUpdateRequest,
  ProductImageResponse,
  ProductRequest,
  ProductResponse,
  ProductSupplierRequest,
  ProductSupplierResponse,
  ProductVariantRequest,
  ProductVariantResponse,
  PurchaseOrderCreateRequest,
  PurchaseOrderReceiveRequest,
  PurchaseOrderResponse,
  PurchaseOrderStatus,
  PurchaseOrderStatusUpdateRequest,
  ReviewResponse,
  RoleResponse,
  StockMovementRequest,
  StockMovementResponse,
  SupplierRequest,
  SupplierResponse,
  UserAddressResponse,
  UserResponse,
  UserUpdateRequest,
} from "../types";

export type AdminLoginResponse = { message: string; userId: string; username: string; accessToken: string; tokenType: "Bearer" };

function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.get(url, config) as unknown as Promise<T>;
}

function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.post(url, data, config) as unknown as Promise<T>;
}

function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.put(url, data, config) as unknown as Promise<T>;
}

function patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.patch(url, data, config) as unknown as Promise<T>;
}

function remove<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return apiClient.delete(url, config) as unknown as Promise<T>;
}

export const adminApi = {
  auth: {
    login: (data: { username: string; password: string; captchaToken?: string }) =>
      post<AdminLoginResponse>("/admin/auth/login", data),
    currentUser: () => get<UserResponse>("/admin/auth/me"),
    refresh: () => post<{ message: string; accessToken: string; tokenType: "Bearer" }>("/admin/auth/refresh"),
    logout: () => post<void>("/admin/auth/logout"),
  },

  brands: {
    list: (params?: PageParams) => get<PageResponse<BrandResponse>>("/brands", { params }),
    detail: (id: number) => get<BrandResponse>(`/brands/${id}`),
    byName: (name: string) => get<BrandResponse>(`/brands/name/${encodeURIComponent(name)}`),
    bySlug: (slug: string) => get<BrandResponse>(`/brands/slug/${encodeURIComponent(slug)}`),
    create: (data: BrandRequest) => post<BrandResponse>("/admin/brands", data),
    update: (id: number, data: BrandRequest) => put<BrandResponse>(`/admin/brands/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/brands/${id}`),
  },

  categories: {
    list: (params?: PageParams) => get<PageResponse<CategoryResponse>>("/categories", { params }),
    create: (data: CategoryRequest) => post<CategoryResponse>("/admin/categories", data),
    update: (id: number, data: CategoryRequest) => put<CategoryResponse>(`/admin/categories/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/categories/${id}`),
    uploadImage: (file: File) => {
      const formData = new FormData();
      formData.append("file", file);
      return post<{ url: string }>("/admin/categories/upload-image", formData);
    },
  },

  colors: {
    list: (params?: PageParams) => get<PageResponse<ColorResponse>>("/admin/color", { params }),
    create: (data: ColorRequest) => post<ColorResponse>("/admin/color", data),
    update: (id: number, data: ColorRequest) => put<ColorResponse>(`/admin/color/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/color/${id}`),
  },

  coupons: {
    list: (params?: PageParams) => get<PageResponse<CouponResponse>>("/admin/coupons", { params }),
    detail: (id: number) => get<CouponResponse>(`/admin/coupons/${id}`),
    byCode: (code: string) => get<CouponResponse>(`/admin/coupons/code/${encodeURIComponent(code)}`),
    create: (data: CouponRequest) => post<CouponResponse>("/admin/coupons", data),
    update: (id: number, data: CouponRequest) => put<CouponResponse>(`/admin/coupons/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/coupons/${id}`),
  },

  products: {
    list: (params?: PageParams) => get<PageResponse<ProductResponse>>("/products", { params }),
    detail: (id: number) => get<ProductResponse>(`/products/${id}`),
    byBrand: (brandId: number, params?: PageParams) => get<PageResponse<ProductResponse>>(`/products/brand/${brandId}`, { params }),
    byCategory: (categoryId: number, params?: PageParams) => get<PageResponse<ProductResponse>>(`/products/category/${categoryId}`, { params }),
    variant: (variantId: number) => get<ProductVariantResponse>(`/products/variant/${variantId}`),
    create: (data: ProductRequest) => post<ProductResponse>("/admin/products", data),
    update: (id: number, data: ProductRequest) => put<ProductResponse>(`/admin/products/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/products/${id}`),
    createVariant: (data: ProductVariantRequest) => post<ProductVariantResponse>("/admin/product-variants", data),
    updateVariant: (id: number, data: ProductVariantRequest) => put<ProductVariantResponse>(`/admin/product-variants/${id}`, data),
    deleteVariant: (id: number) => remove<void>(`/admin/product-variants/${id}`),
    uploadImages: (productId: number, files?: File[], imageUrls?: string[]) => {
      const formData = new FormData();
      if (files) {
        files.forEach((file) => formData.append("file", file));
      }
      if (imageUrls) {
        imageUrls.forEach((url) => formData.append("imageUrls", url));
      }
      return post<ProductImageResponse[]>(`/admin/products/${productId}/images`, formData);
    },
    deleteImage: (productId: number, imageId: number) => remove<void>(`/admin/products/${productId}/images/${imageId}`),
  },

  orders: {
    list: (params?: PageParams) => get<PageResponse<OrderResponse>>("/admin/orders", { params }),
    detail: (orderId: number) => get<OrderResponse>(`/admin/orders/${orderId}`),
    byStatus: (status: OrderStatus, params?: PageParams) => get<PageResponse<OrderResponse>>(`/admin/orders/status/${status}`, { params }),
    updateStatus: (orderId: number, data: OrderStatusUpdateRequest) => patch<OrderResponse>(`/admin/orders/${orderId}/status`, data),
  },

  orderItems: {
    list: (params?: PageParams) => get<PageResponse<OrderItemResponse>>("/admin/order-items", { params }),
    detail: (id: number) => get<OrderItemResponse>(`/admin/order-items/${id}`),
    byOrder: (orderId: number, params?: PageParams) => get<PageResponse<OrderItemResponse>>(`/admin/order-items/order/${orderId}`, { params }),
  },

  orderHistories: {
    list: (params?: PageParams) => get<PageResponse<OrderHistoryResponse>>("/admin/order-histories", { params }),
    detail: (id: number) => get<OrderHistoryResponse>(`/admin/order-histories/${id}`),
    byOrder: (orderId: number, params?: PageParams) => get<PageResponse<OrderHistoryResponse>>(`/admin/order-histories/order/${orderId}`, { params }),
  },

  payments: {
    list: (params?: PageParams & { orderId?: number; status?: PaymentStatus; reviewOnly?: boolean }) => get<PageResponse<PaymentResponse>>("/admin/payments", { params }),
    byOrder: (orderId: number, params?: PageParams) => get<PageResponse<PaymentResponse>>(`/admin/payments/order/${orderId}`, { params }),
    detail: (id: number) => get<PaymentResponse>(`/admin/payments/${id}`),
    byStatus: (status: PaymentStatus, params?: PageParams) => get<PageResponse<PaymentResponse>>(`/admin/payments/status/${status}`, { params }),
    update: (id: number, data: PaymentUpdateRequest) => put<PaymentResponse>(`/admin/payments/${id}`, data),
    refund: (orderId: number, data?: { paymentId?: number; amount?: number; reason?: string }) => post<OrderResponse>(`/admin/payments/order/${orderId}/refund`, data),
    refundAttention: (params?: PageParams) => get<PageResponse<import('../types').RefundAttemptResponse>>('/admin/payments/refunds/attention', { params }),
    confirmRefund: (attemptId: number, reference: string) => post<OrderResponse>(`/admin/payments/refunds/${attemptId}/confirm`, { reference }),
  },

  customers: {
    list: (params?: PageParams) => get<PageResponse<CustomerResponse>>("/admin/customers", { params }),
    search: (name?: string, phone?: string, params?: PageParams) => get<PageResponse<CustomerResponse>>("/admin/customers/search", { params: { name, phone, ...params } }),
    byLevel: (level: CustomerLevel, params?: PageParams) => get<PageResponse<CustomerResponse>>(`/admin/customers/level/${level}`, { params }),
    detail: (id: number) => get<CustomerResponse>(`/admin/customers/${id}`),
    update: (id: number, data: CustomerUpdateRequest) => put<CustomerResponse>(`/admin/customers/${id}`, data),
  },

  users: {
    list: (params?: PageParams) => get<PageResponse<UserResponse>>("/admin/users", { params }),
    detail: (id: number) => get<UserResponse>(`/admin/users/${id}`),
    byEmail: (email: string) => get<UserResponse>(`/admin/users/email/${encodeURIComponent(email)}`),
    search: (name?: string, phone?: string, params?: PageParams) => get<PageResponse<UserResponse>>("/admin/users/search", { params: { name, phone, ...params } }),
    active: (params?: PageParams) => get<PageResponse<UserResponse>>("/admin/users/active", { params }),
    update: (id: number, data: UserUpdateRequest) => put<UserResponse>(`/admin/users/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/users/${id}`),
  },

  roles: {
    list: () => get<RoleResponse[]>("/admin/roles"),
  },

  userAddresses: {
    list: (params?: PageParams) => get<PageResponse<UserAddressResponse>>("/admin/user-addresses", { params }),
    detail: (id: number) => get<UserAddressResponse>(`/admin/user-addresses/${id}`),
  },

  reviews: {
    list: (params?: PageParams) => get<PageResponse<ReviewResponse>>("/admin/reviews", { params }),
    byUser: (userId: number, params?: PageParams) => get<PageResponse<ReviewResponse>>(`/admin/reviews/user/${userId}`, { params }),
    search: (name: string, params?: PageParams) => get<PageResponse<ReviewResponse>>("/admin/reviews/search", { params: { name, ...params } }),
  },

  suppliers: {
    list: (params?: PageParams) => get<PageResponse<SupplierResponse>>("/admin/suppliers", { params }),
    active: (params?: PageParams) => get<PageResponse<SupplierResponse>>("/admin/suppliers/active", { params }),
    detail: (id: number) => get<SupplierResponse>(`/admin/suppliers/${id}`),
    search: (name?: string, phone?: string, params?: PageParams) => get<PageResponse<SupplierResponse>>("/admin/suppliers/search", { params: { name, phone, ...params } }),
    create: (data: SupplierRequest) => post<SupplierResponse>("/admin/suppliers", data),
    update: (id: number, data: SupplierRequest) => put<SupplierResponse>(`/admin/suppliers/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/suppliers/${id}`),
  },

  productSuppliers: {
    byProduct: (productId: number) => get<ProductSupplierResponse[]>(`/admin/product-suppliers/product/${productId}`),
    bySupplier: (supplierId: number) => get<ProductSupplierResponse[]>(`/admin/product-suppliers/supplier/${supplierId}`),
    create: (data: ProductSupplierRequest) => post<ProductSupplierResponse>("/admin/product-suppliers", data),
    update: (id: number, data: ProductSupplierRequest) => put<ProductSupplierResponse>(`/admin/product-suppliers/${id}`, data),
    delete: (id: number) => remove<void>(`/admin/product-suppliers/${id}`),
  },

  purchaseOrders: {
    list: (params?: PageParams) => get<PageResponse<PurchaseOrderResponse>>("/admin/purchase-orders", { params }),
    byStatus: (status: PurchaseOrderStatus, params?: PageParams) => get<PageResponse<PurchaseOrderResponse>>(`/admin/purchase-orders/status/${status}`, { params }),
    bySupplier: (supplierId: number, params?: PageParams) => get<PageResponse<PurchaseOrderResponse>>(`/admin/purchase-orders/supplier/${supplierId}`, { params }),
    detail: (id: number) => get<PurchaseOrderResponse>(`/admin/purchase-orders/${id}`),
    create: (data: PurchaseOrderCreateRequest) => post<PurchaseOrderResponse>("/admin/purchase-orders", data),
    updateStatus: (id: number, data: PurchaseOrderStatusUpdateRequest) => patch<PurchaseOrderResponse>(`/admin/purchase-orders/${id}/status`, data),
    receive: (id: number, data: PurchaseOrderReceiveRequest) => post<PurchaseOrderResponse>(`/admin/purchase-orders/${id}/receive`, data),
  },

  stockMovements: {
    list: (params?: PageParams) => get<PageResponse<StockMovementResponse>>("/admin/stock-movements", { params }),
    byProduct: (productId: number, params?: PageParams) => get<PageResponse<StockMovementResponse>>(`/admin/stock-movements/product/${productId}`, { params }),
    adjust: (data: StockMovementRequest) => post<StockMovementResponse>("/admin/stock-movements/adjustments", data),
  },

  notifications: {
    list: (params?: PageParams) => get<PageResponse<NotificationResponse>>("/admin/notifications", { params }),
    broadcast: (data: NotificationRequest) => post<string>("/admin/notifications", data, { responseType: "text" }),
    sendToUser: (userId: number, data: NotificationRequest) => post<string>(`/admin/notifications/user/${userId}`, data, { responseType: "text" }),
    delete: (id: number) => remove<string>(`/admin/notifications/${id}`, { responseType: "text" }),
    deleteAll: () => remove<string>("/admin/notifications/delete-all", { responseType: "text" }),
    markAsRead: (id: number) => patch<void>(`/admin/notifications/${id}/read`),
    unreadCount: () => get<number>("/admin/notifications/unread-count"),
  },

  chatbot: {
    indexProduct: (productId: number) => post<void>(`/admin/chatbot/index-product/${productId}`),
    indexAllProducts: () => post<number>("/admin/chatbot/index-all"),
  },
};
