export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  pageNumber?: number;
  pageSize?: number;
  numberOfElements?: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  sort?: unknown;
  pageable?: unknown;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}

export type CustomerLevel = "BROWSER" | "SILVER" | "GOLD" | "RUBY";
export type DiscountType = "PERCENT" | "FIXED_AMOUNT";
export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "RETURN_REQUESTED"
  | "RETURNED"
  | "REFUNDED"
  | "CANCELLED";
export type PaymentProvider = "VNPAY" | "MOMO" | "COD";
export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "REFUNDED";
export type PurchaseOrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PARTIALLY_RECEIVED"
  | "RECEIVED"
  | "CANCELLED";
export type StockMovementType =
  | "PURCHASE_IN"
  | "SALE_OUT"
  | "RETURN_IN"
  | "DAMAGE_OUT"
  | "ADJUSTMENT_IN"
  | "ADJUSTMENT_OUT"
  | "OTHER";
export type StockReferenceType = "ORDER" | "PURCHASE_ORDER" | "RETURN" | "ADJUSTMENT" | "OTHER";
export type NotificationType = "ORDER_STATUS" | "PAYMENT" | "PROMOTION" | "SYSTEM" | "STOCK_ALERT";

export interface UserResponse {
  id: number;
  email: string | null;
  username: string;
  fullName: string | null;
  phone: string | null;
  avatar: string | null;
  loyaltyPoints: number | null;
  level: CustomerLevel | null;
  totalSpent: number | null;
  isActive: boolean;
  roles: string[];
}

export interface RoleResponse {
  id: number;
  name: string;
}

export interface BrandResponse {
  id: number;
  name: string;
  logoUrl: string | null;
  slug: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  imageUrl?: string | null;
}

export interface ColorResponse {
  id: number;
  name: string;
  hexCode: string;
}

export interface ProductImageResponse {
  id: number;
  imageUrl: string;
  isMain?: boolean;
}

export interface ProductVariantResponse {
  id: number;
  productId: number;
  sku: string;
  color: ColorResponse | null;
  ram: string | null;
  rom: string | null;
  price: number;
  discountPrice: number | null;
  stockQuantity: number;
  isActive: boolean;
}

export interface ProductResponse {
  id: number;
  brand: BrandResponse | null;
  category: CategoryResponse | null;
  name: string;
  slug: string;
  description: string | null;
  cpu: string | null;
  gpu: string | null;
  display: string | null;
  battery: string | null;
  weight: string | null;
  numberOfFans: number | null;
  os: string | null;
  isActive: boolean;
  createdAt: string;
  images: ProductImageResponse[];
  variants: ProductVariantResponse[];
  averageRating: number;
  reviewCount: number;
}

export interface ProductVariantRequest {
  id?: number | null;
  productId?: number | null;
  colorId?: number | null;
  ram?: string | null;
  rom?: string | null;
  price?: number | null;
  discountPrice?: number | null;
  stockQuantity: number;
  isActive: boolean;
}

export interface ProductRequest {
  brandId: number;
  categoryId: number;
  name: string;
  description?: string | null;
  cpu?: string | null;
  gpu?: string | null;
  display?: string | null;
  battery?: string | null;
  weight?: string | null;
  numberOfFans?: number | null;
  os?: string | null;
  isActive?: boolean | null;
  variants: ProductVariantRequest[];
  imageUrls?: string[];
}

export interface ShippingAddress {
  receiverName: string;
  phone: string;
  street: string;
  ward: string;
  district: string;
  city: string;
}

export interface OrderItemResponse {
  id: number;
  orderId: number;
  product: ProductResponse | null;
  productVariantId: number;
  quantity: number;
  priceAtPurchase: number;
}

export interface OrderResponse {
  id: number;
  userId: number;
  couponCode: string | null;
  subTotal: number;
  discountAmount: number;
  finalAmount: number;
  status: OrderStatus;
  shippingAddress: ShippingAddress;
  trackingNumber: string | null;
  carrier: string | null;
  shippingFee: number | null;
  createdAt: string;
  paymentExpiresAt: string | null;
  orderItems: OrderItemResponse[];
  paymentProvider: PaymentProvider;
  returnReason?: string | null;
}

export interface OrderHistoryResponse {
  id: number;
  orderId: number;
  status: string;
  description: string | null;
  createdAt: string;
}

export interface PaymentResponse {
  id: number;
  orderId: number;
  provider: PaymentProvider;
  transactionNo: string | null;
  requiresReview: boolean;
  amount: number;
  status: PaymentStatus;
  createdAt: string;
}

