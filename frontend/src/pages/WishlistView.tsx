import React from "react";
import { useNavigate } from "react-router-dom";
import { Heart, Star } from "lucide-react";
import { LaptopProduct, formatVND, mapProductResponseToLaptopProduct } from "../types";
import { useWishlistStore } from "../stores";
import { productPath, ROUTES } from "../app/routePaths";

export interface WishlistViewProps {
    setSelectedProduct: (product: LaptopProduct) => void;
}

export default function WishlistView({
    setSelectedProduct,
}: WishlistViewProps) {
    const navigate = useNavigate();
    const { wishlistItems, toggleWishlist: storeToggleWishlist, isLoading } = useWishlistStore();

    const mappedWishlistProducts = wishlistItems
        .filter((w) => w.product !== null)
        .map((w) => mapProductResponseToLaptopProduct(w.product!));

    return (
        <main className="container mx-auto px-6 py-12 max-w-6xl">
            <div className="flex items-center justify-between border-b border-slate-200 pb-6 mb-8">
                <div>
                    <span className="text-xs font-sans normal-case tracking-normal text-sky-700">
                        LADUX FAVORITES ({mappedWishlistProducts.length})
                    </span>
                    <h1 className="text-3xl font-semibold text-slate-800 tracking-tight mt-1">
                        DANH SÁCH LAPTOP YÊU THÍCH
                    </h1>
                </div>
                <button
                    onClick={() => navigate(ROUTES.products)}
                    className="text-xs font-bold normal-case tracking-normal text-slate-500 hover:text-sky-700 transition"
                >
                    ← Trở lại cửa hàng
                </button>
            </div>

            {isLoading ? (
                <div className="py-20 text-center space-y-4">
                    <div className="inline-block w-8 h-8 border-2 border-sky-700 border-t-transparent rounded-full animate-spin"></div>
                    <p className="text-slate-500 text-xs font-sans">Đang nạp danh sách yêu thích từ máy chủ...</p>
                </div>
            ) : mappedWishlistProducts.length === 0 ? (
                <div className="py-20 text-center space-y-6 max-w-md mx-auto">
                    <div className="w-24 h-24 rounded-full bg-white border border-slate-200 flex items-center justify-center mx-auto text-slate-400">
                        <Heart className="w-10 h-10 stroke-[1.5]" />
                    </div>
                    <div className="space-y-2">
                        <h2 className="text-xl font-bold text-slate-800">Chưa có sản phẩm yêu thích</h2>
                        <p className="text-xs text-slate-500">
                            Hãy nhấn vào biểu tượng trái tim ở các sản phẩm Laptop cao cấp để lưu lại danh sách quan tâm của bạn.
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
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {mappedWishlistProducts.map((laptop) => (
                        <div
                            key={laptop.id}
                            className="store-card store-card-hover relative overflow-hidden rounded-2xl border border-sky-100 bg-sky-50 flex flex-col justify-between"
                        >
                            <div>
                                <div className="relative aspect-[4/3] bg-white overflow-hidden">
                                    <div className="absolute inset-0 z-10 bg-gradient-to-b from-black/35 via-transparent to-transparent pointer-events-none" />
                                    <button
                                        onClick={() => storeToggleWishlist(laptop.id)}
                                        aria-label="Xóa khỏi yêu thích"
                                        className="absolute right-3 top-3 z-20 flex h-10 w-10 items-center justify-center rounded-full border border-sky-700/60 bg-sky-600 text-white backdrop-blur-md transition-all duration-300 hover:scale-110"
                                    >
                                        <Heart className="h-4 w-4 fill-current" />
                                    </button>
                                    <img
                                        src={laptop.images[0]}
                                        alt={laptop.name}
                                        className="store-card__image w-full h-full object-cover"
                                    />
                                </div>
                                <div className="p-5">
                                    <div className="flex items-center justify-between text-xs text-slate-500 font-sans mb-2">
                                        <span>{laptop.brand}</span>
                                        <span className="flex items-center gap-1 text-amber-700">
                                            <Star className="w-3.5 h-3.5 fill-amber-600" /> {laptop.rating} (
                                            {laptop.reviewCount})
                                        </span>
                                    </div>
                                    <h3
                                        onClick={() => {
                                            setSelectedProduct(laptop);
                                            navigate(productPath(laptop.id));
                                        }}
                                        className="font-bold text-sm leading-snug line-clamp-2 hover:underline cursor-pointer min-h-[2.5rem] text-slate-800"
                                    >
                                        {laptop.name}
                                    </h3>
                                    <p className="text-xs text-slate-500 line-clamp-2 mt-2 leading-relaxed">
                                        {laptop.description}
                                    </p>
                                </div>
                            </div>

                            <div className="p-5 pt-0 border-t border-slate-200 mt-2">
                                <div className="my-3 text-lg font-bold font-sans text-sky-700">
                                    {formatVND(laptop.discountPrice || laptop.price)}
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                    <button
                                        onClick={() => {
                                            setSelectedProduct(laptop);
                                            navigate(productPath(laptop.id));
                                        }}
                                        className="border border-slate-200 hover:border-sky-100 py-2.5 rounded-xl text-xs font-semibold text-slate-600 transition"
                                    >
                                        Chi Tiết
                                    </button>
                                    <button
                                        onClick={() => {
                                            setSelectedProduct(laptop);
                                            navigate(productPath(laptop.id));
                                        }}
                                        className="bg-sky-600 text-white py-2.5 rounded-xl text-xs font-bold hover:bg-sky-600 transition"
                                    >
                                        Chọn Cấu Hình
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </main>
    );
}
