import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, ArrowRight, ArrowUpRight, Boxes, CalendarDays, CreditCard, PackageCheck, ShoppingBag, Truck, UsersRound } from "lucide-react";
import { Link } from "react-router-dom";
import { adminApi } from "../api/adminApi";
import { AdminTable, Panel, StatusBadge, type AdminColumn } from "../components/AdminUI";
import { adminQueryKeys } from "../queryKeys";
import type { OrderResponse, ProductResponse, PurchaseOrderResponse } from "../types";
import { formatBackendDateTime, formatCurrency, getApiErrorMessage } from "../utils";
import { useAdminAuth } from "../auth/AdminAuthProvider";
import operationsArt from "../../assets/ladux/operations.webp";

type DashboardResult = {
  counts: Record<string, number | null>;
  recentOrders: OrderResponse[];
  recentPurchaseOrders: PurchaseOrderResponse[];
  lowStockProducts: ProductResponse[];
};

async function loadDashboard(): Promise<DashboardResult> {
  const requests = await Promise.allSettled([
    adminApi.orders.list({ page: 0, size: 5, sort: "createdAt,desc" }),
    adminApi.orders.byStatus("PENDING", { page: 0, size: 1 }),
    adminApi.orders.byStatus("CONFIRMED", { page: 0, size: 1 }),
    adminApi.orders.byStatus("SHIPPED", { page: 0, size: 1 }),
    adminApi.customers.list({ page: 0, size: 1 }),
    adminApi.users.list({ page: 0, size: 1 }),
    adminApi.products.list({ page: 0, size: 12, sort: "createdAt,desc" }),
    adminApi.suppliers.list({ page: 0, size: 1 }),
    adminApi.purchaseOrders.list({ page: 0, size: 5, sort: "createdAt,desc" }),
    adminApi.payments.byStatus("PENDING", { page: 0, size: 1 }),
  ]);

  const total = (index: number) => requests[index].status === "fulfilled" ? requests[index].value.totalElements : null;
  const content = <T,>(index: number): T[] => requests[index].status === "fulfilled" ? requests[index].value.content as T[] : [];
  const products = content<ProductResponse>(6);

  return {
    counts: {
      orders: total(0), pendingOrders: total(1), confirmedOrders: total(2), shippedOrders: total(3),
      customers: total(4), users: total(5), products: total(6), suppliers: total(7),
      purchaseOrders: total(8), pendingPayments: total(9),
    },
    recentOrders: content<OrderResponse>(0),
    recentPurchaseOrders: content<PurchaseOrderResponse>(8),
    lowStockProducts: products.filter((product) => (product.variants ?? []).some((variant) => variant.stockQuantity <= 5)),
  };
}