export interface RefundAttemptResponse {
  id: number;
  orderId: number;
  paymentId: number;
  requestId: string;
  amount: number;
  status: 'PROCESSING' | 'REVIEW_REQUIRED' | 'SUCCEEDED';
  reason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CouponResponse {
  id: number;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderValue: number | null;
  usageLimit: number | null;
  usedCount: number;
  expiresAt: string;
}

export interface CustomerResponse {
  id: number;
  userId: number;
  email: string | null;
  username: string;
  fullName: string | null;
  phone: string | null;
  avatarUrl: string | null;
  loyaltyPoints: number | null;
  level: CustomerLevel;
  totalSpent: number | null;
}

export interface UserAddressResponse {
  id: number;
  userId: number;
  receiverName: string;
  phone: string;
  street: string;
  ward: string;
  district: string;
  city: string;
  isDefault: boolean;
}

export interface ReviewResponse {
  id: number;
  reviewerName: string;
  reviewerAvatar: string | null;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface SupplierResponse {
  id: number;
  name: string;
  address: string | null;
  phone: string | null;
  email: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSupplierResponse {
  id: number;
  productId: number;
  productName: string;
  supplierId: number;
  supplierName: string;
  costPrice: number | null;
  leadTimeDays: number | null;
}

export interface PurchaseOrderItemResponse {
  id: number;
  productVariantId: number;
  productName: string;
  quantity: number;
  costPrice: number;
  receivedQuantity: number;
  note: string | null;
}

export interface PurchaseOrderResponse {
  id: number;
  supplierId: number;
  supplierName: string;
  status: PurchaseOrderStatus;
  expectedDeliveryDate: string | null;
  totalAmount: number;
  note: string | null;
  createdById: number | null;
  createdAt: string;
  updatedAt: string;
  items: PurchaseOrderItemResponse[];
}

export interface StockMovementResponse {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  movementType: StockMovementType;
  referenceType: StockReferenceType;
  referenceId: number | null;
  note: string | null;
  createdById: number | null;
  createdAt: string;
}

export interface NotificationResponse {
  id: number;
  userId?: number | null;
  userName?: string | null;
  title: string;
  message: string;
  isRead: boolean;
  type: NotificationType;
  createdAt: string;
}

export interface BrandRequest { name: string; logoUrl?: string | null }
export interface CategoryRequest { name: string; imageUrl?: string | null }
export interface ColorRequest { name: string; hexCode: string }
export interface CouponRequest {
  code: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderValue?: number | null;
  usageLimit?: number | null;
  expiresAt: string;
}
export interface CustomerUpdateRequest {
  fullName?: string | null;
  phone?: string | null;
  avatarUrl?: string | null;
  level?: CustomerLevel | null;
  loyaltyPoints?: number | null;
  totalSpent?: number | null;
}
export interface UserUpdateRequest {
  email?: string | null;
  username?: string | null;
  password?: string | null;
  fullName?: string | null;
  phone?: string | null;
  avatar?: string | null;
  isActive?: boolean | null;
  roleIds?: number[] | null;
}
export interface OrderStatusUpdateRequest { status: OrderStatus; trackingNumber?: string | null }
export interface PaymentUpdateRequest {
  orderId: number;
  provider: PaymentProvider;
  transactionNo?: string | null;
  status?: PaymentStatus | null;
}
export interface SupplierRequest {
  name: string;
  address?: string | null;
  phone?: string | null;
  email?: string | null;
  isActive?: boolean | null;
}
export interface ProductSupplierRequest {
  productId: number;
  supplierId: number;
  costPrice?: number | null;
  leadTimeDays?: number | null;
}
export interface PurchaseOrderItemRequest {
  productVariantId: number;
  quantity: number;
  costPrice: number;
  note?: string | null;
}
export interface PurchaseOrderCreateRequest {
  supplierId: number;
  note?: string | null;
  items: PurchaseOrderItemRequest[];
}
export interface PurchaseOrderStatusUpdateRequest {
  status: PurchaseOrderStatus;
  cancelReason?: string | null;
}
export interface PurchaseOrderReceiveRequest {
  lines: Array<{ itemId: number; receivedQuantity: number }>;
}
export interface StockMovementRequest {
  productId: number;
  quantity: number;
  movementType: StockMovementType;
  note?: string | null;
}
export interface NotificationRequest {
  title: string;
  message: string;
  type: NotificationType;
}

export type AuthStatus = "checking" | "authenticated" | "unauthenticated" | "forbidden";

export type AdminRow = Record<string, unknown>;
