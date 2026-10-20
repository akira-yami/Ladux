import React from "react";
import { Link, NavLink, useNavigate, useLocation } from "react-router-dom";
import {
    Search,
    Heart,
    ShoppingCart,
    Bell,
    Lock,
    User,
    ChevronRight,
    Phone,
    MapPin,
    LogOut,
    Package,
    X,
    Trash2,
} from "lucide-react";
import BrandMark from "./BrandMark";
import ThemeToggle from "./ThemeToggle";
import SmartSearchDropdown from "./SmartSearchDropdown";
import { getAvatarUrl } from "../../types";
import { ROUTES } from "../../app/routePaths";
import { useAuthStore, useNotificationStore, useOrderStore, useProductStore, useWishlistStore } from "../../stores";
import { STOREFRONT_CONTACT } from "../../config/storefront";

export interface HeaderProps {
    searchQuery: string;
    setSearchQuery: (query: string) => void;
    selectedCategory: string;
    setSelectedCategory: (category: string) => void;
    wishlistCount: number;
    cartCount: number;
    isLoggedIn: boolean;
    userAvatar?: string;
    userName?: string;
}

export default function Header({
    searchQuery,
    setSearchQuery,
    selectedCategory,
    setSelectedCategory,
    wishlistCount,
    cartCount,
    isLoggedIn: propsIsLoggedIn,
    userAvatar: propsUserAvatar,
    userName: propsUserName,
}: HeaderProps) {
    const navigate = useNavigate();
    const location = useLocation();
    const [mobileSearchOpen, setMobileSearchOpen] = React.useState(false);
    const closeMobileSearch = React.useCallback(() => setMobileSearchOpen(false), []);

    // Dynamic data from Zustand stores
    const user = useAuthStore((state) => state.user);
    const authIsLoggedIn = useAuthStore((state) => state.isLoggedIn);
    const logout = useAuthStore((state) => state.logout);

    const orders = useOrderStore((state) => state.orders);
    const totalOrderElements = useOrderStore((state) => state.totalElements);
    const wishlistProductIds = useWishlistStore((state) => state.wishlistProductIds);
    const categories = useProductStore((state) => state.categories);

    const notifications = useNotificationStore((state) => state.notifications);
    const unreadCount = useNotificationStore((state) => state.unreadCount);
    const fetchNotifications = useNotificationStore((state) => state.fetchNotifications);
    const fetchUnreadNotifications = useNotificationStore((state) => state.fetchUnreadNotifications);
    const fetchReadNotifications = useNotificationStore((state) => state.fetchReadNotifications);
    const markAsRead = useNotificationStore((state) => state.markAsRead);
    const markAllAsRead = useNotificationStore((state) => state.markAllAsRead);
    const deleteNotification = useNotificationStore((state) => state.deleteNotification);
    const deleteAllNotifications = useNotificationStore((state) => state.deleteAllNotifications);

    const [notifTab, setNotifTab] = React.useState<"all" | "unread" | "read">("all");
    const [isCatDropdownOpen, setIsCatDropdownOpen] = React.useState<boolean>(false);
    const catDropdownRef = React.useRef<HTMLDivElement>(null);

    const [isNotifOpen, setIsNotifOpen] = React.useState<boolean>(false);
    const notifRef = React.useRef<HTMLDivElement>(null);

    const [isUserMenuOpen, setIsUserMenuOpen] = React.useState<boolean>(false);
    const userMenuRef = React.useRef<HTMLDivElement>(null);

    React.useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            const target = event.target as Node;
            if (catDropdownRef.current && !catDropdownRef.current.contains(target)) {
                setIsCatDropdownOpen(false);
            }
            if (notifRef.current && !notifRef.current.contains(target)) {
                setIsNotifOpen(false);
            }
            if (userMenuRef.current && !userMenuRef.current.contains(target)) {
                setIsUserMenuOpen(false);
            }
        };

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setIsCatDropdownOpen(false);
                setIsNotifOpen(false);
                setIsUserMenuOpen(false);
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        document.addEventListener("keydown", handleKeyDown);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, []);

    React.useEffect(() => {
        setIsCatDropdownOpen(false);
        setIsNotifOpen(false);
        setIsUserMenuOpen(false);
    }, [location.pathname]);

    const [isScrolled, setIsScrolled] = React.useState<boolean>(false);
    const [isSubHeaderVisible, setIsSubHeaderVisible] = React.useState<boolean>(true);
    const lastScrollY = React.useRef<number>(0);

    React.useEffect(() => {
        let rafId = 0;

        const handleScroll = () => {
            const currentScrollY = window.scrollY;

            setIsScrolled(currentScrollY > 30);

            if (currentScrollY < 30) {
                setIsSubHeaderVisible(true);
            } else if (currentScrollY < lastScrollY.current) {
                // Scrolling UP -> Reveal sub-header!
                setIsSubHeaderVisible(true);
            } else if (currentScrollY > lastScrollY.current && currentScrollY > 60) {
                // Scrolling DOWN -> Hide sub-header!
                setIsSubHeaderVisible(false);
            }

            lastScrollY.current = currentScrollY;
        };

        // Throttle to animation frames so state updates stay in sync with
        // the compositor and the header animation remains buttery smooth.
        const onScroll = () => {
            if (rafId) return;
            rafId = requestAnimationFrame(() => {
                rafId = 0;
                handleScroll();
            });
        };

        handleScroll();
        window.addEventListener("scroll", onScroll, { passive: true });
        return () => {
            window.removeEventListener("scroll", onScroll);
            if (rafId) cancelAnimationFrame(rafId);
        };
    }, []);

    const isLoggedIn = authIsLoggedIn || propsIsLoggedIn;
    const displayName = user?.fullName || user?.username || propsUserName || "Thành viên LADUX";
    const userPhone = user?.phone || user?.email || "";
    const userAvatar = getAvatarUrl(user?.avatar || propsUserAvatar);

    const displayWishlistCount = wishlistProductIds.length > 0 ? wishlistProductIds.length : wishlistCount;
    const displayOrderCount = totalOrderElements || orders.length;

    React.useEffect(() => {
        if (isLoggedIn) {
            if (notifTab === "unread") {
                fetchUnreadNotifications(0, 10);
            } else if (notifTab === "read") {
                fetchReadNotifications(0, 10);
            } else {
                fetchNotifications(0, 10);
            }
        }
    }, [isLoggedIn, notifTab, fetchNotifications, fetchUnreadNotifications, fetchReadNotifications]);

    const handleLogout = async () => {
        try {
            await logout();
        } catch {
            // fallback
        }
        navigate(ROUTES.home);
    };

    const handleNavigate = (path: string) => {
        navigate(path);
    };

    return (
        <header
            className={`fixed top-0 left-0 right-0 z-50 border-b text-slate-800 backdrop-blur-xl backdrop-saturate-150 transition-[background-color,border-color,box-shadow] duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none ${isScrolled ? "header-scrolled" : ""
                }`}
        >
            <div className={`store-header-main container mx-auto px-5 sm:px-8 flex items-center justify-between gap-3 md:gap-6 relative transition-[height] duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none ${isScrolled ? "h-16" : "h-[76px]"}`}>
                {/* ── Logo ── */}
                <div className="z-10 flex items-center shrink-0">
                    <Link
                        to={ROUTES.home}
                        className="flex items-center gap-2.5 group shrink-0 cursor-pointer"
                    >
                        <BrandMark />
                    </Link>
                </div>

                {/* ── Dynamic Flex Search Bar (centered) ── */}
                <SmartSearchDropdown
                    searchQuery={searchQuery}
                    setSearchQuery={setSearchQuery}
                    mobileOpen={mobileSearchOpen}
                    onCloseMobile={closeMobileSearch}
                >
                    {/* Custom Category Dropdown */}
                    <div className="relative hidden lg:flex items-center  shrink-0 h-full" ref={catDropdownRef}>
                        <button
                            type="button"
                            onClick={() => setIsCatDropdownOpen(!isCatDropdownOpen)}
                            className="flex items-center gap-2 px-3 sm:px-4 h-full text-[10px] sm:text-[11px] font-sans font-bold normal-case tracking-normal text-slate-600 hover:text-sky-700 transition-colors cursor-pointer"
                        >
                            <span className="max-w-[130px] text-[12.5px] leading-none">
                                {selectedCategory && selectedCategory !== "All" ? selectedCategory : "Danh mục"}
                            </span>
                            <ChevronRight
                                className={`w-3.5 h-3.5 text-slate-500 transition-transform duration-300 shrink-0 translate-y-[0.5px] ${isCatDropdownOpen ? "-rotate-90 text-sky-700" : "rotate-90"
                                    }`}
                            />
                        </button>

                        {/* Custom Dropdown Popover */}
                        {isCatDropdownOpen && (
                            <div className="absolute right-0 top-[calc(100%+8px)] w-56 rounded-2xl border border-sky-100 bg-white backdrop-blur-2xl p-1.5 shadow-xl shadow-sky-100/60 z-50 animate-in fade-in slide-in-from-top-1 zoom-in-[0.98] duration-200 ease-out origin-top-right">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setSelectedCategory("All");
                                        setIsCatDropdownOpen(false);
                                        if (location.pathname !== ROUTES.products) {
                                            handleNavigate(ROUTES.products);
                                        }
                                    }}
                                    className={`w-full text-left px-3 py-2 rounded-xl text-xs font-sans font-bold transition-all flex items-center justify-between cursor-pointer ${!selectedCategory || selectedCategory === "All"
                                        ? "bg-sky-600 text-white shadow-md shadow-sky-700/20"
                                        : "text-slate-600 hover:bg-sky-50 hover:text-slate-800"
                                        }`}
                                >
                                    <span>Tất cả danh mục</span>
                                    {(!selectedCategory || selectedCategory === "All") && (
                                        <div className="h-1.5 w-1.5 rounded-full bg-white" />
                                    )}
                                </button>
                                <div className="h-px bg-sky-50 my-1" />
                                <div className="max-h-60 overflow-y-auto space-y-0.5 scrollbar-thin">
                                    {categories.map((category) => {
                                        const isSelected = selectedCategory === category.name;
                                        return (
                                            <button
                                                key={category.id}
                                                type="button"
                                                onClick={() => {
                                                    setSelectedCategory(category.name);
                                                    setIsCatDropdownOpen(false);
                                                    if (location.pathname !== ROUTES.products) {
                                                        handleNavigate(ROUTES.products);
                                                    }
                                                }}
                                                className={`w-full text-left px-3 py-2 rounded-xl text-xs font-sans font-bold transition-all flex items-center justify-between cursor-pointer ${isSelected
                                                    ? "bg-sky-600 text-white shadow-md shadow-sky-700/20"
                                                    : "text-slate-600 hover:bg-sky-50 hover:text-sky-700"
                                                    }`}
                                            >
                                                <span>{category.name}</span>
                                                {isSelected && <div className="h-1.5 w-1.5 rounded-full bg-white" />}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>

                </SmartSearchDropdown>

                {/* ── Right Actions ── */}
                <div className="z-10 flex items-center gap-1 sm:gap-2 shrink-0 md:ml-0 ml-auto">
                    <ThemeToggle />
                    <button type="button" aria-label="Mở tìm kiếm laptop" aria-expanded={mobileSearchOpen} onPointerDown={event => { event.preventDefault(); event.stopPropagation(); }} onClick={() => setMobileSearchOpen(value => !value)} className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-500 hover:bg-sky-50 hover:text-sky-700 md:hidden"><Search className="h-5 w-5" /></button>
                    {/* Cart Badge */}
                    <Link
                        to={ROUTES.cart}
                        className="relative p-2.5 text-sky-600 hover:text-sky-700 transition-all duration-200 rounded-xl hover:bg-sky-50 flex items-center justify-center group cursor-pointer"
                        aria-label="Giỏ hàng"
                    >
                        <ShoppingCart className="w-5 h-5 group-hover:scale-110 transition-transform duration-300 ease-[cubic-bezier(0.34,1.56,0.64,1)]" />
                        {cartCount > 0 && (
                            <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 bg-sky-600 text-white font-semibold text-[10px] rounded-full flex items-center justify-center shadow-sm">
                                {cartCount}
                            </span>
                        )}
                    </Link>

                    {/* Notification Badge & Popover */}
                    <div className="relative" ref={notifRef}>
                        <button
                            type="button"
                            onClick={(e) => {
                                e.stopPropagation();
                                if (!isLoggedIn) {
                                    navigate(ROUTES.login);
                                    return;
                                }
                                setIsUserMenuOpen(false);
                                setIsNotifOpen((prev) => !prev);
                            }}
                            className={`relative p-2.5 transition-all duration-200 rounded-xl flex items-center justify-center group cursor-pointer ${
                                isNotifOpen
                                    ? "bg-sky-100 text-sky-800 ring-2 ring-sky-200"
                                    : "text-sky-600 hover:text-sky-700 hover:bg-sky-50"
                            }`}
                            aria-label="Thông báo"
                            aria-expanded={isNotifOpen}
                        >
                            <Bell className="w-5 h-5 group-hover:scale-110 transition-transform duration-300 ease-[cubic-bezier(0.34,1.56,0.64,1)]" />
                            {unreadCount > 0 && (
                                <span className="absolute top-0.5 right-0.5 w-3.5 h-3.5 bg-amber-400 text-white font-bold text-[8px] rounded-full flex items-center justify-center shadow-sm">
                                    {unreadCount > 99 ? "99+" : unreadCount}
                                </span>
                            )}
                        </button>

                        {/* Notification Dropdown Menu */}
                        {isLoggedIn && isNotifOpen && (
                            <div
                                className="absolute right-0 top-full pt-2 z-50 animate-in fade-in slide-in-from-top-1 zoom-in-[0.98] duration-200 ease-out"
                                onClick={(e) => e.stopPropagation()}
                            >
                                <div className="w-[min(calc(100vw-24px),384px)] rounded-2xl border border-sky-100 bg-white/95 backdrop-blur-2xl p-4 shadow-2xl shadow-sky-100/70">
                                    <div className="flex items-center justify-between border-b border-sky-100 pb-3 mb-3">
                                        <div className="flex items-center gap-2">
                                            <Bell className="w-4 h-4 text-sky-700" />
                                            <span className="text-xs font-sans font-bold normal-case tracking-normal text-slate-800">Thông báo</span>
                                        </div>
                                        {unreadCount > 0 && (
                                            <button
                                                type="button"
                                                onClick={async (e) => {
                                                    e.stopPropagation();
                                                    await markAllAsRead();
                                                }}
                                                className="text-[10px] font-sans text-sky-700 hover:underline cursor-pointer"
                                            >
                                                Đánh dấu đã đọc
                                            </button>
                                        )}
                                    </div>

                                    {/* ── Tabs: Tất cả | Chưa đọc | Đã đọc ── */}
                                    <div className="grid grid-cols-3 gap-1 bg-slate-100/80 p-1 rounded-xl mb-3 text-[10px] font-sans font-bold">
                                        <button
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setNotifTab("all");
                                            }}
                                            className={`py-1 rounded-lg transition cursor-pointer ${
                                                notifTab === "all" ? "bg-sky-600 text-white shadow" : "text-slate-600 hover:text-slate-800"
                                            }`}
                                        >
                                            Tất cả
                                        </button>
                                        <button
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setNotifTab("unread");
                                            }}
                                            className={`py-1 rounded-lg transition cursor-pointer ${
                                                notifTab === "unread" ? "bg-sky-600 text-white shadow" : "text-slate-600 hover:text-slate-800"
                                            }`}
                                        >
                                            Chưa đọc
                                        </button>
                                        <button
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setNotifTab("read");
                                            }}
                                            className={`py-1 rounded-lg transition cursor-pointer ${
                                                notifTab === "read" ? "bg-sky-600 text-white shadow" : "text-slate-600 hover:text-slate-800"
                                            }`}
                                        >
                                            Đã đọc
                                        </button>
                                    </div>

                                    {/* ── Notification Items List ── */}
                                    {notifications.length === 0 ? (
                                        <div className="py-8 text-center text-xs font-sans text-slate-500">
                                            Không có thông báo nào ({notifTab === "all" ? "Tất cả" : notifTab === "unread" ? "Chưa đọc" : "Đã đọc"})
                                        </div>
                                    ) : (
                                        <div className="space-y-2 max-h-72 overflow-y-auto pr-1 scrollbar-thin">
                                            {notifications.map((n) => (
                                                <div
                                                    key={n.id}
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        if (!n.isRead) {
                                                            markAsRead(n.id);
                                                        }
                                                    }}
                                                    className={`group/item relative p-3 rounded-xl border text-xs transition cursor-pointer flex items-start justify-between gap-3 ${
                                                        n.isRead
                                                            ? "bg-slate-50/60 border-slate-200 text-slate-500"
                                                            : "bg-sky-50/50 border-sky-200 text-slate-800 font-medium hover:border-sky-300"
                                                    }`}
                                                >
                                                    <div className="flex-1 min-w-0">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            {!n.isRead && <span className="w-2 h-2 rounded-full bg-sky-600 shrink-0" />}
                                                            <span className="font-bold text-slate-800 truncate">{n.title}</span>
                                                        </div>
                                                        <p className="text-[11px] text-slate-600 line-clamp-2 leading-relaxed">{n.message}</p>
                                                    </div>

                                                    {/* Dấu X ở bên phải để xóa thông báo cụ thể */}
                                                    <button
                                                        type="button"
                                                        title="Xóa thông báo"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            deleteNotification(n.id);
                                                        }}
                                                        className="p-1 text-slate-400 hover:text-rose-700 hover:bg-red-50 rounded-lg transition shrink-0 cursor-pointer"
                                                    >
                                                        <X className="w-3.5 h-3.5" />
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {/* ── Bottom Section: Label xóa tất cả ── */}
                                    {notifications.length > 0 && (
                                        <div className="mt-3 pt-2.5 border-t border-sky-100 flex items-center justify-between text-[11px] font-sans">
                                            <span className="text-slate-500">Tổng: {notifications.length}</span>
                                            <button
                                                type="button"
                                                onClick={async (e) => {
                                                    e.stopPropagation();
                                                    if (window.confirm("Bạn có chắc muốn xóa tất cả thông báo không?")) {
                                                        await deleteAllNotifications();
                                                    }
                                                }}
                                                className="text-rose-700 hover:text-red-700 font-bold hover:underline flex items-center gap-1 cursor-pointer"
                                            >
                                                <Trash2 className="w-3 h-3" />
                                                Xóa tất cả thông báo
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Wishlist Badge */}
                    <Link
                        to={ROUTES.wishlist}
                        className="header-wishlist relative p-2.5 text-sky-600 hover:text-sky-700 transition-all duration-200 rounded-xl hover:bg-sky-50 flex items-center justify-center group cursor-pointer"
                        aria-label="Danh sách yêu thích"
                    >
                        <Heart className="w-5 h-5 text-sky-600 fill-sky-100 group-hover:scale-110 transition-transform duration-300 ease-[cubic-bezier(0.34,1.56,0.64,1)]" />
                        {displayWishlistCount > 0 && (
                            <span className="absolute top-1 right-1 w-4 h-4 bg-sky-600 text-white font-bold text-[9px] rounded-full flex items-center justify-center">
                                {displayWishlistCount}
                            </span>
                        )}
                    </Link>

                    {/* ── User Account CTA & Popover Dropdown ── */}
                    <div className="relative group/user">
                        {isLoggedIn ? (
                            /* Logged in CTA Button */
                            <button
                                onClick={() => navigate(ROUTES.account)}
                                className="flex items-center gap-2 rounded-full border border-sky-200 bg-sky-50 py-1 pl-1 pr-1 lg:pr-3 text-slate-700 transition hover:border-sky-700 hover:bg-sky-50"
                            >
                                {userAvatar ? (
                                    <img
                                        src={userAvatar}
                                        alt="Avatar"
                                        className="h-7 w-7 rounded-full object-cover border-2 border-sky-700"
                                    />
                                ) : (
                                    <span className="flex h-7 w-7 items-center justify-center rounded-full border-2 border-sky-700 bg-white">
                                        <User className="h-3.5 w-3.5 text-sky-700" />
                                    </span>
                                )}
                                <span className="text-xs font-bold truncate max-w-[110px] hidden lg:inline">
                                    {displayName}
                                </span>
                            </button>
                        ) : (
                            /* Unauthenticated CTA Button */
                            <button
                                onClick={() => navigate(ROUTES.login)}
                                className="w-9 h-9 rounded-full border border-sky-200 bg-sky-50 flex items-center justify-center text-sky-700 hover:border-sky-700 hover:bg-sky-50 transition-all cursor-pointer"
                                aria-label="Đăng nhập"
                            >
                                <User className="w-4.5 h-4.5 text-sky-700" />
                            </button>
                        )}

                        {/* Hover Dropdown Popover */}
                        <div className="absolute right-0 top-full pt-2 opacity-0 invisible translate-y-1.5 scale-[0.98] origin-top-right group-hover/user:opacity-100 group-focus-within/user:opacity-100 group-hover/user:visible group-focus-within/user:visible group-hover/user:translate-y-0 group-hover/user:scale-100 group-focus-within/user:translate-y-0 group-focus-within/user:scale-100 transition-all duration-300 ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none z-50 pointer-events-none group-hover/user:pointer-events-auto group-focus-within/user:pointer-events-auto">
                            <div className={`bg-white border border-sky-100 rounded-2xl shadow-xl shadow-sky-100/50 p-4 text-slate-800 font-sans ${isLoggedIn ? "w-72" : "w-64"}`}>
                                {isLoggedIn ? (
                                    <>
                                        {/* User Header Info */}
                                        <div className="px-1 py-0.5">
                                            <div className="font-semibold text-sm text-slate-800 truncate">{displayName}</div>
                                            <div className="text-[11px] font-sans text-sky-700 font-semibold flex items-center gap-1 mt-0.5">
                                                <span>{user?.level || "BROWSER"} MEMBER</span>
                                                <span>·</span>
                                                <span>{userPhone}</span>
                                            </div>
                                        </div>

                                        <div className="my-3 border-b border-sky-100" />

                                        {/* Nav items */}
                                        <div className="space-y-1">
                                            <Link
                                                to={ROUTES.account}
                                                className="flex items-center justify-between px-2.5 py-2 rounded-xl text-xs font-medium text-slate-600 hover:text-slate-800 hover:bg-sky-50 transition"
                                            >
                                                <div className="flex items-center gap-3">
                                                    <User className="w-4 h-4 text-sky-700" />
                                                    <span>Xem thông tin tài khoản</span>
                                                </div>
                                            </Link>

                                            <Link
                                                to={ROUTES.orders}
                                                className="flex items-center justify-between px-2.5 py-2 rounded-xl text-xs font-medium text-slate-600 hover:text-slate-800 hover:bg-sky-50 transition"
                                            >
                                                <div className="flex items-center gap-3">
                                                    <Package className="w-4 h-4 text-sky-700" />
                                                    <span>Quản lý đơn hàng</span>
                                                </div>
                                                <span className="px-2 py-0.5 text-[10px] font-bold bg-sky-50 text-sky-700 rounded-md">
                                                    {displayOrderCount}
                                                </span>
                                            </Link>

                                            <Link
                                                to={ROUTES.addresses}
                                                className="flex items-center justify-between px-2.5 py-2 rounded-xl text-xs font-medium text-slate-600 hover:text-slate-800 hover:bg-sky-50 transition"
                                            >
                                                <div className="flex items-center gap-3">
                                                    <MapPin className="w-4 h-4 text-sky-700" />
                                                    <span>Sổ địa chỉ giao hàng</span>
                                                </div>
                                            </Link>

                                            <Link
                                                to={ROUTES.wishlist}
                                                className="flex items-center justify-between px-2.5 py-2 rounded-xl text-xs font-medium text-slate-600 hover:text-slate-800 hover:bg-sky-50 transition"
                                            >
                                                <div className="flex items-center gap-3">
                                                    <Heart className="w-4 h-4 text-rose-700 fill-red-500/20" />
                                                    <span>Danh sách yêu thích</span>
                                                </div>
                                                <span className="px-2 py-0.5 text-[10px] font-bold bg-red-500/20 text-rose-700 rounded-md">
                                                    {displayWishlistCount}
                                                </span>
                                            </Link>
                                        </div>

                                        <div className="my-3 border-b border-sky-100" />

                                        {/* Logout action */}
                                        <button
                                            onClick={handleLogout}
                                            className="w-full flex items-center gap-3 px-2.5 py-2 rounded-xl text-xs font-bold text-rose-700 hover:bg-red-500/10 transition text-left"
                                        >
                                            <LogOut className="w-4 h-4 text-rose-700" />
                                            <span>Đăng xuất</span>
                                        </button>
                                    </>
                                ) : (
                                    <>
                                        {/* Unauthenticated Popover */}
                                        <button
                                            onClick={() => navigate(ROUTES.login)}
                                            className="w-full bg-sky-600 hover:bg-sky-600 text-white font-semibold py-2.5 px-4 rounded-full flex items-center justify-center gap-2 text-xs tracking-normal transition shadow-lg shadow-sky-700/20 normal-case"
                                        >
                                            <Lock className="w-4 h-4 text-white stroke-[2.5]" />
                                            <span>Đăng nhập</span>
                                        </button>
                                        <Link
                                            to={ROUTES.register}
                                            className="w-full text-center text-xs font-semibold text-slate-600 hover:text-sky-700 transition-colors mt-3 block py-1"
                                        >
                                            Tạo tài khoản mới
                                        </Link>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* ── Sub-header (Header phụ) ── */}
            <div
                className={`grid backdrop-blur-md transition-[grid-template-rows,opacity] duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none ${!isSubHeaderVisible
                    ? "[grid-template-rows:0fr] opacity-0 pointer-events-none"
                    : "[grid-template-rows:1fr] opacity-100"
                    }`}
            >
                <div className="min-h-0 overflow-hidden">
                <div className="container mx-auto px-5 sm:px-8 py-3 border-t border-sky-100/70 flex items-center justify-between gap-4 text-sm text-slate-600 font-medium">
                    {/* Left side links */}
                    <nav aria-label="Điều hướng chính" className="flex items-center gap-5 sm:gap-8">
                        <NavLink
                            to={ROUTES.home}
                            end
                            className={({ isActive }) =>
                                `transition-all duration-300 ease-out pb-1 hover:text-sky-700 border-b-2 text-xs sm:text-sm ${isActive
                                    ? "text-sky-700 border-sky-700 font-semibold"
                                    : "text-slate-600 border-transparent hover:border-sky-700/50"
                                }`
                            }
                        >
                            Trang chủ
                        </NavLink>
                        <NavLink
                            to={ROUTES.products}
                            className={({ isActive }) =>
                                `transition-all duration-300 ease-out pb-1 hover:text-sky-700 border-b-2 text-xs sm:text-sm ${isActive
                                    ? "text-sky-700 border-sky-700 font-semibold"
                                    : "text-slate-600 border-transparent hover:border-sky-700/50"
                                }`
                            }
                        >
                            Sản phẩm
                        </NavLink>
                        <NavLink
                            to={ROUTES.about}
                            className={({ isActive }) =>
                                `transition-all duration-300 ease-out pb-1 hover:text-sky-700 border-b-2 text-xs sm:text-sm ${isActive
                                    ? "text-sky-700 border-sky-700 font-semibold"
                                    : "text-slate-600 border-transparent hover:border-sky-700/50"
                                }`
                            }
                        >
                            Về chúng tôi
                        </NavLink>
                        <NavLink
                            to={ROUTES.contact}
                            className={({ isActive }) =>
                                `transition-all duration-300 ease-out pb-1 hover:text-sky-700 border-b-2 text-xs sm:text-sm ${isActive
                                    ? "text-sky-700 border-sky-700 font-semibold"
                                    : "text-slate-600 border-transparent hover:border-sky-700/50"
                                }`
                            }
                        >
                            Liên hệ
                        </NavLink>
                    </nav>

                    {/* Right side info */}
                    <div className="hidden lg:flex items-center gap-6 text-xs">
                        {STOREFRONT_CONTACT.phone && <div className="flex items-center gap-2.5 group cursor-pointer">
                            <Phone className="w-[18px] h-[18px] text-sky-700 group-hover:scale-110 transition-transform duration-300 ease-[cubic-bezier(0.34,1.56,0.64,1)]" />
                            <span className="text-slate-500 group-hover:text-slate-800 transition-colors">
                                Hotline: <strong className="text-slate-800 font-medium">{STOREFRONT_CONTACT.phone}</strong>
                            </span>
                        </div>}
                        {STOREFRONT_CONTACT.phone && <span className="text-slate-800 hidden sm:inline">|</span>}
                        <div className="flex items-center gap-2.5 text-sky-700 font-semibold">
                            <span className="w-2 h-2 rounded-full bg-sky-600 " />
                            <span>Giao hỏa tốc 2H</span>
                        </div>
                    </div>
                </div>
                </div>
            </div>
        </header>
    );
}