const orderColumns: AdminColumn<OrderResponse>[] = [
  { key: "id", header: "Đơn hàng", render: (row) => <div><Link to={`/admin/orders/${row.id}`} className="font-extrabold text-slate-900 hover:text-indigo-600">#{row.id}</Link><p className="mt-1 text-xs text-slate-400">User {row.userId}</p></div> },
  { key: "date", header: "Thời gian", render: (row) => formatBackendDateTime(row.createdAt) },
  { key: "amount", header: "Tổng tiền", render: (row) => <span className="font-bold text-slate-900">{formatCurrency(row.finalAmount)}</span> },
  { key: "status", header: "Trạng thái", render: (row) => <StatusBadge value={row.status} /> },
];

export default function DashboardPage() {
  const { user } = useAdminAuth();
  const query = useQuery({ queryKey: adminQueryKeys.dashboard, queryFn: loadDashboard, staleTime: 60_000 });
  const counts = query.data?.counts ?? {};
  const name = (user?.fullName || user?.username || "bạn").trim().split(/\s+/).pop();
  const hasMix = [counts.orders, counts.pendingOrders, counts.confirmedOrders, counts.shippedOrders].every(value => typeof value === "number");
  const orderMix = [
    { label: "Chờ xác nhận", value: counts.pendingOrders ?? 0, color: "#ed6c27" },
    { label: "Đã xác nhận", value: counts.confirmedOrders ?? 0, color: "#e6b07d" },
    { label: "Đang giao", value: counts.shippedOrders ?? 0, color: "#889e8b" },
    { label: "Trạng thái khác", value: Math.max(0, (counts.orders ?? 0) - (counts.pendingOrders ?? 0) - (counts.confirmedOrders ?? 0) - (counts.shippedOrders ?? 0)), color: "#c9c4ba" },
  ];
  let angle = 0;
  const chart = orderMix.map(item => { const start = angle; angle += (item.value / Math.max(counts.orders ?? 1, 1)) * 360; return `${item.color} ${start}deg ${angle}deg`; }).join(", ");
  const cards = [
    { label: "Tổng đơn hàng", value: counts.orders, helper: `${counts.pendingOrders ?? "—"} đang chờ xác nhận`, icon: ShoppingBag, iconClass: "bg-indigo-50 text-indigo-600", path: "/admin/orders" },
    { label: "Sản phẩm", value: counts.products, helper: "Catalog hiện có", icon: Boxes, iconClass: "bg-emerald-50 text-emerald-600", path: "/admin/products" },
    { label: "Khách hàng", value: counts.customers, helper: `${counts.users ?? "—"} tài khoản hệ thống`, icon: UsersRound, iconClass: "bg-violet-50 text-violet-600", path: "/admin/customers" },
    { label: "Thanh toán chờ", value: counts.pendingPayments, helper: "Cần kiểm tra", icon: CreditCard, iconClass: "bg-amber-50 text-amber-600", path: "/admin/payments?status=PENDING" },
  ] as const;

  return (
    <>
      <div className="dashboard-heading"><div><h1>Tổng quan vận hành</h1><p>Một góc nhìn rõ ràng, cho những quyết định tốt hơn.</p></div><span className="dashboard-date"><CalendarDays size={14} />{new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "long", year: "numeric" }).format(new Date())}</span></div>

      <section className="dashboard-welcome"><div className="dashboard-welcome-copy"><p className="section-eyebrow">A LITTLE CLARITY, EVERY DAY</p><h2>Chào {name}, ngày mới sẵn sàng.</h2><p>Cùng chăm chút từng đơn hàng và mang công nghệ phù hợp đến nhiều người hơn.</p></div><img src={operationsArt} alt="Không gian vận hành Ladux với laptop và những khối hàng màu kem, cam" width={1254} height={1254} /></section>

      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {cards.map((card) => {
          const Icon = card.icon;
          return <Link key={card.label} to={card.path} className="dashboard-stat">
            <div className="dashboard-stat-top"><span>{card.label}</span><span className="dashboard-stat-icon"><Icon size={16} strokeWidth={1.6} /></span></div>
            <p className="dashboard-stat-value">{query.isLoading ? <span className="inline-block h-9 w-20 animate-pulse rounded bg-slate-100" /> : card.value?.toLocaleString("vi-VN") ?? "—"}</p>
            <p className="dashboard-stat-helper">{card.helper}<ArrowUpRight size={13} /></p>
          </Link>;
        })}
      </div>

      {query.isError && <div role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-medium text-rose-700">{getApiErrorMessage(query.error)}</div>}

      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <Panel><div className="dashboard-panel-heading"><div><h2>Nhịp bán hàng</h2><p>Phân bổ trạng thái trên toàn bộ đơn hàng</p></div><Link to="/admin/orders">Chi tiết<ArrowUpRight size={12} /></Link></div>
          {hasMix ? <div className="dashboard-mix-layout"><div className="dashboard-donut" style={{ background: counts.orders ? `conic-gradient(${chart})` : "var(--line)" }} role="img" aria-label={orderMix.map(item => `${item.label}: ${item.value}`).join(", ")}><div className="dashboard-donut-label"><strong>{counts.orders?.toLocaleString("vi-VN")}</strong><span>Tổng đơn hàng</span></div></div><div className="dashboard-mix-legend">{orderMix.map(item => <div key={item.label}><i style={{ background: item.color }} /><span>{item.label}</span><strong>{item.value.toLocaleString("vi-VN")}</strong></div>)}</div></div> : <p className="p-10 text-center text-xs text-slate-500">{query.isLoading ? "Đang tải phân bổ đơn hàng..." : "Chưa thể tải đầy đủ dữ liệu đơn hàng."}</p>}
        </Panel>
        <Panel><div className="dashboard-panel-heading"><div><h2>Việc cần quan tâm</h2><p>Đi thẳng đến những hoạt động đang chờ bạn</p></div><span className="admin-online-dot" /></div><div className="space-y-1 p-4">{[{ label: "Đơn hàng chờ xác nhận", value: counts.pendingOrders, to: "/admin/orders", icon: ShoppingBag }, { label: "Thanh toán cần kiểm tra", value: counts.pendingPayments, to: "/admin/payments", icon: CreditCard }, { label: "Quản lý nhập kho", value: counts.purchaseOrders, to: "/admin/purchase-orders", icon: Truck }].map(({ label, value, to, icon: Icon }) => <Link key={to} to={to} className="flex items-center gap-3 rounded-lg px-3 py-3 text-xs transition hover:bg-slate-50"><span className="rounded-lg bg-sky-50 p-2 text-sky-700"><Icon size={15} /></span><span className="text-slate-600">{label}</span><strong className="ml-auto font-semibold text-slate-900">{value?.toLocaleString("vi-VN") ?? "—"}</strong><ArrowRight size={13} className="ml-2 text-slate-400" /></Link>)}</div></Panel>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.45fr_0.85fr]">
        <Panel>
          <div className="dashboard-panel-heading"><div><h2>Đơn hàng gần đây</h2><p>Những đơn hàng mới nhất tại Ladux</p></div><Link to="/admin/orders">Xem tất cả<ArrowUpRight size={12} /></Link></div>
          <AdminTable rows={query.data?.recentOrders ?? []} columns={orderColumns} isLoading={query.isLoading} />
        </Panel>
        <div className="space-y-6">
          <Panel className="p-5">
            <div className="flex items-center justify-between"><div><h2 className="font-extrabold text-slate-900">Đơn nhập hàng</h2><p className="mt-1 text-xs text-slate-400">Theo dõi luồng cung ứng</p></div><span className="rounded-xl bg-indigo-50 p-2.5 text-indigo-600"><Truck className="h-5 w-5" /></span></div>
            <div className="mt-5 space-y-3">
              {query.isLoading && Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-14 animate-pulse rounded-xl bg-slate-100" />)}
              {query.data?.recentPurchaseOrders.slice(0, 4).map((order) => <Link key={order.id} to={`/admin/purchase-orders/${order.id}`} className="flex items-center justify-between rounded-xl border border-slate-100 p-3 hover:border-indigo-200 hover:bg-indigo-50/40"><div><p className="text-sm font-bold text-slate-900">PO #{order.id}</p><p className="mt-1 text-xs text-slate-400">{order.supplierName || `NCC #${order.supplierId}`}</p></div><div className="text-right"><p className="text-xs font-bold text-slate-700">{formatCurrency(order.totalAmount)}</p><div className="mt-1"><StatusBadge value={order.status} /></div></div></Link>)}
              {!query.isLoading && !query.data?.recentPurchaseOrders.length && <p className="py-8 text-center text-sm text-slate-400">Chưa có đơn nhập hàng</p>}
            </div>
          </Panel>
          <Panel className="p-5">
            <div className="flex items-center gap-3"><span className="rounded-xl bg-amber-50 p-2.5 text-amber-600"><AlertTriangle className="h-5 w-5" /></span><div><h2 className="font-extrabold text-slate-900">Cảnh báo kho thấp</h2><p className="text-xs text-slate-400">Chỉ trên các sản phẩm đã tải</p></div></div>
            <div className="mt-4 space-y-2">{query.data?.lowStockProducts.slice(0, 4).map((product) => <Link key={product.id} to={`/admin/products/${product.id}`} className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2.5 text-sm hover:bg-amber-50"><span className="truncate font-semibold text-slate-700">{product.name}</span><span className="ml-3 text-xs font-bold text-amber-700">{Math.min(...product.variants.map((variant) => variant.stockQuantity))} còn lại</span></Link>)}{!query.isLoading && !query.data?.lowStockProducts.length && <div className="flex items-center gap-2 rounded-xl bg-emerald-50 p-3 text-xs font-bold text-emerald-700"><PackageCheck className="h-4 w-4" />Chưa thấy cảnh báo trên dữ liệu đã tải</div>}</div>
          </Panel>
        </div>
      </div>
    </>
  );
}
