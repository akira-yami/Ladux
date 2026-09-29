import React, { useState, useEffect, useRef } from "react";
import { ChevronLeft, ChevronRight, ShieldCheck, Truck, Shirt, Percent } from "lucide-react";

export interface ProductHeroProps {
    onShopNowClick?: () => void;
    onAiConsultClick?: () => void;
}

export default function ProductHero({ onShopNowClick }: ProductHeroProps) {
    const API_BASE = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1").replace("/api/v1", "");

    const slides = [
        {
            id: 1,
            url: `${API_BASE}/uploads/banners/banner_slide1.png`,
            fallback: "/uploads/banners/banner_slide1.png",
            alt: "Ladux Store Banner Slide 1",
            badge: "LADUX ULTIMATE GAMING & WORKSTATION",
        },
        {
            id: 2,
            url: `${API_BASE}/uploads/banners/banner_slide2.png`,
            fallback: "/uploads/banners/banner_slide2.png",
            alt: "Ladux Store Banner Slide 2",
            badge: "SIÊU PHẨM LAPTOP ULTRABOOK CAO CẤP",
        },
        {
            id: 3,
            url: `${API_BASE}/uploads/banners/banner_slide3.png`,
            fallback: "/uploads/banners/banner_slide3.png",
            alt: "Ladux Store Banner Slide 3",
            badge: "ƯU ĐÃI KHUYẾN MÃI ĐẶC BIỆT LADUX",
        },
        {
            id: 4,
            url: `${API_BASE}/uploads/banners/banner_slide4.png`,
            fallback: "/uploads/banners/banner_slide4.png",
            alt: "Ladux Store Banner Slide 4",
            badge: "BẢO HÀNH CHÍNH HÃNG 1 ĐỔI 1 TRONG 30 NGÀY",
        },
    ];

    const [currentIndex, setCurrentIndex] = useState<number>(0);
    const [isHovered, setIsHovered] = useState<boolean>(false);
    const touchStartX = useRef<number | null>(null);

    const prevSlide = (e?: React.MouseEvent) => {
        e?.stopPropagation();
        setCurrentIndex((prev) => (prev === 0 ? slides.length - 1 : prev - 1));
    };

    const nextSlide = (e?: React.MouseEvent) => {
        e?.stopPropagation();
        setCurrentIndex((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
    };

    useEffect(() => {
        const timer = setInterval(() => {
            setCurrentIndex((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
        }, 5000);
        return () => clearInterval(timer);
    }, [slides.length]);

    const handleTouchStart = (e: React.TouchEvent) => {
        touchStartX.current = e.touches[0].clientX;
    };

    const handleTouchEnd = (e: React.TouchEvent) => {
        if (touchStartX.current === null) return;
        const touchEndX = e.changedTouches[0].clientX;
        const diff = touchStartX.current - touchEndX;

        if (Math.abs(diff) > 40) {
            if (diff > 0) {
                nextSlide();
            } else {
                prevSlide();
            }
        }
        touchStartX.current = null;
    };

    const benefits = [
        {
            icon: ShieldCheck,
            title: "Chính Hãng New Seal 100%",
            desc: "Bảo hành 12 – 24 tháng",
        },
        {
            icon: Truck,
            title: "Giao Hàng Hỏa Tốc",
            desc: "Nội thành nhận ngay trong 2H",
        },
        {
            icon: Shirt,
            title: "Đổi Trả 7 Ngày",
            desc: "Thoải mái mua sắm",
        },
        {
            icon: Percent,
            title: "Trả Góp 0% Lãi Suất",
            desc: "Hỗ trợ qua thẻ tín dụng",
        },
    ];

    return (
        <section className="relative overflow-hidden py-3 lg:py-4 border-b border-white/[0.08] bg-black">
            {/* Ambient Cyber Background Glow */}
            <div className="pointer-events-none absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 h-[22rem] w-[45rem] rounded-full bg-[#00FF55]/[0.05] blur-3xl" />
            <div className="pointer-events-none absolute right-10 top-10 h-72 w-72 rounded-full bg-cyan-500/[0.03] blur-3xl" />

            <div className="container mx-auto px-6 sm:px-10 lg:px-16 space-y-3.5 sm:space-y-4">
                {/* ── Main Banner Carousel Container ── */}
                <div className="max-w-6xl mx-auto">
                    <div
                        onClick={onShopNowClick}
                        onMouseEnter={() => setIsHovered(true)}
                        onMouseLeave={() => setIsHovered(false)}
                        onTouchStart={handleTouchStart}
                        onTouchEnd={handleTouchEnd}
                        className="relative group w-full h-[220px] sm:h-[280px] md:h-[330px] lg:h-[370px] rounded-2xl lg:rounded-3xl overflow-hidden border border-white/10 bg-neutral-950 shadow-[0_15px_40px_rgba(0,0,0,0.8),0_0_20px_rgba(0,255,85,0.1)] select-none"
                    >
                        {/* Slide Images Container */}
                        <div
                            className="flex w-full h-full transition-transform duration-700 ease-out"
                            style={{ transform: `translateX(-${currentIndex * 100}%)` }}
                        >
                            {slides.map((slide) => (
                                <div key={slide.id} className="w-full h-full shrink-0 relative">
                                    <img
                                        src={slide.url}
                                        alt={slide.alt}
                                        className="w-full h-full block object-fill rounded-2xl lg:rounded-3xl"
                                        onError={(e) => {
                                            (e.currentTarget as HTMLImageElement).src = slide.fallback;
                                        }}
                                    />
                                    {/* Top Glass Badge */}
                                    <div className="absolute top-4 left-4 sm:top-6 sm:left-6 z-10 inline-flex items-center gap-2 rounded-full border border-[#00FF55]/40 bg-black/65 backdrop-blur-md px-3.5 py-1.5 shadow-lg">
                                        <div className="h-2 w-2 rounded-full bg-[#00FF55] animate-pulse" />
                                        <span className="text-[10px] sm:text-xs font-mono font-bold uppercase tracking-wider text-[#00FF55]">
                                            {slide.badge}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {/* Left Navigation Arrow */}
                        <button
                            type="button"
                            onClick={prevSlide}
                            aria-label="Previous Slide"
                            className="absolute left-3.5 top-1/2 -translate-y-1/2 z-10 w-11 h-11 rounded-full border border-white/20 bg-black/60 text-white flex items-center justify-center backdrop-blur-md transition-all duration-300 opacity-70 hover:opacity-100 hover:bg-[#00FF55] hover:text-black hover:border-[#00FF55] hover:scale-110 shadow-xl cursor-pointer"
                        >
                            <ChevronLeft className="w-6 h-6 stroke-[2.5]" />
                        </button>

                        {/* Right Navigation Arrow */}
                        <button
                            type="button"
                            onClick={nextSlide}
                            aria-label="Next Slide"
                            className="absolute right-3.5 top-1/2 -translate-y-1/2 z-10 w-11 h-11 rounded-full border border-white/20 bg-black/60 text-white flex items-center justify-center backdrop-blur-md transition-all duration-300 opacity-70 hover:opacity-100 hover:bg-[#00FF55] hover:text-black hover:border-[#00FF55] hover:scale-110 shadow-xl cursor-pointer"
                        >
                            <ChevronRight className="w-6 h-6 stroke-[2.5]" />
                        </button>

                        {/* Pagination Dots */}
                        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-10 flex items-center gap-2.5 bg-black/50 backdrop-blur-md px-3.5 py-1.5 rounded-full border border-white/10">
                            {slides.map((_, idx) => (
                                <button
                                    key={idx}
                                    type="button"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        setCurrentIndex(idx);
                                    }}
                                    aria-label={`Go to slide ${idx + 1}`}
                                    className={`h-2.5 rounded-full transition-all duration-300 cursor-pointer ${currentIndex === idx
                                        ? "w-8 bg-[#00FF55] shadow-[0_0_12px_#00FF55]"
                                        : "w-2.5 bg-white/40 hover:bg-white/80"
                                        }`}
                                />
                            ))}
                        </div>
                    </div>
                </div>

                {/* ── Sub-Hero Feature Assurance Bar ── */}
                <div className="max-w-6xl mx-auto rounded-xl sm:rounded-2xl border border-white/[0.08] bg-[#080c0e]/95 shadow-[0_10px_30px_rgba(0,0,0,0.5)] backdrop-blur-xl overflow-hidden">
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
                        {benefits.map((item, idx) => {
                            const Icon = item.icon;
                            return (
                                <div
                                    key={idx}
                                    className="group relative flex items-center gap-3 sm:gap-3.5 px-4 sm:px-5 lg:px-5 xl:px-6 py-3.5 sm:py-4 transition-colors duration-200 hover:bg-white/[0.02]"
                                >
                                    {/* Icon Box: Squarish rounded box matching the screenshot */}
                                    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#02491b] border border-[#03ef66]/20 text-[#03ef66] shadow-[0_0_15px_rgba(3,239,102,0.12)] transition-transform duration-200 group-hover:scale-105 group-hover:border-[#03ef66]/40">
                                        <Icon className="h-5 w-5 stroke-[2.2]" />
                                    </div>

                                    {/* Text Content */}
                                    <div className="min-w-0 flex-1">
                                        <h4 className="text-xs sm:text-sm font-bold text-white tracking-tight leading-snug truncate group-hover:text-white transition-colors">
                                            {item.title}
                                        </h4>
                                        <p className="text-[11px] sm:text-xs text-neutral-400 font-normal leading-snug truncate mt-0.5">
                                            {item.desc}
                                        </p>
                                    </div>

                                    {/* Vertical divider on desktop */}
                                    {idx < benefits.length - 1 && (
                                        <div className="hidden lg:block absolute right-0 top-1/2 -translate-y-1/2 h-8 w-[1px] bg-white/[0.08]" />
                                    )}

                                    {/* Vertical divider on tablet (column 1 right separator) */}
                                    {idx % 2 === 0 && (
                                        <div className="hidden sm:block lg:hidden absolute right-0 top-1/2 -translate-y-1/2 h-8 w-[1px] bg-white/[0.08]" />
                                    )}

                                    {/* Horizontal divider on tablet (between row 1 and row 2) */}
                                    {idx < 2 && (
                                        <div className="hidden sm:block lg:hidden absolute bottom-0 left-4 right-4 h-[1px] bg-white/[0.06]" />
                                    )}

                                    {/* Horizontal divider on mobile (between rows) */}
                                    {idx < benefits.length - 1 && (
                                        <div className="block sm:hidden absolute bottom-0 left-4 right-4 h-[1px] bg-white/[0.06]" />
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        </section>
    );
}
