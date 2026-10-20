import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChevronRight, Star, ShoppingBag, MessageSquare, Send, Heart, ImageOff } from "lucide-react";
import { LaptopProduct, formatVND } from "../types";
import { useWishlistStore } from "../stores";
import { ROUTES } from "../app/routePaths";

export interface ProductDetailViewProps {
    selectedProduct: LaptopProduct;
    toggleWishlist?: (id: number) => Promise<void>;
    addToCartCustom: (
        product: LaptopProduct,
        variantId: number,
        quantity: number
    ) => Promise<boolean>;
    handleAddReview: (e: React.FormEvent) => Promise<void>;
    newRating: number;
    setNewRating: (rating: number) => void;
    newComment: string;
    setNewComment: (comment: string) => void;
}

export default function ProductDetailView({
    selectedProduct,
    toggleWishlist,
    addToCartCustom,
    handleAddReview,
    newRating,
    setNewRating,
    newComment,
    setNewComment,
}: ProductDetailViewProps) {
    const navigate = useNavigate();
    useEffect(() => {
        window.scrollTo(0, 0);
    }, [selectedProduct?.id]);

    const availableVariants = selectedProduct.variants.filter((variant) => variant.isActive);
    const [selectedVariantId, setSelectedVariantId] = useState<number | null>(
        selectedProduct.defaultVariantId ?? availableVariants[0]?.id ?? null
    );
    const [productQuantity, setProductQuantity] = useState<number>(1);

    const wishlistStore = useWishlistStore();
    const isWishlisted = wishlistStore.isInWishlist(selectedProduct.id);

    const selectedVariant = availableVariants.find((variant) => variant.id === selectedVariantId)
        ?? availableVariants[0];
    const selectedColorName = selectedVariant?.color?.name?.trim();
    const variantConfigParts = [
        selectedVariant?.ram,
        selectedVariant?.rom,
        selectedColorName && selectedColorName.toLowerCase() !== "default" ? selectedColorName : "",
    ].filter(Boolean);
    const selectedVariantLabel = variantConfigParts.length > 0
        ? variantConfigParts.join(" · ")
        : selectedVariant?.sku || "Cấu hình mặc định";
    const technicalSpecs = [
        { label: "Bộ xử lý (CPU)", value: selectedProduct.cpu },
        { label: "Đồ họa (GPU)", value: selectedProduct.gpu },
        { label: "Màn hình", value: selectedProduct.display },
        { label: "RAM", value: selectedVariant?.ram },
        { label: "Lưu trữ", value: selectedVariant?.rom },
        { label: "Hệ điều hành", value: selectedProduct.os },
        { label: "Pin", value: selectedProduct.battery },
        { label: "Khối lượng", value: selectedProduct.weight },
    ].filter((item) => item.value && item.value.toString().trim());

    const [activeImageIndex, setActiveImageIndex] = useState<number>(0);

    const currentImage = selectedProduct.images[activeImageIndex] || selectedProduct.images[0];

    useEffect(() => {
        setSelectedVariantId(selectedProduct.defaultVariantId ?? availableVariants[0]?.id ?? null);
        setProductQuantity(1);
        setActiveImageIndex(0);
    }, [selectedProduct.id]);

    return (
        <main className="container mx-auto px-6 py-12">
            <div className="flex items-center gap-2 text-xs font-sans text-slate-500 mb-8 flex-wrap">
                <Link to={ROUTES.home} className="hover:text-sky-700 hover:underline transition-colors">
                    Trang chủ
                </Link>
                <ChevronRight className="w-3 h-3" />
                <Link to={ROUTES.products} className="hover:text-sky-700 hover:underline transition-colors">
                    Sản phẩm
                </Link>
                <ChevronRight className="w-3 h-3" />
                <span className="text-slate-800 font-semibold truncate max-w-[300px] sm:max-w-none">
                    {selectedProduct.name}
                </span>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-12">
                {/* Left Column: Product Gallery & Overview */}
                <div className="lg:col-span-6 space-y-6">
                    <div className="relative select-none">
                        {/* Main Image Container */}
                        <div
                            className="aspect-[4/3] bg-white rounded-xl overflow-hidden border border-slate-200 relative"
                        >
                            {currentImage ? (
                                <img
                                    src={currentImage}
                                    alt={selectedProduct.name}
                                    className="w-full h-full object-cover"
                                />
                            ) : (
                                <div className="flex h-full w-full flex-col items-center justify-center gap-2 text-slate-400">
                                    <ImageOff className="h-10 w-10" />
                                    <span className="text-xs">Sản phẩm chưa có ảnh</span>
                                </div>
                            )}

                            <span className="absolute top-4 left-4 bg-white border border-sky-700/40 text-sky-700 text-[10px] font-sans font-bold px-3 py-1 rounded-full backdrop-blur-md z-10 pointer-events-none">
                                CHÍNH HÃNG NGUYÊN SEAL
                            </span>

                        </div>

                        {/* Thumbnails Gallery */}
                        {selectedProduct.images && selectedProduct.images.length > 1 && (
                            <div className="flex items-center gap-3 overflow-x-auto pb-2 mt-4 scrollbar-thin scrollbar-thumb-white/10">
                                {selectedProduct.images.map((imgUrl, idx) => (
                                    <button
                                        key={idx}
                                        onClick={() => setActiveImageIndex(idx)}
                                        className={`relative w-16 h-16 rounded-lg overflow-hidden border-2 transition-all shrink-0 cursor-pointer ${
                                            activeImageIndex === idx
                                                ? "border-sky-700 shadow-sm scale-105"
                                                : "border-slate-200 opacity-60 hover:opacity-100 hover:border-neutral-600"
                                        }`}
                                    >
                                        <img src={imgUrl} alt={`${selectedProduct.name} ${idx + 1}`} className="w-full h-full object-cover" />
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Specifications Overview Cards */}
                    <div className="space-y-3">
                        <h3 className="font-bold text-xs normal-case tracking-normal text-slate-500">
                            Thông số kỹ thuật chi tiết
                        </h3>
                        <div className="grid grid-cols-2 gap-3 text-xs font-sans">
                            {technicalSpecs.slice(0, 3).map((spec) => (
                                <div key={spec.label} className="p-3.5 bg-white border border-slate-200 rounded-xl space-y-1">
                                    <span className="text-slate-500 block text-[10px] normal-case">{spec.label}</span>
                                    <span className="font-semibold text-slate-600 block truncate">
                                        {spec.value}
                                    </span>
                                </div>
                            ))}
                            {technicalSpecs.length === 0 && (
                                <div className="p-3.5 bg-white border border-slate-200 rounded-xl space-y-1">
                                    <span className="text-slate-500 block text-[10px] normal-case">Thông số</span>
                                    <span className="font-semibold text-slate-600 block truncate">
                                        Chưa cập nhật
                                    </span>
                                </div>
                            )}
                            <div className="p-3.5 bg-white border border-slate-200 rounded-xl space-y-1">
                                <span className="text-slate-500 block text-[10px] normal-case">Tình trạng kho</span>
                                <span className={`font-semibold block ${selectedVariant?.stockQuantity ? "text-emerald-700" : "text-rose-700"}`}>
                                    {selectedVariant?.stockQuantity
                                        ? `Sẵn hàng (${selectedVariant.stockQuantity} máy)`
                                        : "Tạm hết hàng"}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column: Variant & Quantity Selector */}
                <div className="lg:col-span-6 space-y-8">
                    <div>
                        <div className="flex items-start justify-between gap-4">
                            <div>
                                <span className="inline-block px-3 py-1 bg-white border border-emerald-500/30 text-emerald-700 text-xs font-sans font-bold rounded-md mb-3">
                                    {selectedProduct.brand} • {selectedProduct.category}
                                </span>
                                <h1 className="text-2xl sm:text-3xl font-semibold text-slate-800 leading-tight">
                                    {selectedProduct.name}
                                </h1>
                            </div>
                            <button
                                onClick={() => {
                                    void toggleWishlist?.(selectedProduct.id);
                                }}
                                className={`p-3 rounded-xl border transition ${
                                    isWishlisted
                                        ? "bg-red-500/10 border-red-500 text-red-500"
                                        : "bg-white border-slate-200 text-slate-500 hover:text-rose-700"
                                }`}
                                title={isWishlisted ? "Bỏ yêu thích" : "Thêm vào yêu thích"}
                            >
                                <Heart className={`w-5 h-5 ${isWishlisted ? "fill-current" : ""}`} />
                            </button>
                        </div>
                        <div className="mt-3 flex items-center gap-3 text-xs text-slate-500">
                            {selectedProduct.reviewCount > 0 ? (
                                <>
                                    <div className="flex items-center gap-1 text-amber-700 font-bold">
                                        <Star className="w-4 h-4 fill-amber-600" /> {selectedProduct.rating.toFixed(1)}
                                    </div>
                                    <span>•</span>
                                    <span>{selectedProduct.reviewCount} đánh giá từ khách hàng</span>
                                </>
                            ) : (
                                <span>Chưa có đánh giá</span>
                            )}
                        </div>
                    </div>

                    {/* Giá thật của biến thể đang chọn */}
                    <div className="p-6 bg-white rounded-2xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                        <div>
                            <span className="text-xs font-sans text-slate-500 normal-case block mb-1">
                                Giá chính thức (Đã có VAT)
                            </span>
                            <div className="flex items-baseline gap-3">
                                <span className="text-3xl font-semibold font-sans text-sky-700">
                                    {selectedVariant
                                        ? formatVND(selectedVariant.discountPrice || selectedVariant.price)
                                        : "Chưa có giá"}
                                </span>
                                {selectedVariant?.discountPrice
                                    && Number(selectedVariant.discountPrice) < Number(selectedVariant.price) && (
                                    <span className="text-sm font-sans text-slate-500 line-through">
                                        {formatVND(selectedVariant.price)}
                                    </span>
                                )}
                            </div>
                        </div>
                        <span className="self-start sm:self-center text-[10px] font-bold normal-case tracking-normal text-sky-700 bg-sky-50 border border-sky-700/30 px-3 py-1.5 rounded-full">
                            Phí giao hàng hiển thị tại bước thanh toán
                        </span>
                    </div>

                    {/* Các biến thể thật từ backend */}
                    <div className="space-y-6 border-t border-slate-200 pt-6">
                        <div>
                            <div className="flex justify-between items-center mb-3">
                                <label className="text-xs font-bold normal-case tracking-normal text-slate-600">
                                    Chọn cấu hình:
                                </label>
                                {selectedVariant && (
                                    <span className="text-xs font-sans text-sky-700 font-bold">
                                        SKU: {selectedVariant.sku}
                                    </span>
                                )}
                            </div>
                            {availableVariants.length > 0 ? (
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                {availableVariants.map((variant) => (
                                    <button
                                        key={variant.id}
                                        onClick={() => {
                                            setSelectedVariantId(variant.id);
                                            setProductQuantity(1);
                                        }}
                                        className={`flex items-center justify-between gap-3 py-3 px-4 rounded-xl text-left text-xs border transition-all ${
                                            selectedVariant?.id === variant.id
                                                ? "border-sky-700 bg-sky-50 text-sky-700 shadow-sm"
                                                : "border-slate-200 bg-white text-slate-500 hover:border-slate-200 hover:text-slate-800"
                                        }`}
                                    >
                                            <span>
                                                <span className="block font-bold text-slate-800">
                                                    {[
                                                        variant.ram,
                                                        variant.rom,
                                                        variant.color?.name?.trim()?.toLowerCase() === "default" ? "" : variant.color?.name,
                                                    ].filter(Boolean).join(" · ") || variant.sku || "Cấu hình mặc định"}
                                                </span>
                                                <span className="mt-1 block font-sans text-[10px]">
                                                    Còn {variant.stockQuantity} máy
                                            </span>
                                        </span>
                                        <span className="font-sans font-bold">
                                            {formatVND(variant.discountPrice || variant.price)}
                                        </span>
                                    </button>
                                ))}
                                </div>
                            ) : (
                                <p className="rounded-xl border border-red-500/20 bg-red-500/5 p-4 text-xs text-red-300">
                                    Sản phẩm chưa có cấu hình đang bán.
                                </p>
                            )}
                        </div>
                    </div>

                    {/* Quantity Stepper & Add to Cart Action */}
                    <div className="pt-4 space-y-4">
                        <div className="flex items-center gap-4">
                            {/* Quantity Stepper */}
                            <div className="flex items-center border border-slate-200 bg-white rounded-xl p-1">
                                <button
                                    onClick={() => setProductQuantity((q) => Math.max(1, q - 1))}
                                    className="w-10 h-10 flex items-center justify-center text-slate-500 hover:text-slate-800 font-bold transition"
                                >
                                    -
                                </button>
                                <span className="w-12 text-center font-sans font-bold text-sm text-slate-800">
                                    {productQuantity}
                                </span>
                                <button
                                    onClick={() => setProductQuantity((q) => Math.min(selectedVariant?.stockQuantity ?? 1, q + 1))}
                                    disabled={!selectedVariant || productQuantity >= selectedVariant.stockQuantity}
                                    className="w-10 h-10 flex items-center justify-center text-slate-500 hover:text-slate-800 font-bold transition"
                                >
                                    +
                                </button>
                            </div>

                             {/* Add to Cart Button */}
                            <button
                                onClick={() => {
                                    if (selectedVariant) {
                                        void addToCartCustom(selectedProduct, selectedVariant.id, productQuantity);
                                    }
                                }}
                                disabled={!selectedVariant || selectedVariant.stockQuantity <= 0}
                                className="flex-1 bg-sky-600 text-white py-3.5 px-6 rounded-xl font-semibold text-sm normal-case tracking-normal hover:bg-sky-600 transition shadow-lg shadow-sky-700/20 flex items-center justify-center gap-2 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                <ShoppingBag className="w-4 h-4" />
                                <span>Thêm Vào Giỏ Hàng</span>
                            </button>
                        </div>

                        <button
                            onClick={async () => {
                                if (!selectedVariant) return;
                                const added = await addToCartCustom(selectedProduct, selectedVariant.id, productQuantity);
                                if (added) navigate(ROUTES.checkout);
                            }}
                            disabled={!selectedVariant || selectedVariant.stockQuantity <= 0}
                            className="w-full bg-sky-600 text-white py-3.5 rounded-xl font-semibold text-sm normal-case tracking-normal hover:bg-primary-hover transition disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            Mua Ngay: {selectedVariantLabel}
                        </button>
                    </div>
                </div>
            </div>

            <section className="mt-12 max-w-4xl rounded-2xl border border-slate-200 bg-white p-6">
                <h2 className="mb-3 text-sm font-semibold normal-case tracking-normal text-slate-800">Mô tả sản phẩm</h2>
                <p className="whitespace-pre-line text-sm leading-relaxed text-slate-500">
                    {selectedProduct.description || "Thông tin mô tả đang được cập nhật."}
                </p>
            </section>

            {/* Reviews */}
            <section className="mt-16 pt-12 border-t border-slate-200 max-w-4xl">
                <h2 className="text-2xl font-semibold mb-8 flex items-center gap-3">
                    <MessageSquare className="w-6 h-6 text-sky-700" />
                    <span>ĐÁNH GIÁ TỪ NGƯỜI DÙNG ({selectedProduct.reviews.length})</span>
                </h2>

                <form
                    onSubmit={handleAddReview}
                    className="mb-10 p-6 bg-white border border-slate-200 rounded-xl space-y-4"
                >
                    <h3 className="text-sm font-bold text-slate-800 normal-case">Viết Đánh Giá Của Bạn</h3>

                    <div className="flex items-center gap-2">
                        <span className="text-xs text-slate-500">Đánh giá số sao:</span>
                        {[1, 2, 3, 4, 5].map((star) => (
                            <button
                                key={star}
                                type="button"
                                onClick={() => setNewRating(star)}
                                className="p-1 hover:scale-110 transition-transform"
                            >
                                <Star
                                    className={`w-5 h-5 ${
                                        star <= newRating ? "fill-amber-600 text-amber-700" : "text-slate-600"
                                    }`}
                                />
                            </button>
                        ))}
                    </div>

                    <textarea
                        required
                        rows={3}
                        placeholder="Chia sẻ trải nghiệm sử dụng mẫu Laptop này..."
                        value={newComment}
                        onChange={(e) => setNewComment(e.target.value)}
                        className="w-full p-3 bg-white border border-slate-200 rounded-lg text-xs text-slate-800 focus:outline-none focus:border-sky-700"
                    ></textarea>

                    <button
                        type="submit"
                        className="bg-sky-600 text-white px-6 py-2.5 rounded-lg text-xs font-bold flex items-center gap-2 hover:bg-sky-600"
                    >
                        <Send className="w-3.5 h-3.5" /> Gửi Đánh Giá
                    </button>
                </form>

                <div className="space-y-6">
                    {selectedProduct.reviews.map((rev) => (
                        <div
                            key={rev.id}
                            className="p-6 bg-white border border-slate-200 rounded-xl space-y-3"
                        >
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    {rev.avatar ? (
                                        <img
                                            src={rev.avatar}
                                            alt={rev.reviewerName}
                                            className="w-8 h-8 rounded-full object-cover"
                                        />
                                    ) : (
                                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-xs font-bold text-sky-700">
                                            {rev.reviewerName.trim().charAt(0).toUpperCase() || "?"}
                                        </div>
                                    )}
                                    <div>
                                        <span className="font-bold text-xs text-slate-800 block">
                                            {rev.reviewerName}
                                        </span>
                                        <span className="text-[10px] text-slate-500 font-sans">
                                            {rev.createdAt}
                                        </span>
                                    </div>
                                </div>
                                <div className="flex items-center gap-1 text-amber-700">
                                    {[...Array(rev.rating)].map((_, i) => (
                                        <Star key={i} className="w-3.5 h-3.5 fill-amber-600" />
                                    ))}
                                </div>
                            </div>
                            <p className="text-xs text-slate-600 leading-relaxed">{rev.comment}</p>
                        </div>
                    ))}
                </div>
            </section>
        </main>
    );
}
