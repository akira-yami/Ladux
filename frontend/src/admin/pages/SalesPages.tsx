import { useEffect, useMemo, useState, type FormEvent } from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Check, Edit3, LoaderCircle, RotateCcw, Search, X } from "lucide-react";
import { Link, useLocation, useParams, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../../app/components/ui/dialog";
import { env } from "../../config/env";
import { adminApi } from "../api/adminApi";
import { AdminButton, AdminTable, fieldClassName, LoadingScreen, PageHeader, PaginationBar, Panel, StatusBadge, type AdminColumn } from "../components/AdminUI";
import { adminQueryKeys } from "../queryKeys";
import type { OrderHistoryResponse, OrderItemResponse, OrderResponse, OrderStatus, PageResponse, PaymentProvider, PaymentResponse, PaymentStatus } from "../types";
import { formatBackendDateTime, formatCurrency, getApiErrorMessage, orderTransitions } from "../utils";

const orderStatuses: OrderStatus[] = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "RETURN_REQUESTED", "RETURNED", "REFUNDED", "CANCELLED"];

function oneItemPage<T>(item: T | null, page: number, size: number): PageResponse<T> {
  const content = item ? [item] : [];
  return { content, totalElements: content.length, totalPages: content.length ? 1 : 0, size, number: page, numberOfElements: content.length, first: true, last: true, empty: content.length === 0 };
}

