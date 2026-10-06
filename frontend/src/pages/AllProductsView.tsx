import React, { useState, useMemo, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronRight, Search, X } from "lucide-react";
import { useProductStore, useWishlistStore } from "../stores";
import ProductCard from "../components/product/ProductCard";
import { LaptopProduct, mapProductResponseToLaptopProduct } from "../types";
import { productPath, ROUTES } from "../app/routePaths";

const SORT_OPTIONS = [
    { value: "featured", label: "Nổi bật nhất" },
    { value: "price-asc", label: "Giá: Thấp đến Cao" },
    { value: "price-desc", label: "Giá: Cao đến Thấp" },
    { value: "newest", label: "Mới nhất" },
    { value: "rating", label: "Đánh giá cao nhất" },
];

export interface AllProductsViewProps {
    allProducts?: LaptopProduct[];
    selectedCategory?: string;
    setSelectedCategory?: (category: string) => void;
    searchQuery?: string;
    setSearchQuery?: (query: string) => void;
    toggleWishlist?: (laptopId: number) => Promise<void>;
    setSelectedProduct: (product: LaptopProduct) => void;
    addToCartCustom?: (
        product: LaptopProduct,
        variantId: number,
        quantity: number
    ) => Promise<boolean>;
}

export default function AllProductsView({
    allProducts,
    selectedCategory,
    setSelectedCategory,
    searchQuery = "",
    setSearchQuery,
    toggleWishlist,
    setSelectedProduct,
    addToCartCustom,
}: AllProductsViewProps) {
    const navigate = useNavigate();
    const {
        products,
        brands,
        categories,
        filters,
        isLoading,
        fetchProducts,
        fetchBrands,
        fetchCategories,
        setBrandFilter,
        setCategoryFilter,
    } = useProductStore();

    const { wishlistProductIds } = useWishlistStore();

    const [sidebarSearch, setSidebarSearch] = useState<string>("");
    const [selectedRam, setSelectedRam] = useState<string>("All");
    const [selectedRom, setSelectedRom] = useState<string>("All");
    const [maxPrice, setMaxPrice] = useState<number>(Number.POSITIVE_INFINITY);
    const [sortBy, setSortBy] = useState<string>("newest");
    const [currentPage, setCurrentPage] = useState<number>(1);
    const [isMobileFiltersOpen, setIsMobileFiltersOpen] = useState(false);
    const itemsPerPage = 8;

    const [isSortDropdownOpen, setIsSortDropdownOpen] = useState<boolean>(false);
    const sortDropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (sortDropdownRef.current && !sortDropdownRef.current.contains(event.target as Node)) {
                setIsSortDropdownOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    // Trigger store fetch if products/brands/categories are not loaded yet
    useEffect(() => {
        if (products.length === 0) fetchProducts();
        if (brands.length === 0) fetchBrands();
        if (categories.length === 0) fetchCategories();
    }, []);

    // Chuyển đổi dữ liệu Backend ProductResponse DTO sang LaptopProduct UI model
    const mappedProducts = useMemo(() => {
        if (allProducts && allProducts.length > 0) {
            return allProducts;
        }
        return products.map((p) => mapProductResponseToLaptopProduct(p));
    }, [allProducts, products]);

    const ramOptions = useMemo(
        () => Array.from(new Set(mappedProducts.flatMap((product) => product.variants.map((variant) => variant.ram)).filter(Boolean))).sort(),
        [mappedProducts]
    );
    const romOptions = useMemo(
        () => Array.from(new Set(mappedProducts.flatMap((product) => product.variants.map((variant) => variant.rom)).filter(Boolean))).sort(),
        [mappedProducts]
    );
    const catalogMaxPrice = useMemo(() => {
        const highest = mappedProducts.reduce((max, product) => {
            const variantMax = product.variants.reduce(
                (current, variant) => Math.max(current, Number(variant.discountPrice || variant.price)),
                0
            );
            return Math.max(max, variantMax);
        }, 0);
        return highest > 0 ? Math.ceil(highest / 1000000) * 1000000 : 0;
    }, [mappedProducts]);

    // Combined search & filter logic for Brand, Category, Header Search, Sidebar Filter Search, RAM, ROM, Price & Sorting
    const filteredProducts = useMemo(() => {
        let list = [...mappedProducts];

        // 1. Brand Filter
        if (filters.brandId !== null) {
            const foundBrand = brands.find((b) => b.id === filters.brandId);
            list = list.filter((p) => {
                if (p.brandId != null && p.brandId === filters.brandId) return true;
                if (foundBrand && (p.brand || "").toLowerCase() === foundBrand.name.toLowerCase()) return true;
                return false;
            });
        }

        // 2. Category Filter
        if (selectedCategory && selectedCategory !== "All") {
            const catLower = selectedCategory.toLowerCase();
            list = list.filter((p) => (p.category || "").toLowerCase() === catLower);
        } else if (filters.categoryId !== null) {
            const foundCat = categories.find((c) => c.id === filters.categoryId);
            list = list.filter((p) => {
                if (p.categoryId != null && p.categoryId === filters.categoryId) return true;
                if (foundCat && (p.category || "").toLowerCase() === foundCat.name.toLowerCase()) return true;
                return false;
            });
        }

        // 3. Header Search Query filter (from Header search bar)
        const headerQuery = (searchQuery || "").toLowerCase().trim();
        if (headerQuery) {
            list = list.filter((p) => {
                if (!p) return false;
                const name = (p.name || "").toLowerCase();
                const brand = (p.brand || "").toLowerCase();
                const category = (p.category || "").toLowerCase();
                const cpu = (p.cpu || "").toLowerCase();
                const gpu = (p.gpu || "").toLowerCase();
                const ram = (p.ram || "").toLowerCase();
                const rom = (p.rom || "").toLowerCase();
                return (
                    name.includes(headerQuery) ||
                    brand.includes(headerQuery) ||
                    category.includes(headerQuery) ||
                    cpu.includes(headerQuery) ||
                    gpu.includes(headerQuery) ||
                    ram.includes(headerQuery) ||
                    rom.includes(headerQuery)
                );
            });
        }

        // 4. Sidebar Filter Text (independent search inside sidebar filter)
        const sidebarQuery = sidebarSearch.toLowerCase().trim();
        if (sidebarQuery) {
            list = list.filter((p) => {
                if (!p) return false;
                const name = (p.name || "").toLowerCase();
                const brand = (p.brand || "").toLowerCase();
                const category = (p.category || "").toLowerCase();
                const cpu = (p.cpu || "").toLowerCase();
                const gpu = (p.gpu || "").toLowerCase();
                const ram = (p.ram || "").toLowerCase();
                const rom = (p.rom || "").toLowerCase();
                return (
                    name.includes(sidebarQuery) ||
                    brand.includes(sidebarQuery) ||
                    category.includes(sidebarQuery) ||
                    cpu.includes(sidebarQuery) ||
                    gpu.includes(sidebarQuery) ||
                    ram.includes(sidebarQuery) ||
                    rom.includes(sidebarQuery)
                );
            });
        }

        // 5. RAM filter
        if (selectedRam !== "All") {
            list = list.filter((p) => p.variants.some((variant) => variant.ram === selectedRam));
        }

        // 6. ROM filter
        if (selectedRom !== "All") {
            list = list.filter((p) => p.variants.some((variant) => variant.rom === selectedRom));
        }

        // 7. Price filter (Safely check undefined/null)
        list = list.filter((p) => {
            const cost = p.discountPrice ?? p.price ?? 0;
            return cost <= maxPrice;
        });

        // 8. Sorting
        if (sortBy === "price-asc") {
            list.sort((a, b) => (a.discountPrice || a.price) - (b.discountPrice || b.price));
        } else if (sortBy === "price-desc") {
            list.sort((a, b) => (b.discountPrice || b.price) - (a.discountPrice || a.price));
        } else if (sortBy === "newest") {
            list.sort((a, b) => b.id - a.id);
        } else if (sortBy === "rating") {
            list.sort((a, b) => b.rating - a.rating);
        }

        return list;
    }, [mappedProducts, selectedCategory, filters.brandId, filters.categoryId, brands, categories, searchQuery, sidebarSearch, selectedRam, selectedRom, maxPrice, sortBy]);

    // Pagination
    const totalPages = Math.ceil(filteredProducts.length / itemsPerPage) || 1;
    const paginatedProducts = useMemo(() => {
        const start = (currentPage - 1) * itemsPerPage;
        return filteredProducts.slice(start, start + itemsPerPage);
    }, [filteredProducts, currentPage]);

    return (
        <div className="bg-white text-slate-800 min-h-screen py-8">
            <div className="container mx-auto px-5 sm:px-6">
                {/* ── Breadcrumb & Top Bar ── */}
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
                    <div>
                        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium mb-2">
                            <button
                                onClick={() => navigate(ROUTES.home)}
                                className="hover:text-slate-800 transition-colors"
                            >
                                Trang chủ
                            </button>
                            <ChevronRight className="w-3 h-3 text-slate-400" />
                            <span className="text-slate-600">Cửa hàng</span>
                        </div>
                        <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight text-slate-800 normal-case">
                            Laptop phù hợp với bạn
                        </h1>
                        <p className="text-xs text-slate-500 font-sans mt-1">
                            {filteredProducts.length} sản phẩm
                        </p>
                    </div>

                    {/* Top Right Sort */}
                    <div className="flex items-center gap-3">
                        <span className="text-xs text-slate-500 font-medium hidden sm:inline">Sắp xếp:</span>
                        <div className="relative" ref={sortDropdownRef}>
                            <button
                                type="button"
                                onClick={() => setIsSortDropdownOpen(!isSortDropdownOpen)}
                                className="flex items-center gap-2.5 bg-white border border-sky-100 hover:border-sky-700/60 rounded-full px-5 py-2 text-xs font-bold text-slate-800 transition-all cursor-pointer shadow-md shadow-black/40"
                            >
                                <span>{SORT_OPTIONS.find((o) => o.value === sortBy)?.label || "Nổi bật nhất"}</span>
                                <ChevronRight
                                    className={`w-3.5 h-3.5 text-sky-700 transition-transform duration-300 ${
                                        isSortDropdownOpen ? "-rotate-90" : "rotate-90"
                                    }`}
                                />
                            </button>

                            {isSortDropdownOpen && (
                                <div className="absolute right-0 top-[calc(100%+8px)] w-52 rounded-2xl border border-sky-100 bg-white backdrop-blur-2xl p-1.5 shadow-sm z-50 animate-in fade-in zoom-in-95 duration-150">
                                    {SORT_OPTIONS.map((option) => {
                                        const isSelected = sortBy === option.value;
                                        return (
                                            <button
                                                key={option.value}
                                                type="button"
                                                onClick={() => {
                                                    setSortBy(option.value);
                                                    setCurrentPage(1);
                                                    setIsSortDropdownOpen(false);
                                                }}
                                                className={`w-full text-left px-3.5 py-2.5 rounded-xl text-xs font-medium transition-all flex items-center justify-between cursor-pointer ${
                                                    isSelected
                                                        ? "bg-sky-600 text-white font-semibold shadow-md shadow-sky-700/20"
                                                        : "text-slate-600 hover:bg-sky-50 hover:text-sky-700"
                                                }`}
                                            >
                                                <span>{option.label}</span>
                                                {isSelected && <div className="h-1.5 w-1.5 rounded-full bg-white shrink-0" />}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* ── Main Layout: Sidebar Filter + Products Grid ── */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
                    {/* Left Sidebar Filter Column */}
                    <div className="lg:col-span-3">
                    <button type="button" onClick={() => setIsMobileFiltersOpen(open => !open)} aria-expanded={isMobileFiltersOpen} aria-controls="catalog-filters" className="mb-3 flex w-full items-center justify-between rounded-2xl border border-sky-100 bg-sky-50 px-5 py-3 text-sm font-semibold text-sky-700 lg:hidden">
                        Bộ lọc sản phẩm <ChevronRight className={`h-4 w-4 transition ${isMobileFiltersOpen ? '-rotate-90' : 'rotate-90'}`} />
                    </button>
                    <aside id="catalog-filters" className={`${isMobileFiltersOpen ? 'block' : 'hidden lg:block'} space-y-8 bg-white p-6 rounded-2xl border border-sky-100 store-card`}>
                        {/* 0. TÌM KIẾM BỘ LỌC ĐỘC LẬP */}
                        <div>
                            <div className="flex items-center justify-between mb-3">
                                <h3 className="text-xs font-sans font-bold tracking-normal text-slate-500 normal-case">
                                    Tìm nhanh
                                </h3>
                                {(sidebarSearch || searchQuery || (selectedCategory && selectedCategory !== "All") || selectedRam !== "All" || selectedRom !== "All" || Number.isFinite(maxPrice) || filters.brandId !== null || filters.categoryId !== null) && (
                                    <button
                                        onClick={() => {
                                            setSidebarSearch("");
                                            if (setSearchQuery) setSearchQuery("");
                                            if (setSelectedCategory) setSelectedCategory("All");
                                            setSelectedRam("All");
                                            setSelectedRom("All");
                                            setMaxPrice(Number.POSITIVE_INFINITY);
                                            setBrandFilter(null);
                                            setCategoryFilter(null);
                                            setCurrentPage(1);
                                        }}
                                        className="text-[11px] font-bold text-sky-700 hover:underline flex items-center gap-1 cursor-pointer"
                                    >
                                        <X className="w-3.5 h-3.5" />
                                        <span>Đặt lại</span>
                                    </button>
                                )}
                            </div>
                            <div className="relative">
                                <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                                <input
                                    type="text"
                                    value={sidebarSearch}
                                    onChange={(e) => {
                                        setSidebarSearch(e.target.value);
                                        setCurrentPage(1);
                                    }}
                                    placeholder="Lọc tên laptop, CPU..."
                                    className="w-full bg-white border border-sky-100 rounded-xl pl-9 pr-8 py-2.5 text-xs text-slate-800 placeholder-neutral-500 focus:outline-none focus:border-sky-700 transition font-medium"
                                />
                                {sidebarSearch && (
                                    <button
                                        onClick={() => setSidebarSearch("")}
                                        className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-800"
                                    >
                                        <X className="w-3.5 h-3.5" />
                                    </button>
                                )}
                            </div>
                        </div>
                        <div>
                            <h3 className="text-xs font-sans font-bold tracking-normal text-slate-500 normal-case mb-3">
                                Thương hiệu
                            </h3>
                            <div className="space-y-1.5 flex flex-col items-start">
                                <button
                                    onClick={() => {
                                        setBrandFilter(null);
                                        setCurrentPage(1);
                                    }}
                                    className={`text-xs font-bold transition-all text-left ${filters.brandId === null
                                            ? "bg-sky-600 text-white rounded-full py-1.5 px-4 shadow-md shadow-sky-700/20"
                                            : "text-slate-500 hover:text-slate-800 py-1.5 px-4"
                                        }`}
                                >
                                    Tất cả thương hiệu
                                </button>
                                {brands.map((b) => {
                                    const isActive = filters.brandId === b.id;
                                    return (
                                        <button
                                            key={b.id}
                                            onClick={() => {
                                                setBrandFilter(b.id);
                                                setCurrentPage(1);
                                            }}
                                            className={`text-xs font-bold transition-all text-left ${isActive
                                                    ? "bg-sky-600 text-white rounded-full py-1.5 px-4 shadow-md shadow-sky-700/20"
                                                    : "text-slate-500 hover:text-slate-800 py-1.5 px-4"
                                                }`}
                                        >
                                            {b.name}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>

                        {/* 2. Nhu cầu sử dụng / DANH MỤC ĐỘNG TỪ BACKEND */}
                        <div className="pt-4 border-t border-sky-100">
                            <h3 className="text-xs font-sans font-bold tracking-normal text-slate-500 normal-case mb-3">
                                Nhu cầu sử dụng
                            </h3>
                            <div className="space-y-1.5 flex flex-col items-start">
                                <button
                                    onClick={() => {
                                        if (setSelectedCategory) setSelectedCategory("All");
                                        setCategoryFilter(null);
                                        setCurrentPage(1);
                                    }}
                                    className={`text-xs font-bold transition-all text-left ${
                                        filters.categoryId === null && (!selectedCategory || selectedCategory === "All")
                                            ? "bg-sky-600 text-white rounded-full py-1.5 px-4 shadow-md shadow-sky-700/20"
                                            : "text-slate-500 hover:text-slate-800 py-1.5 px-4"
                                    }`}
                                >
                                    Tất cả dòng máy
                                </button>
                                {categories.map((c) => {
                                    const isActive =
                                        filters.categoryId === c.id ||
                                        (selectedCategory && selectedCategory.toLowerCase() === c.name.toLowerCase());
                                    return (
                                        <button
                                            key={c.id}
                                            onClick={() => {
                                                if (isActive) {
                                                    if (setSelectedCategory) setSelectedCategory("All");
                                                    setCategoryFilter(null);
                                                } else {
                                                    if (setSelectedCategory) setSelectedCategory(c.name);
                                                    setCategoryFilter(c.id);
                                                }
                                                setCurrentPage(1);
                                            }}
                                            className={`text-xs font-bold transition-all text-left flex items-center justify-between gap-2 cursor-pointer ${
                                                isActive
                                                    ? "bg-sky-600 text-white rounded-full py-1.5 px-4 shadow-md shadow-sky-700/20"
                                                    : "text-slate-500 hover:text-slate-800 py-1.5 px-4"
                                            }`}
                                        >
                                            <span>{c.name}</span>
                                            {isActive && <X className="w-3 h-3 text-white shrink-0" />}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>

                        {/* 3. Khoảng giá */}
                        <div className="pt-4 border-t border-sky-100">
                            <div className="flex justify-between items-center mb-3">
                                <h3 className="text-xs font-sans font-bold tracking-normal text-slate-500 normal-case">
                                    Khoảng giá
                                </h3>
                                <button
                                    onClick={() => setMaxPrice(Number.POSITIVE_INFINITY)}
                                    className="text-[11px] font-medium text-sky-700 hover:underline"
                                >
                                    Tất cả
                                </button>
                            </div>
                            {catalogMaxPrice > 0 && (
                                <>
                                    <input
                                        type="range"
                                        min={0}
                                        max={catalogMaxPrice}
                                        step={1000000}
                                        value={Number.isFinite(maxPrice) ? maxPrice : catalogMaxPrice}
                                        onChange={(e) => {
                                            setMaxPrice(Number(e.target.value));
                                            setCurrentPage(1);
                                        }}
                                        className="w-full accent-sky-700 bg-slate-100 rounded-lg cursor-pointer h-2"
                                    />
                                    <div className="flex justify-between text-[11px] font-sans text-slate-500 mt-2">
                                        <span>0 ₫</span>
                                        <span>{new Intl.NumberFormat("vi-VN").format(catalogMaxPrice)} ₫</span>
                                    </div>
                                </>
                            )}
                        </div>

                        {/* 4. Bộ nhớ RAM */}
                        <div className="pt-4 border-t border-sky-100">
                            <h3 className="text-xs font-sans font-bold tracking-normal text-slate-500 normal-case mb-3">
                                Bộ nhớ RAM
                            </h3>
                            <div className="flex flex-wrap gap-2">
                                {["All", ...ramOptions].map((value) => {
                                    const item = { label: value === "All" ? "Tất cả" : value, value };
                                    const isActive = selectedRam === item.value;
                                    return (
                                        <button
                                            key={item.value}
                                            onClick={() => {
                                                setSelectedRam(item.value);
                                                setCurrentPage(1);
                                            }}
                                            className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all ${isActive
                                                    ? "bg-sky-600 text-white shadow-md shadow-sky-700/20"
                                                    : "border border-slate-200 text-slate-600 hover:border-neutral-600"
                                                }`}
                                        >
                                            {item.label}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>

                        {/* 5. Dung lượng lưu trữ */}
                        <div className="pt-4 border-t border-sky-100">
                            <h3 className="text-xs font-sans font-bold tracking-normal text-slate-500 normal-case mb-3">
                                Dung lượng lưu trữ
                            </h3>
                            <div className="flex flex-wrap gap-2">
                                {["All", ...romOptions].map((value) => {
                                    const item = { label: value === "All" ? "Tất cả" : value, value };
                                    const isActive = selectedRom === item.value;
                                    return (
                                        <button
                                            key={item.value}
                                            onClick={() => {
                                                setSelectedRom(item.value);
                                                setCurrentPage(1);
                                            }}
                                            className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all ${isActive
                                                    ? "bg-sky-600 text-white shadow-md shadow-sky-700/20"
                                                    : "border border-slate-200 text-slate-600 hover:border-neutral-600"
                                                }`}
                                        >
                                            {item.label}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>
                    </aside>
                    </div>

                    {/* Right Products Grid Column */}
                    <main className="lg:col-span-9 space-y-8">
                        {isLoading ? (
                            <div className="text-center py-20 bg-white rounded-2xl border border-sky-100">
                                <div className="inline-block w-8 h-8 border-2 border-sky-700 border-t-transparent rounded-full animate-spin mb-3"></div>
                                <p className="text-slate-500 text-xs font-sans">Đang tải sản phẩm từ Database...</p>
                            </div>
                        ) : paginatedProducts.length > 0 ? (
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                                {paginatedProducts.map((laptop) => (
                                    <ProductCard
                                        key={laptop.id}
                                        laptop={laptop}
                                        isWishlisted={wishlistProductIds.includes(laptop.id)}
                                        onToggleWishlist={(id) => {
                                            void toggleWishlist?.(id);
                                        }}
                                        onSelectProduct={(p) => {
                                            setSelectedProduct(p);
                                            navigate(productPath(p.id));
                                        }}
                                        onAddToCart={(p) => {
                                            const variant = p.variants.find((item) => item.id === p.defaultVariantId && item.stockQuantity > 0)
                                                ?? p.variants.find((item) => item.stockQuantity > 0);
                                            if (variant) void addToCartCustom?.(p, variant.id, 1);
                                        }}
                                    />
                                ))}
                            </div>
                        ) : (
                            <div className="text-center py-16 bg-white rounded-2xl border border-sky-100">
                                <p className="text-slate-500 text-sm font-medium">
                                    Không tìm thấy sản phẩm nào phù hợp với bộ lọc hiện tại.
                                </p>
                                <button
                                    onClick={() => {
                                        setBrandFilter(null);
                                        setCategoryFilter(null);
                                        setSelectedRam("All");
                                        setSelectedRom("All");
                                        setMaxPrice(Number.POSITIVE_INFINITY);
                                    }}
                                    className="mt-4 bg-sky-600 text-white font-bold text-xs px-5 py-2 rounded-full hover:bg-sky-600 transition-colors"
                                >
                                    Xóa tất cả bộ lọc
                                </button>
                            </div>
                        )}

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="flex items-center justify-center gap-2 pt-8 border-t border-sky-100">
                                <button
                                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                                    disabled={currentPage === 1}
                                    className="px-4 py-2 rounded-full border border-slate-200 text-xs font-medium text-slate-500 hover:border-neutral-600 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                                >
                                    &larr; Trước
                                </button>

                                {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => {
                                    const isActive = currentPage === pageNum;
                                    return (
                                        <button
                                            key={pageNum}
                                            onClick={() => setCurrentPage(pageNum)}
                                            className={`w-8 h-8 rounded-full text-xs font-bold flex items-center justify-center transition-all ${isActive
                                                    ? "bg-sky-600 text-white shadow-md shadow-sky-700/20"
                                                    : "border border-slate-200 text-slate-600 hover:border-neutral-600"
                                                }`}
                                        >
                                            {pageNum}
                                        </button>
                                    );
                                })}

                                <button
                                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                                    disabled={currentPage === totalPages}
                                    className="px-4 py-2 rounded-full border border-slate-200 text-xs font-medium text-slate-500 hover:border-neutral-600 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                                >
                                    Sau &rarr;
                                </button>
                            </div>
                        )}
                    </main>
                </div>
            </div>
        </div>
    );
}
