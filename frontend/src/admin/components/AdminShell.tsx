import { useEffect, useMemo, useState } from "react";
import {
  ArrowUpRight, Bell, Boxes, ChartNoAxesCombined, ChevronLeft, CircleDollarSign, Database,
  ClipboardList, ContactRound, CreditCard, History, LogOut, Menu, Package,
  Palette, Search, ShoppingBag, Star, Tags, TicketPercent, Truck, UserRound, UsersRound, Warehouse,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import BrandMark from "../../components/common/BrandMark";
import ThemeToggle from "../../components/common/ThemeToggle";
import { Dialog, DialogContent, DialogTitle, DialogDescription } from "../../app/components/ui/dialog";
import "../../styles/admin-v2.css";
import { cn } from "../../app/components/ui/utils";
import { env } from "../../config/env";
import { useAdminAuth } from "../auth/AdminAuthProvider";
import { AdminButton } from "./AdminUI";
import { resolveImageUrl } from "../utils";
import { AdminNotificationDropdown } from "./AdminNotificationDropdown";

interface NavigationGroup {
  label: string;
  items: Array<{ label: string; path: string; icon: LucideIcon }>;
}

const navigation: NavigationGroup[] = [
  { label: "Không gian làm việc", items: [{ label: "Tổng quan", path: "/admin/dashboard", icon: ChartNoAxesCombined }] },
  { label: "Bán hàng", items: [
    { label: "Đơn hàng", path: "/admin/orders", icon: ShoppingBag },
    { label: "Thanh toán", path: "/admin/payments", icon: CreditCard },
    { label: "Lịch sử đơn hàng", path: "/admin/order-histories", icon: History },
    { label: "Dòng sản phẩm", path: "/admin/order-items", icon: ClipboardList },
  ] },
  { label: "Sản phẩm", items: [
    { label: "Sản phẩm", path: "/admin/products", icon: Package },
    { label: "Thương hiệu", path: "/admin/brands", icon: Tags },
    { label: "Danh mục", path: "/admin/categories", icon: Boxes },
    { label: "Màu sắc", path: "/admin/colors", icon: Palette },
    { label: "Mã giảm giá", path: "/admin/coupons", icon: TicketPercent },
  ] },
  { label: "Khách hàng", items: [
    { label: "Khách hàng", path: "/admin/customers", icon: ContactRound },
    { label: "Người dùng", path: "/admin/users", icon: UsersRound },
    { label: "Địa chỉ", path: "/admin/user-addresses", icon: UserRound },
    { label: "Đánh giá", path: "/admin/reviews", icon: Star },
  ] },
  { label: "Kho & cung ứng", items: [
    { label: "Nhà cung cấp", path: "/admin/suppliers", icon: Truck },
    { label: "Liên kết nhà cung cấp", path: "/admin/product-suppliers", icon: Boxes },
    { label: "Đơn nhập hàng", path: "/admin/purchase-orders", icon: CircleDollarSign },
    { label: "Biến động kho", path: "/admin/stock-movements", icon: Warehouse },
  ] },
  { label: "Hệ thống", items: [
    { label: "Thông báo", path: "/admin/notifications", icon: Bell },
    { label: "Ladux SmartFit", path: "/admin/chatbot-index", icon: Database },
  ] },
];
const allNavigation = navigation.flatMap(group => group.items);

function SidebarContent({ collapsed, onNavigate, onToggleCollapse }: { collapsed: boolean; onNavigate?: () => void; onToggleCollapse?: () => void }) {
  return <div className="admin-sidebar-content">
    <div className={cn("admin-sidebar-brand", collapsed && "is-collapsed")}>
      <Link to="/admin/dashboard" onClick={onNavigate}><BrandMark compact={collapsed} /></Link>
      {!collapsed && <span className="admin-workspace-label">WORKSPACE</span>}
    </div>
    <nav className="admin-navigation custom-sidebar-scrollbar" aria-label="Điều hướng quản trị">
      {navigation.map(group => <div className="admin-nav-group" key={group.label}>
        {!collapsed && <p className="admin-nav-label">{group.label}</p>}
        {group.items.map(({ path, label, icon: Icon }) => <NavLink key={path} to={path} onClick={onNavigate} title={collapsed ? label : undefined} className={({ isActive }) => cn("admin-nav-link", isActive && "is-active", collapsed && "is-collapsed")}>
          <Icon size={17} strokeWidth={1.6} />{!collapsed && <span>{label}</span>}
        </NavLink>)}
      </div>)}
    </nav>
    <div className="admin-sidebar-bottom">
      {!collapsed && <Link to="/" className="admin-store-link"><span className="admin-online-dot" />Ladux Store<ArrowUpRight size={14} /></Link>}
      {onToggleCollapse && <button className="admin-collapse-button" aria-label={collapsed ? "Mở rộng sidebar" : "Thu gọn sidebar"} onClick={onToggleCollapse}><ChevronLeft size={16} className={collapsed ? "rotate-180" : ""} />{!collapsed && <span>Thu gọn menu</span>}</button>}
    </div>
  </div>;
}

export default function AdminShell() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [search, setSearch] = useState("");
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAdminAuth();
  const currentLabel = useMemo(() => allNavigation.find(entry => location.pathname === entry.path || location.pathname.startsWith(entry.path + "/"))?.label ?? "Quản trị", [location.pathname]);
  const searchTerm = search.trim().toLocaleLowerCase("vi");
  const matches = searchTerm ? allNavigation.filter(item => item.label.toLocaleLowerCase("vi").includes(searchTerm)).sort((left, right) => Number(right.label.toLocaleLowerCase("vi") === searchTerm) - Number(left.label.toLocaleLowerCase("vi") === searchTerm)) : [];

  useEffect(() => { setMobileOpen(false); setSearch(""); }, [location.pathname]);
  const handleLogout = async () => { await logout(); navigate("/admin/login", { replace: true }); };
  const avatar = resolveImageUrl(user?.avatar, env.backendOrigin);
  const initial = (user?.fullName || user?.username || "A").trim().charAt(0).toUpperCase();

  return <div className={cn("admin-workspace", collapsed && "sidebar-collapsed")}>
    <aside className="admin-sidebar"><SidebarContent collapsed={collapsed} onToggleCollapse={() => setCollapsed(value => !value)} /></aside>
    <Dialog open={mobileOpen} onOpenChange={setMobileOpen}>
      <DialogContent className="admin-mobile-drawer">
        <DialogTitle className="sr-only">Menu quản trị</DialogTitle>
        <DialogDescription className="sr-only">Điều hướng các chức năng quản trị Ladux.</DialogDescription>
        <SidebarContent collapsed={false} onNavigate={() => setMobileOpen(false)} />
      </DialogContent>
    </Dialog>
    <div className="admin-workspace-body">
      <header className="admin-topbar">
        <button aria-label="Mở menu" onClick={() => setMobileOpen(true)} className="admin-mobile-menu"><Menu size={20} /></button>
        <div className="admin-breadcrumb"><span>Workspace</span><span>/</span><strong>{currentLabel}</strong></div>
        <form className="admin-nav-search" role="search" onSubmit={event => { event.preventDefault(); if (matches[0]) navigate(matches[0].path); }}>
          <Search size={15} /><input aria-label="Tìm trang quản trị" placeholder="Tìm trong workspace..." value={search} onChange={event => setSearch(event.target.value)} onKeyDown={event => { if (event.key === "Escape") setSearch(""); }} />
          {search.trim() && <div className="admin-search-results">{matches.length ? matches.map(item => <Link key={item.path} to={item.path}>{item.label}<ArrowUpRight size={13} /></Link>) : <p>Không tìm thấy trang phù hợp.</p>}</div>}
        </form>
        <div className="admin-topbar-actions">
          <ThemeToggle /><AdminNotificationDropdown />
          <span className="admin-topbar-divider" />
          <div className="admin-profile-name"><strong>{user?.fullName || user?.username}</strong><span>Quản trị viên</span></div>
          <div className="admin-profile-avatar">{avatar ? <img src={avatar} alt="Ảnh đại diện quản trị viên" onError={event => { event.currentTarget.style.display = "none"; }} /> : null}<span>{initial}</span></div>
          <AdminButton tone="ghost" size="icon" aria-label="Đăng xuất" onClick={handleLogout}><LogOut size={16} /></AdminButton>
        </div>
      </header>
      <main className="admin-main"><Outlet /></main>
      <footer className="admin-footer"><span>© {new Date().getFullYear()} Ladux Workspace</span><Link to="/">Về cửa hàng <ArrowUpRight size={12} /></Link></footer>
    </div>
  </div>;
}