function ReturnReviewDialog({
  order,
  open,
  onOpenChange,
}: {
  order: OrderResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const acceptMutation = useMutation({
    mutationFn: () => adminApi.orders.updateStatus(order!.id, { status: "RETURNED" }),
    onSuccess: () => {
      toast.success(`Đã chấp nhận yêu cầu trả hàng đơn #${order?.id}. Hàng đã được nhập lại vào kho.`);
      queryClient.invalidateQueries({ queryKey: ["admin", "orders"] });
      queryClient.invalidateQueries({ queryKey: adminQueryKeys.dashboard });
      onOpenChange(false);
    },
    onError: (mutationError) => setError(getApiErrorMessage(mutationError)),
  });

  const rejectMutation = useMutation({
    mutationFn: () => adminApi.orders.updateStatus(order!.id, { status: "DELIVERED" }),
    onSuccess: () => {
      toast.success(`Đã từ chối yêu cầu trả hàng đơn #${order?.id}. Đơn chuyển về DELIVERED.`);
      queryClient.invalidateQueries({ queryKey: ["admin", "orders"] });
      queryClient.invalidateQueries({ queryKey: adminQueryKeys.dashboard });
      onOpenChange(false);
    },
    onError: (mutationError) => setError(getApiErrorMessage(mutationError)),
  });

  if (!order) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="border-slate-200 bg-white text-slate-950 sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2 text-amber-600">
            <RotateCcw className="h-5 w-5" />
            <DialogTitle>Xử lý yêu cầu trả hàng #{order.id}</DialogTitle>
          </div>
          <DialogDescription className="text-slate-500">
            Khách hàng (User #{order.userId}) đã gửi yêu cầu trả hàng cho đơn này.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {error && <div role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}

          <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 space-y-2">
            <div className="flex justify-between text-xs">
              <span className="text-slate-500">Tổng giá trị đơn:</span>
              <span className="font-bold text-slate-900">{formatCurrency(order.finalAmount)}</span>
            </div>
            <div className="flex justify-between text-xs">
              <span className="text-slate-500">Ngày tạo đơn:</span>
              <span className="font-semibold text-slate-700">{formatBackendDateTime(order.createdAt)}</span>
            </div>
          </div>

          <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3.5">
            <p className="text-xs font-bold uppercase tracking-wider text-amber-800 mb-1">
              Nội dung / Lý do trả hàng từ khách hàng:
            </p>
            <p className="text-sm font-medium text-amber-950 whitespace-pre-wrap">
              {order.returnReason || "Khách hàng không cung cấp lý do chi tiết."}
            </p>
          </div>

          <div className="rounded-xl border border-indigo-100 bg-indigo-50/60 p-3 text-xs text-indigo-900 leading-relaxed">
            • <strong>Chấp nhận (Có)</strong>: Đơn chuyển sang <code>RETURNED</code>, hệ thống tự động hoàn kho các sản phẩm trong đơn.<br />
            • <strong>Từ chối (Không)</strong>: Đơn quay lại trạng thái <code>DELIVERED</code>.
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <AdminButton
            type="button"
            tone="secondary"
            onClick={() => onOpenChange(false)}
            disabled={acceptMutation.isPending || rejectMutation.isPending}
          >
            Đóng
          </AdminButton>
          <AdminButton
            type="button"
            tone="danger"
            onClick={() => { setError(null); rejectMutation.mutate(); }}
            disabled={acceptMutation.isPending || rejectMutation.isPending}
          >
            {rejectMutation.isPending && <LoaderCircle className="h-4 w-4 animate-spin" />}
            <X className="h-4 w-4" /> Từ chối (Không)
          </AdminButton>
          <AdminButton
            type="button"
            tone="primary"
            onClick={() => { setError(null); acceptMutation.mutate(); }}
            disabled={acceptMutation.isPending || rejectMutation.isPending}
          >
            {acceptMutation.isPending && <LoaderCircle className="h-4 w-4 animate-spin" />}
            <Check className="h-4 w-4" /> Chấp nhận (Có)
          </AdminButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function OrderStatusDialog({ order, open, onOpenChange }: { order: OrderResponse | null; open: boolean; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const [target, setTarget] = useState<OrderStatus | "">("");
  const [error, setError] = useState<string | null>(null);
  const transitions = order ? orderTransitions[order.status] : [];
  const isDelivered = order?.status === "DELIVERED";
  const mutation = useMutation({
    mutationFn: () => adminApi.orders.updateStatus(order!.id, { status: target as OrderStatus }),
    onSuccess: () => { toast.success("Đã cập nhật trạng thái đơn hàng"); queryClient.invalidateQueries({ queryKey: ["admin", "orders"] }); queryClient.invalidateQueries({ queryKey: adminQueryKeys.dashboard }); onOpenChange(false); },
    onError: (mutationError) => setError(getApiErrorMessage(mutationError)),
  });
  const submit = (event: FormEvent) => { event.preventDefault(); setError(null); if (isDelivered) return setError("Đơn hàng DELIVERED không thể đổi trạng thái"); if (!target) return setError("Vui lòng chọn trạng thái mới"); mutation.mutate(); };
  return <Dialog open={open} onOpenChange={onOpenChange}><DialogContent className="border-slate-200 bg-white text-slate-950"><DialogHeader><DialogTitle>Cập nhật đơn #{order?.id}</DialogTitle><DialogDescription className="text-slate-500">Trạng thái hiện tại: {order?.status}. Backend sẽ xử lý luồng nghiệp vụ tương ứng.</DialogDescription></DialogHeader><form className="space-y-4" onSubmit={submit}>{error && <div role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}{isDelivered ? <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm font-semibold text-slate-700">Đơn hàng đã được giao thành công (DELIVERED). Trạng thái này đã hoàn tất và không thể thay đổi thủ công. Chỉ khách hàng mới có thể gửi yêu cầu trả hàng.</div> : <div><label className="mb-1.5 block text-sm font-bold text-slate-700">Trạng thái mới</label><select className={fieldClassName} value={target} onChange={(event) => setTarget(event.target.value as OrderStatus)}><option value="">Chọn trạng thái</option>{transitions.map((status) => <option key={status} value={status}>{status}</option>)}</select></div>}{target === "RETURNED" && <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm font-semibold text-amber-800">Chấp nhận trả hàng: Hệ thống sẽ tự động nhập lại kho cho sản phẩm trong đơn.</div>}{target === "REFUNDED" && <div className="rounded-xl border border-teal-200 bg-teal-50 p-3 text-sm font-semibold text-teal-800">Hoàn tiền: Hệ thống sẽ thực hiện hoàn tiền qua VNPay Gateway hoặc ghi nhận hoàn tiền thủ công.</div>}{target === "CANCELLED" && <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm font-semibold text-rose-700">Hủy đơn có thể hoàn kho và coupon. Hãy kiểm tra kỹ trước khi xác nhận.</div>}<DialogFooter><AdminButton type="button" tone="secondary" onClick={() => onOpenChange(false)}>Đóng</AdminButton><AdminButton type="submit" tone={target === "CANCELLED" ? "danger" : "primary"} disabled={isDelivered || mutation.isPending || !target}>{mutation.isPending && <LoaderCircle className="h-4 w-4 animate-spin" />}Cập nhật</AdminButton></DialogFooter></form></DialogContent></Dialog>;
}

export function OrdersPage() {
  const [params, setParams] = useSearchParams();
  const [statusOrder, setStatusOrder] = useState<OrderResponse | null>(null);
  const [returnOrder, setReturnOrder] = useState<OrderResponse | null>(null);
  const [searchDraft, setSearchDraft] = useState(params.get("search") ?? "");
  const page = Math.max(0, Number(params.get("page") ?? 0));
  const size = [10, 20, 50, 100].includes(Number(params.get("size"))) ? Number(params.get("size")) : 20;
  const status = (params.get("status") ?? "") as OrderStatus | "";
  const search = params.get("search") ?? "";
  const queryParams = useMemo(() => ({ page, size, status, search }), [page, size, status, search]);
  const query = useQuery({
    queryKey: adminQueryKeys.resource("orders", queryParams),
    queryFn: async () => {
      const orderId = Number(search);
      if (search.trim()) {
        if (!Number.isInteger(orderId) || orderId <= 0) return oneItemPage<OrderResponse>(null, page, size);
        const order = await adminApi.orders.detail(orderId).catch(() => null);
        return oneItemPage(order && (!status || order.status === status) ? order : null, page, size);
      }
      return status ? adminApi.orders.byStatus(status, { page, size, sort: "createdAt,desc" }) : adminApi.orders.list({ page, size, sort: "createdAt,desc" });
    },
    placeholderData: (previous) => previous,
  });
  const updateParam = (key: string, value: string) => { const next = new URLSearchParams(params); value ? next.set(key, value) : next.delete(key); if (key !== "page") next.set("page", "0"); setParams(next); };
  const columns: AdminColumn<OrderResponse>[] = [
    { key: "order", header: "Đơn hàng", render: (order) => <div><Link to={`/admin/orders/${order.id}`} state={{ order }} className="font-extrabold text-slate-900 hover:text-indigo-600">#{order.id}</Link><p className="mt-1 text-xs text-slate-400">User #{order.userId}</p></div> },
    { key: "date", header: "Ngày tạo", render: (order) => formatBackendDateTime(order.createdAt) },
    { key: "amount", header: "Tổng tiền", render: (order) => <strong>{formatCurrency(order.finalAmount)}</strong> },
    { key: "shipping", header: "Vận chuyển", render: (order) => <div><p>{order.carrier || "—"}</p><p className="mt-1 text-xs text-slate-400">{order.trackingNumber || "Chưa có tracking"}</p></div> },
    { key: "payment", header: "Thanh toán", render: (order) => order.paymentProvider ?? "—" },
    { key: "status", header: "Trạng thái", render: (order) => <StatusBadge value={order.status} /> },
    { key: "actions", header: "Thao tác", render: (order) => (
      order.status === "RETURN_REQUESTED" ? (
        <AdminButton tone="warning" size="sm" onClick={() => setReturnOrder(order)}>
          <RotateCcw className="h-3.5 w-3.5" />Xử lý trả hàng
        </AdminButton>
      ) : (
        <AdminButton
          tone="secondary"
          size="sm"
          disabled={orderTransitions[order.status].length === 0}
          title={order.status === "DELIVERED" ? "Đơn hàng đã giao thành công (DELIVERED) - không thể chỉnh sửa trạng thái trực tiếp" : undefined}
          onClick={() => setStatusOrder(order)}
        >
          <Edit3 className="h-3.5 w-3.5" />Cập nhật
        </AdminButton>
      )
    ) },
  ];
  return <><PageHeader title="Đơn hàng" description="Theo dõi đơn bán hàng và chuyển trạng thái theo state machine an toàn." /><Panel><div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between"><form className="relative flex-1 sm:max-w-sm" onSubmit={(event) => { event.preventDefault(); updateParam("search", searchDraft.trim()); }}><Search className="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" /><input className={`${fieldClassName} pl-10`} inputMode="numeric" placeholder="Tìm theo mã đơn..." value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /></form><select aria-label="Lọc trạng thái đơn" className={`${fieldClassName} sm:w-60`} value={status} onChange={(event) => updateParam("status", event.target.value)}><option value="">Tất cả trạng thái</option>{orderStatuses.map((item) => <option key={item} value={item}>{item}</option>)}</select></div><AdminTable rows={query.data?.content ?? []} columns={columns} isLoading={query.isLoading} error={query.isError ? getApiErrorMessage(query.error) : null} onRetry={() => query.refetch()} /><PaginationBar page={page} totalPages={query.data?.totalPages ?? 0} totalElements={query.data?.totalElements ?? 0} size={size} onPageChange={(value) => updateParam("page", String(value))} onSizeChange={(value) => updateParam("size", String(value))} /></Panel><OrderStatusDialog key={statusOrder?.id ?? "closed"} order={statusOrder} open={Boolean(statusOrder)} onOpenChange={(open) => !open && setStatusOrder(null)} /><ReturnReviewDialog order={returnOrder} open={Boolean(returnOrder)} onOpenChange={(open) => !open && setReturnOrder(null)} /></>;
}

export function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const id = Number(orderId);
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const isReviewRequested = searchParams.get("review") === "true";
  const [statusOrder, setStatusOrder] = useState<OrderResponse | null>(null);
  const [returnReviewOrder, setReturnReviewOrder] = useState<OrderResponse | null>(null);
  const initialOrder = (location.state as { order?: OrderResponse } | null)?.order;
  const orderQuery = useQuery({ queryKey: adminQueryKeys.detail("orders", id), queryFn: () => adminApi.orders.detail(id), enabled: id > 0, initialData: initialOrder });
  const results = useQueries({ queries: [
    { queryKey: adminQueryKeys.resource("order-items", { orderId: id }), queryFn: () => adminApi.orderItems.byOrder(id, { page: 0, size: 100 }), enabled: id > 0 },
    { queryKey: adminQueryKeys.resource("order-histories", { orderId: id }), queryFn: () => adminApi.orderHistories.byOrder(id, { page: 0, size: 100 }), enabled: id > 0 },
    { queryKey: adminQueryKeys.resource("payments", { orderId: id }), queryFn: () => adminApi.payments.byOrder(id, { page: 0, size: 100 }), enabled: id > 0 },
  ] });

  const order = orderQuery.data ?? null;

  useEffect(() => {
    if (order && order.status === "RETURN_REQUESTED" && isReviewRequested) {
      setReturnReviewOrder(order);
    }
  }, [order, isReviewRequested]);

  if (orderQuery.isLoading || results.some((result) => result.isLoading)) return <LoadingScreen label="Đang tải dữ liệu đơn hàng..." />;
  const items = (results[0].data?.content ?? []) as OrderItemResponse[];
  const histories = [...((results[1].data?.content ?? []) as OrderHistoryResponse[])].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  const payments = (results[2].data?.content ?? []) as PaymentResponse[];

  const returnHistoryReason = histories.find((h) => h.status === "RETURN_REQUESTED")?.description?.replace(/^Khách hàng yêu cầu trả hàng\.?\s*(?:Lý do:\s*)?/i, "");

  return <><PageHeader title={`Đơn hàng #${id}`} description="Chi tiết đơn, sản phẩm, lịch sử và thanh toán được tải trực tiếp từ backend." actions={<><Link to="/admin/orders"><AdminButton tone="secondary"><ArrowLeft className="h-4 w-4" />Danh sách</AdminButton></Link>{order && order.status === "RETURN_REQUESTED" && <AdminButton tone="warning" onClick={() => setReturnReviewOrder(order)}><RotateCcw className="h-4 w-4" />Xử lý trả hàng</AdminButton>}{order && order.status !== "RETURN_REQUESTED" && orderTransitions[order.status].length > 0 && <AdminButton onClick={() => setStatusOrder(order)}><Edit3 className="h-4 w-4" />Cập nhật trạng thái</AdminButton>}</>} />{orderQuery.isError && <div role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{getApiErrorMessage(orderQuery.error)}</div>}
    {order && order.status === "RETURN_REQUESTED" && (
      <Panel className="border-2 border-amber-300 bg-gradient-to-r from-amber-50 to-orange-50 p-5 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2 text-amber-800">
              <RotateCcw className="h-5 w-5 animate-pulse text-amber-600" />
              <h3 className="text-base font-extrabold text-amber-950">Yêu cầu trả hàng từ khách hàng</h3>
            </div>
            <p className="text-sm text-amber-900 leading-relaxed">
              <span className="font-bold text-amber-950">Nội dung / Lý do trả hàng: </span>
              {order.returnReason || returnHistoryReason || "Khách hàng không cung cấp lý do chi tiết."}
            </p>
          </div>
          <div className="flex items-center gap-2.5 shrink-0">
            <AdminButton tone="warning" onClick={() => setReturnReviewOrder(order)}>
              <RotateCcw className="h-4 w-4" /> Xử lý yêu cầu (Có / Không)
            </AdminButton>
          </div>
        </div>
      </Panel>
    )}
    {order && <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4"><Panel className="p-4"><p className="text-xs font-bold uppercase text-slate-400">Trạng thái</p><div className="mt-2"><StatusBadge value={order.status} /></div></Panel><Panel className="p-4"><p className="text-xs font-bold uppercase text-slate-400">Tổng tiền</p><p className="mt-2 text-xl font-black">{formatCurrency(order.finalAmount)}</p></Panel><Panel className="p-4"><p className="text-xs font-bold uppercase text-slate-400">Khách hàng</p><p className="mt-2 font-black">User #{order.userId}</p></Panel><Panel className="p-4"><p className="text-xs font-bold uppercase text-slate-400">Ngày tạo</p><p className="mt-2 font-bold">{formatBackendDateTime(order.createdAt)}</p></Panel></div>}
    <div className="grid gap-6 xl:grid-cols-[1.35fr_0.65fr]"><Panel><div className="border-b border-slate-200 p-5"><h2 className="font-extrabold text-slate-900">Sản phẩm trong đơn</h2></div><AdminTable rows={items} columns={[{ key: "product", header: "Sản phẩm", render: (item) => <div><p className="font-bold text-slate-900">{item.product?.name ?? "—"}</p><p className="text-xs text-slate-400">Variant #{item.productVariantId}</p></div> }, { key: "quantity", header: "SL", render: (item) => item.quantity }, { key: "price", header: "Giá mua", render: (item) => formatCurrency(item.priceAtPurchase) }, { key: "total", header: "Thành tiền", render: (item) => <strong>{formatCurrency(item.priceAtPurchase * item.quantity)}</strong> }]} error={results[0].isError ? getApiErrorMessage(results[0].error) : null} /></Panel><Panel className="p-5"><h2 className="font-extrabold text-slate-900">Timeline trạng thái</h2><div className="mt-5 space-y-0">{histories.map((history, index) => <div key={history.id} className="relative flex gap-3 pb-5">{index < histories.length - 1 && <span className="absolute left-[7px] top-4 h-full w-px bg-slate-200" />}<span className="relative mt-1 h-4 w-4 shrink-0 rounded-full border-4 border-indigo-100 bg-indigo-600" /><div><StatusBadge value={history.status} /><p className="mt-2 text-xs leading-5 text-slate-500">{history.description || "Không có mô tả"}</p><p className="mt-1 text-[11px] text-slate-400">{formatBackendDateTime(history.createdAt)}</p></div></div>)}{!histories.length && <p className="text-sm text-slate-400">Chưa có lịch sử</p>}</div></Panel></div>
    <Panel><div className="border-b border-slate-200 p-5"><h2 className="font-extrabold text-slate-900">Payment attempts</h2></div><AdminTable rows={payments} columns={[{ key: "id", header: "ID", render: (payment) => `#${payment.id}` }, { key: "provider", header: "Cổng", render: (payment) => payment.provider }, { key: "transaction", header: "Mã giao dịch", render: (payment) => payment.transactionNo || "—" }, { key: "amount", header: "Số tiền", render: (payment) => formatCurrency(payment.amount) }, { key: "status", header: "Trạng thái", render: (payment) => <StatusBadge value={payment.status} /> }, { key: "created", header: "Ngày tạo", render: (payment) => formatBackendDateTime(payment.createdAt) }]} error={results[2].isError ? getApiErrorMessage(results[2].error) : null} /></Panel><OrderStatusDialog order={statusOrder} open={Boolean(statusOrder)} onOpenChange={(open) => !open && setStatusOrder(null)} /><ReturnReviewDialog order={returnReviewOrder} open={Boolean(returnReviewOrder)} onOpenChange={(open) => !open && setReturnReviewOrder(null)} /></>;
}

export function PaymentsPage() {
  const [params, setParams] = useSearchParams();
  const [editing, setEditing] = useState<PaymentResponse | null>(null);
  const [form, setForm] = useState<{ provider: PaymentProvider; status: PaymentStatus } | null>(null);
  const [searchDraft, setSearchDraft] = useState(params.get("orderId") ?? "");
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const page = Math.max(0, Number(params.get("page") ?? 0)); const size = Number(params.get("size") ?? 20); const status = (params.get("status") ?? "") as PaymentStatus | "";
  const orderId = Number(params.get("orderId") ?? 0);
  const query = useQuery({
    queryKey: adminQueryKeys.resource("payments", { page, size, status, orderId }),
    queryFn: async () => {
      if (orderId > 0) {
        const response = await adminApi.payments.byOrder(orderId, { page, size });
        return status ? { ...response, content: response.content.filter((payment) => payment.status === status), totalElements: response.content.filter((payment) => payment.status === status).length } : response;
      }
      return status ? adminApi.payments.byStatus(status, { page, size }) : adminApi.payments.list({ page, size });
    },
    placeholderData: (previous) => previous,
  });
  const updateParam = (key: string, value: string) => { const next = new URLSearchParams(params); value ? next.set(key, value) : next.delete(key); if (key !== "page") next.set("page", "0"); setParams(next); };
  const mutation = useMutation({ mutationFn: () => adminApi.payments.update(editing!.id, { orderId: editing!.orderId, provider: form!.provider, status: form!.status }), onSuccess: (response) => { if (response.status === editing?.status) toast.info("Backend trả về payment không thay đổi"); else toast.success("Đã cập nhật thanh toán"); setEditing(null); queryClient.invalidateQueries({ queryKey: ["admin", "payments"] }); queryClient.invalidateQueries({ queryKey: ["admin", "orders"] }); }, onError: (mutationError) => setError(getApiErrorMessage(mutationError)) });
  const columns: AdminColumn<PaymentResponse>[] = [{ key: "id", header: "Giao dịch", render: (payment) => <Link className="font-extrabold text-indigo-600" to={`/admin/payments/${payment.id}`}>#{payment.id}</Link> }, { key: "order", header: "Đơn", render: (payment) => <Link className="font-bold text-indigo-600" to={`/admin/orders/${payment.orderId}`}>#{payment.orderId}</Link> }, { key: "provider", header: "Cổng", render: (payment) => payment.provider }, { key: "transaction", header: "Mã giao dịch", render: (payment) => payment.transactionNo || "Backend tự sinh khi SUCCESS" }, { key: "amount", header: "Số tiền", render: (payment) => <strong>{formatCurrency(payment.amount)}</strong> }, { key: "status", header: "Trạng thái", render: (payment) => <StatusBadge value={payment.status} /> }, { key: "created", header: "Ngày tạo", render: (payment) => formatBackendDateTime(payment.createdAt) }, { key: "action", header: "Thao tác", render: (payment) => <AdminButton tone="secondary" size="sm" disabled={payment.status !== "PENDING"} onClick={() => { setEditing(payment); setForm({ provider: payment.provider, status: payment.status }); setError(null); }}><Edit3 className="h-3.5 w-3.5" />Xử lý</AdminButton> }];
  return <><PageHeader title="Thanh toán" description="Chỉ payment PENDING được phép đổi sang SUCCESS hoặc FAILED. Backend tự sinh mã giao dịch khi xác nhận thành công." /><Panel><div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between"><form className="relative flex-1 sm:max-w-sm" onSubmit={(event) => { event.preventDefault(); updateParam("orderId", searchDraft.trim()); }}><Search className="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" /><input className={`${fieldClassName} pl-10`} inputMode="numeric" placeholder="Tìm theo mã đơn..." value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /></form><select className={`${fieldClassName} sm:w-60`} value={status} onChange={(event) => updateParam("status", event.target.value)}><option value="">Tất cả trạng thái</option>{["PENDING", "SUCCESS", "FAILED", "REFUNDED"].map((item) => <option key={item}>{item}</option>)}</select></div><AdminTable rows={query.data?.content ?? []} columns={columns} isLoading={query.isLoading} error={query.isError ? getApiErrorMessage(query.error) : null} onRetry={() => query.refetch()} /><PaginationBar page={page} totalPages={query.data?.totalPages ?? 0} totalElements={query.data?.totalElements ?? 0} size={size} onPageChange={(value) => updateParam("page", String(value))} onSizeChange={(value) => updateParam("size", String(value))} /></Panel><Dialog open={Boolean(editing)} onOpenChange={(open) => !open && setEditing(null)}><DialogContent className="border-slate-200 bg-white text-slate-950"><DialogHeader><DialogTitle>Xử lý payment #{editing?.id}</DialogTitle><DialogDescription className="text-slate-500">SUCCESS có thể xác nhận đơn; FAILED có thể hủy đơn và hoàn kho/coupon.</DialogDescription></DialogHeader>{form && <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); setError(null); mutation.mutate(); }}>{error && <div role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}<div><label className="mb-1.5 block text-sm font-bold text-slate-700">Nhà cung cấp thanh toán</label><select className={fieldClassName} value={form.provider} onChange={(event) => setForm((value) => value && ({ ...value, provider: event.target.value as PaymentProvider }))}>{["VNPAY", "MOMO", "COD"].map((item) => <option key={item}>{item}</option>)}</select></div><div><label className="mb-1.5 block text-sm font-bold text-slate-700">Kết quả</label><select className={fieldClassName} value={form.status} onChange={(event) => setForm((value) => value && ({ ...value, status: event.target.value as PaymentStatus }))}><option value="PENDING">PENDING</option><option value="SUCCESS">SUCCESS</option><option value="FAILED">FAILED</option></select></div><DialogFooter><AdminButton type="button" tone="secondary" onClick={() => setEditing(null)}>Đóng</AdminButton><AdminButton type="submit" disabled={mutation.isPending}>{mutation.isPending && <LoaderCircle className="h-4 w-4 animate-spin" />}Xác nhận</AdminButton></DialogFooter></form>}</DialogContent></Dialog></>;
}

export function PaymentDetailPage() {
  const { paymentId } = useParams<{ paymentId: string }>();
  const id = Number(paymentId);
  const query = useQuery({ queryKey: adminQueryKeys.detail("payments", id), queryFn: () => adminApi.payments.detail(id), enabled: id > 0 });
  if (query.isLoading) return <LoadingScreen label="Đang tải giao dịch thanh toán..." />;
  if (!query.data) return <Panel className="p-8 text-center text-rose-600">{getApiErrorMessage(query.error)}</Panel>;
  const payment = query.data;
  return <><PageHeader title={`Payment #${payment.id}`} description={`Giao dịch của đơn hàng #${payment.orderId}`} actions={<Link to="/admin/payments"><AdminButton tone="secondary"><ArrowLeft className="h-4 w-4" />Danh sách</AdminButton></Link>} /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><Panel className="p-5"><p className="text-xs font-bold uppercase text-slate-400">Trạng thái</p><div className="mt-3"><StatusBadge value={payment.status} /></div></Panel><Panel className="p-5"><p className="text-xs font-bold uppercase text-slate-400">Số tiền</p><p className="mt-2 text-xl font-black">{formatCurrency(payment.amount)}</p></Panel><Panel className="p-5"><p className="text-xs font-bold uppercase text-slate-400">Cổng thanh toán</p><p className="mt-2 font-black">{payment.provider}</p></Panel><Panel className="p-5"><p className="text-xs font-bold uppercase text-slate-400">Ngày tạo</p><p className="mt-2 font-bold">{formatBackendDateTime(payment.createdAt)}</p></Panel></div><Panel className="p-6"><dl className="grid gap-5 md:grid-cols-2"><div><dt className="text-xs font-bold uppercase text-slate-400">Order ID</dt><dd className="mt-2"><Link className="font-bold text-indigo-600" to={`/admin/orders/${payment.orderId}`}>#{payment.orderId}</Link></dd></div><div><dt className="text-xs font-bold uppercase text-slate-400">Transaction No</dt><dd className="mt-2 font-mono text-sm font-bold text-slate-800">{payment.transactionNo || "—"}</dd></div></dl>{payment.status !== "PENDING" && <p className="mt-6 rounded-xl bg-slate-50 p-3 text-xs font-semibold text-slate-600">Backend chỉ thực sự cập nhật payment khi trạng thái hiện tại là PENDING.</p>}</Panel></>;
}
