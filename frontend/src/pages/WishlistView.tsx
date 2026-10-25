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
            <div className="flex items-center justify-between border-b border-slate-200 dark:border-neutral-800 pb-6 mb-8">
                <div>
                    <span className="text-xs font-sans font-bold uppercase tracking-wider text-sky-600 dark:text-sky-400">
                        LADUX FAVORITES ({mappedWishlistProducts.length})
                    </span>
                    <h1 className="text-3xl font-semibold text-slate-800 dark:text-white tracking-tight mt-1">
                        DANH SÁCH LAPTOP YÊU THÍCH
                    </h1>
                </div>
                <button
                    onClick={() => navigate(ROUTES.products)}
                    className="text-xs font-bold normal-case tracking-normal text-slate-500 dark:text-neutral-400 hover:text-sky-600 dark:hover:text-white transition cursor-pointer"
                >
                    ← Trở lại cửa hàng
                </button>
            </div>

            {isLoading ? (
                <div className="py-20 text-center space-y-4">
                    <div className="inline-block w-8 h-8 border-2 border-sky-600 dark:border-sky-400 border-t-transparent rounded-full animate-spin"></div>
                    <p className="text-slate-500 dark:text-neutral-400 text-xs font-sans">Đang nạp danh sách yêu thích từ máy chủ...</p>
                </div>
            ) : mappedWishlistProducts.length === 0 ? (
                <div className="py-20 text-center space-y-6 max-w-md mx-auto">
                    <div className="w-24 h-24 rounded-full bg-white dark:bg-neutral-800 border border-slate-200 dark:border-neutral-700 flex items-center justify-center mx-auto text-slate-400">
                        <Heart className="w-10 h-10 stroke-[1.5]" />
                    </div>
                    <div className="space-y-2">
                        <h2 className="text-xl font-bold text-slate-800 dark:text-white">Chưa có sản phẩm yêu thích</h2>
                        <p className="text-xs text-slate-500 dark:text-neutral-400">
                            Hãy nhấn vào biểu tượng trái tim ở các sản phẩm Laptop cao cấp để lưu lại danh sách quan tâm của bạn.
                        </p>
                    </div>
                    <button
                        onClick={() => navigate(ROUTES.products)}
                        className="bg-sky-600 text-white font-semibold px-8 py-3.5 rounded-xl text-xs normal-case tracking-normal hover:bg-sky-500 transition shadow-lg shadow-sky-700/20 cursor-pointer"
                    >
                        Khám phá Laptop ngay
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {mappedWishlistProducts.map((laptop) => (
                        <div
                            key={laptop.id}
                            className="store-card group relative overflow-hidden rounded-2xl border border-sky-100 dark:border-white/10 bg-white dark:bg-[#1e1e1c] shadow-sm hover:shadow-xl transition-all duration-300 hover:-translate-y-1 flex flex-col justify-between"
                        >
                            <div>
                                <div className="relative aspect-[4/3] bg-slate-50 dark:bg-neutral-900/60 overflow-hidden">
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            storeToggleWishlist(laptop.id);
                                        }}
                                        aria-label="Xóa khỏi yêu thích"
                                        className="absolute right-3 top-3 z-20 flex h-9 w-9 items-center justify-center rounded-full border border-white/20 bg-black/40 hover:bg-black/60 text-white backdrop-blur-md transition-all duration-300 hover:scale-110 shadow-md cursor-pointer"
                                    >
                                        <Heart className="h-4 w-4 fill-red-500 text-red-500" />
                                    </button>
                                    <img
                                        src={laptop.images[0]}
                                        alt={laptop.name}
                                        className="store-card__image w-full h-full object-cover dark:mix-blend-normal transition-transform duration-500 group-hover:scale-105"
                                    />
                                </div>
                                <div className="p-5">
                                    <div className="flex items-center justify-between text-xs font-sans mb-2">
                                        <span className="uppercase tracking-wider font-semibold text-slate-400 dark:text-neutral-400">{laptop.brand}</span>
                                        <span className="flex items-center gap-1 font-medium text-amber-500 dark:text-amber-400">
                                            <Star className="w-3.5 h-3.5 fill-amber-500 text-amber-500" /> {laptop.rating ? laptop.rating.toFixed(1) : "0"} ({laptop.reviewCount || 0})
                                        </span>
                                    </div>
                                    <h3
                                        onClick={() => {
                                            setSelectedProduct(laptop);
                                            navigate(productPath(laptop.id));
                                        }}
                                        className="font-bold text-sm sm:text-base leading-snug line-clamp-2 hover:underline cursor-pointer min-h-[2.5rem] text-slate-800 dark:text-white hover:text-sky-600 dark:hover:text-sky-400 transition-colors"
                                    >
                                        {laptop.name}
                                    </h3>
                                    <p className="text-xs text-slate-500 dark:text-neutral-400 line-clamp-2 mt-2 leading-relaxed">
                                        {laptop.description}
                                    </p>
                                </div>
                            </div>

                            <div className="p-5 pt-0 border-t border-slate-100 dark:border-white/10 mt-3">
                                <div className="my-3 text-lg font-bold font-sans text-sky-600 dark:text-sky-400">
                                    {formatVND(laptop.discountPrice ?? laptop.price)}
                                </div>
                                <div className="grid grid-cols-2 gap-2.5">
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setSelectedProduct(laptop);
                                            navigate(productPath(laptop.id));
                                        }}
                                        className="border border-slate-200 dark:border-white/20 hover:border-slate-300 dark:hover:border-white/40 py-2.5 rounded-xl text-xs font-semibold text-slate-700 dark:text-white hover:bg-slate-50 dark:hover:bg-white/10 transition cursor-pointer text-center"
                                    >
                                        Chi Tiết
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setSelectedProduct(laptop);
                                            navigate(productPath(laptop.id));
                                        }}
                                        className="bg-sky-600 hover:bg-sky-500 text-white py-2.5 rounded-xl text-xs font-bold transition shadow-md shadow-sky-600/20 cursor-pointer text-center"
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
