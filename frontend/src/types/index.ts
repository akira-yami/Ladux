export type ViewType =
    | "login"
    | "register"
    | "store"
    | "all-products"
    | "product-detail"
    | "cart"
    | "checkout"
    | "orders"
    | "wishlist"
    | "about"
    | "contact"
    | "account"
    | "addresses";

export type CustomerLevel = "BROWSER" | "SILVER" | "GOLD" | "RUBY";

export type OrderStatus =
    | "PENDING"
    | "CONFIRMED"
    | "PAID"
    | "PROCESSING"
    | "SHIPPED"
    | "DELIVERED"
    | "RETURN_REQUESTED"
    | "RETURNED"
    | "REFUNDED"
    | "CANCELLED";

export type PaymentProvider = "VNPAY" | "ZALOPAY" | "MOMO" | "COD";

export interface VariantOption {
    ram: "16GB" | "32GB" | "64GB";
    storage: "512GB SSD" | "1TB SSD" | "2TB SSD";
    colorName: string;
    colorHex: string;
    priceDelta: number;
}

export interface ShippingAddressRequest {
    id: number;
    fullName: string;
    phone: string;
    addressDetail: string;
    ward: string;
    district: string;
    city: string;
    isDefault: boolean;
}

export interface CartItem {
    product: LaptopProduct;
    quantity: number;
    selectedRam: string;
    selectedStorage: string;
    selectedColorName: string;
    selectedColorHex: string;
    price: number;
}

export interface CouponItem {
    code: string;
    discountAmount: number;
    minSubtotal: number;
    description: string;
}

export interface OrderItemRecord {
    id: string;
    orderNumber: string;
    date: string;
    items: CartItem[];
    shippingAddress: ShippingAddressRequest;
    paymentMethod: PaymentProvider | null;
    subTotal: number;
    discountAmount: number;
    shippingFee: number;
    finalAmount: number;
    status: OrderStatus;
    trackingNumber: string;
}

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

export interface ReviewItem {
    id: number;
    reviewerName: string;
    rating: number;
    comment: string;
    createdAt: string;
    avatar?: string;
}

import type { ProductResponse, ProductVariantResponse } from "../services/productService";

export interface LaptopProduct {
    id: number;
    brand: string;
    brandId?: number;
    category: string;
    categoryId?: number;
    name: string;
    slug: string;
    cpu: string;
    gpu: string;
    display: string;
    battery: string;
    weight: string;
    numberOfFans: number;
    os: string;
    ram: string;
    rom: string;
    price: number;
    discountPrice?: number;
    stockQuantity: number;
    rating: number;
    reviewCount: number;
    images: string[];
    description: string;
    reviews: ReviewItem[];
    variants: ProductVariantResponse[];
    defaultVariantId?: number;
    isNew?: boolean;
    isFeatured?: boolean;
}

/**
 * Chuyển đổi ProductResponse (Backend DTO) → LaptopProduct (Frontend UI type)
 * Nếu có variants, lấy variant đầu tiên active để fill ram/rom/price.
 */
export function mapProductResponseToLaptopProduct(
    product: ProductResponse,
    selectedVariantId?: number
): LaptopProduct {
    const API_BASE = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1").replace("/api/v1", "");

    // Lấy variant chính (ưu tiên variant active đầu tiên)
    const availableVariants = (product.variants || []).filter((variant) => variant.isActive);
    const activeVariant = availableVariants.find((variant) => variant.id === selectedVariantId)
        ?? availableVariants.reduce<ProductVariantResponse | undefined>((lowest, variant) => {
            if (!lowest) return variant;
            const currentPrice = Number(variant.discountPrice ?? variant.price);
            const lowestPrice = Number(lowest.discountPrice ?? lowest.price);
            return currentPrice < lowestPrice ? variant : lowest;
        }, undefined);

    // Map images: backend trả { imageUrl } relative → cần prefix API_BASE nếu là relative path + khử trùng trùng lặp URL
    const imageUrls = Array.from(
        new Set(
            (product.images || []).flatMap((img) => {
                if (!img?.imageUrl) return [];
                if (img.imageUrl.startsWith("http")) return img.imageUrl;
                return `${API_BASE}${img.imageUrl.startsWith("/") ? "" : "/"}${img.imageUrl}`;
            })
        )
    );

    // Kiểm tra isNew: sản phẩm tạo trong 30 ngày gần đây
    const createdDate = product.createdAt ? new Date(product.createdAt) : null;
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    const isNew = createdDate ? createdDate > thirtyDaysAgo : false;

    return {
        id: product.id,
        brand: product.brand?.name || "Unknown",
        brandId: product.brand?.id,
        category: product.category?.name || "Other",
        categoryId: product.category?.id,
        name: product.name || "",
        slug: product.slug || "",
        cpu: product.cpu || "",
        gpu: product.gpu || "",
        display: product.display || "",
        battery: product.battery || "",
        weight: product.weight || "",
        numberOfFans: product.numberOfFans || 0,
        os: product.os || "",
        ram: activeVariant?.ram || "",
        rom: activeVariant?.rom || "",
        price: activeVariant ? Number(activeVariant.price) : 0,
        discountPrice: activeVariant?.discountPrice != null ? Number(activeVariant.discountPrice) : undefined,
        stockQuantity: availableVariants.reduce((total, variant) => total + variant.stockQuantity, 0),
        rating: Number(product.averageRating || 0),
        reviewCount: Number(product.reviewCount || 0),
        images: imageUrls,
        description: product.description || "",
        reviews: [],
        variants: availableVariants,
        defaultVariantId: activeVariant?.id,
        isNew,
        isFeatured: false,
    };
}

export const formatVND = (amount?: number | string | null): string => {
    const num = Number(amount ?? 0);
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
    }).format(isNaN(num) ? 0 : num);
};

export function getAvatarUrl(avatarPath?: string | null): string {
    if (!avatarPath) {
        return "";
    }
    if (avatarPath.startsWith("http") || avatarPath.startsWith("blob:")) {
        return avatarPath;
    }
    const API_BASE = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1").replace("/api/v1", "");
    return `${API_BASE}${avatarPath.startsWith("/") ? "" : "/"}${avatarPath}`;
}
