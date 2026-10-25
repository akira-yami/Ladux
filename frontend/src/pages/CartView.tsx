import React from "react";
import { useNavigate } from "react-router-dom";
import { ShoppingCart, Trash2, ChevronRight, ShieldCheck, Truck } from "lucide-react";
import { LaptopProduct, formatVND, mapProductResponseToLaptopProduct } from "../types";
import { useCartStore } from "../stores";
import { productPath, ROUTES } from "../app/routePaths";

export interface CartViewProps {
    setSelectedProduct: (product: LaptopProduct) => void;
}

export default function CartView({
    setSelectedProduct,
}: CartViewProps) {
    const navigate = useNavigate();
    const { cart, updateQuantity, removeItem, isLoading } = useCartStore();

    const cartItemsList = cart?.items || [];
    const totalCount = cartItemsList.reduce((acc, i) => acc + i.quantity, 0);
    const subTotal = cart?.totalPrice || 0;
    const shippingFee = Number(cart?.shippingFee || 0);
    const finalAmount = subTotal + shippingFee;

    return (
        <main className="container mx-auto px-6 py-12 max-w-6xl">
            <div className="flex items-center justify-between border-b border-slate-200 pb-6 mb-8">
                <div>
                    <span className="text-xs font-sans normal-case tracking-normal text-sky-700">
                        GIỎ HÀNG LADUX ({totalCount})
                    </span>
                    <h1 className="text-3xl font-semibold text-slate-800 tracking-tight mt-1">
                        SẢN PHẨM ĐÃ CHỌN
                    </h1>
                </div>
                <button
                    onClick={() => navigate(ROUTES.products)}
                    className="text-xs font-bold normal-case tracking-normal text-slate-500 hover:text-sky-700 transition"
                >
                    ← Tiếp tục xem sản phẩm
                </button>
            </div>

            {isLoading ? (
                <div className="py-20 text-center space-y-4">
                    <div className="inline-block w-8 h-8 border-2 border-sky-700 border-t-transparent rounded-full animate-spin"></div>
                    <p className="text-slate-500 text-xs font-sans">Đang cập nhật giỏ hàng từ máy chủ...</p>
                </div>
            ) : cartItemsList.length === 0 ? (
                /* Empty Cart State */
                <div className="py-20 text-center space-y-6 max-w-md mx-auto">
                    <div className="w-24 h-24 rounded-full bg-white border border-slate-200 flex items-center justify-center mx-auto text-sky-700/70 shadow-sm">
                        <ShoppingCart className="w-10 h-10 stroke-[1.5]" />
                    </div>
                    <div className="space-y-2">
                        <h2 className="text-xl font-bold text-slate-800">Giỏ hàng của bạn đang trống</h2>
                        <p className="text-xs text-slate-500">
                            Chưa có sản phẩm Laptop cao cấp nào được chọn. Hãy khám phá ngay các siêu phẩm tại LADUX!
                        </p>
                    </div>
                    <button
                        onClick={() => navigate(ROUTES.products)}
                        className="bg-sky-600 text-white font-semibold px-8 py-3.5 rounded-xl text-xs normal-case tracking-normal hover:bg-sky-600 transition shadow-lg shadow-sky-700/20"
                    >
                        Khám phá Laptop ngay
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    {/* Item List */}
                    <div className="lg:col-span-8 space-y-4">
                        {cartItemsList.map((item) => {
                            if (!item.product) return null;
                            const mappedProduct = mapProductResponseToLaptopProduct(item.product, item.productVariant?.id);
                            const itemPrice = Number(item.productVariant?.discountPrice ?? item.productVariant?.price ?? 0);
                            const variantId = item.productVariant?.id;

                            return (
                                <div
                                    key={item.id}
                                    className="p-5 bg-white rounded-2xl border border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-6 transition hover:border-slate-200"
                                >
                                    <div className="flex items-center gap-4 w-full sm:w-auto">
                                        <div className="w-24 h-24 bg-white rounded-xl overflow-hidden border border-slate-200 shrink-0 p-2 flex items-center justify-center">
                                            <img
                                                src={mappedProduct.images[0]}
                                                alt={mappedProduct.name}
                                                className="w-full h-full object-contain"
                                            />
                                        </div>
                                        <div className="space-y-1">
                                            <span className="text-[10px] font-sans text-sky-700 font-bold normal-case">
                                                {mappedProduct.brand}
                                            </span>
                                            <h3
                                                onClick={() => {
                                                    setSelectedProduct(mappedProduct);
                                                    navigate(productPath(mappedProduct.id));
                                                }}
                                                className="font-bold text-sm text-slate-800 line-clamp-1 hover:underline cursor-pointer"
                                            >
                                                {mappedProduct.name}
                                            </h3>
                                            <p className="text-xs font-sans text-slate-500 flex items-center gap-2">
                                                <span>
                                                    {[item.productVariant?.ram, item.productVariant?.rom, item.productVariant?.color?.name]
                                                        .filter(Boolean)
                                                        .join(" / ") || "Chưa có thông tin cấu hình"}
                                                </span>
                                            </p>
                                            <p className="text-sm font-sans font-bold text-sky-700 pt-1">
                                                {formatVND(itemPrice)}
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-center justify-between sm:justify-end gap-6 w-full sm:w-auto border-t sm:border-t-0 border-slate-200 pt-4 sm:pt-0">
                                        {/* Quantity Stepper */}
                                        <div className="flex items-center border border-slate-200 bg-white rounded-xl p-1">
                                            <button
                                                onClick={() => {
                                                    if (!variantId) return;
                                                    if (item.quantity > 1) updateQuantity(variantId, item.quantity - 1);
                                                    else removeItem(variantId);
                                                }}
                                                className="w-8 h-8 flex items-center justify-center text-slate-500 hover:text-slate-800 font-bold transition"
                                            >
                                                -
                                            </button>
                                            <span className="w-10 text-center font-sans font-bold text-xs text-slate-800">
                                                {item.quantity}
                                            </span>
                                            <button
                                                onClick={() => variantId && updateQuantity(variantId, item.quantity + 1)}
                                                disabled={!variantId || item.quantity >= (item.productVariant?.stockQuantity ?? 0)}
                                                className="w-8 h-8 flex items-center justify-center text-slate-500 hover:text-slate-800 font-bold transition"
                                            >
                                                +
                                            </button>
                                        </div>

                                        <div className="text-right font-sans text-sm font-bold text-slate-800 min-w-[100px]">
                                            {formatVND(itemPrice * item.quantity)}
                                        </div>

                                        <button
                                            onClick={() => variantId && removeItem(variantId)}
                                            disabled={!variantId}
                                            aria-label="Xóa sản phẩm"
                                            className="p-2.5 text-slate-500 hover:text-red-600 hover:bg-red-500/10 rounded-xl transition"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>

                    {/* Cart Summary */}
                    <div className="lg:col-span-4 space-y-6">
                        <div className="p-6 bg-white rounded-2xl border border-slate-200 space-y-6">
                            <h2 className="text-lg font-semibold text-slate-800 normal-case tracking-normal pb-4 border-b border-slate-200">
                                TỔNG ĐƠN HÀNG
                            </h2>

                            <div className="space-y-3 font-sans text-xs">
                                <div className="flex justify-between text-slate-500">
                                    <span>Tạm tính ({totalCount} máy):</span>
                                    <span className="text-slate-800 font-semibold">
                                        {formatVND(subTotal)}
                                    </span>
                                </div>

                                <div className="flex justify-between text-slate-500">
                                    <span>Phí vận chuyển bảo hiểm:</span>
                                    <span className="text-slate-800 font-semibold">{formatVND(shippingFee)}</span>
                                </div>

                                <div className="pt-4 border-t border-slate-200 flex justify-between items-baseline">
                                    <span className="text-sm font-bold text-slate-800 normal-case">Tổng thanh toán:</span>
                                    <span className="text-2xl font-semibold text-sky-700">
                                        {formatVND(finalAmount)}
                                    </span>
                                </div>
                            </div>

                            <button
                                onClick={() => navigate(ROUTES.checkout)}
                                className="w-full bg-sky-600 text-white py-4 rounded-xl font-semibold text-sm normal-case tracking-normal hover:bg-sky-600 transition shadow-lg shadow-sky-700/20 flex items-center justify-center gap-2"
                            >
                                <span>TIẾN HÀNH THANH TOÁN</span>
                                <ChevronRight className="w-4 h-4 stroke-[3]" />
                            </button>

                            <div className="pt-2 text-[10px] text-slate-500 font-sans space-y-2">
                                <p className="flex items-center gap-2">
                                    <ShieldCheck className="w-3.5 h-3.5 text-sky-700" />
                                    Cam kết bảo mật thông tin thanh toán 100%
                                </p>
                                <p className="flex items-center gap-2">
                                    <Truck className="w-3.5 h-3.5 text-sky-700" />
                                    Giao hàng hỏa tốc trong 2h tại Hà Nội & TP.HCM
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </main>
    );
}
