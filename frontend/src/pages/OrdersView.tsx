import React, { useEffect, useMemo, useState, useRef } from "react";
import {
    AlertCircle,
    Check,
    RefreshCw,
    FileText,
    ShoppingBag,
    Clock,
    MapPin,
    CreditCard,
    Package,
    Truck,
    CheckCircle2,
    XCircle,
    RotateCcw,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { OrderItemRecord, OrderStatus, formatVND, mapProductResponseToLaptopProduct } from "../types";
import { useOrderStore } from "../stores";
import { ROUTES } from "../app/routePaths";
import { redirectToVNPay } from "../utils/vnpayNavigation";

export interface OrdersViewProps {
    selectedOrderId?: string;
    setSelectedAddressOrderId?: (id: string) => void;
    setOrders?: React.Dispatch<React.SetStateAction<OrderItemRecord[]>>;
    showToast: (msg: string) => void;
}

const STATUS_STEPS = [
    { code: "PENDING",   label: "Chờ thanh toán" },
    { code: "CONFIRMED", label: "Đã xác nhận" },
    { code: "SHIPPED",   label: "Đang giao" },
    { code: "DELIVERED", label: "Đã giao hàng" },
] as const;

function getStepIndex(status: string): number {
    // PAID / PROCESSING / CONFIRMED all map to step 1
    if (status === "PAID" || status === "PROCESSING") return 1;
    return STATUS_STEPS.findIndex((s) => s.code === status);
}

function StatusBadge({ status }: { status: string }) {
    const map: Record<string, { label: string; cls: string }> = {
        PENDING:          { label: "Đã đặt hàng",  cls: "bg-amber-50 text-amber-700 border-amber-200" },
        CONFIRMED:        { label: "Đã xác nhận",  cls: "bg-sky-50 text-sky-700 border-sky-200" },
        PAID:             { label: "Đã xác nhận",  cls: "bg-sky-50 text-sky-700 border-sky-200" },
        PROCESSING:       { label: "Đang xử lý",   cls: "bg-sky-50 text-sky-700 border-sky-200" },
        SHIPPED:          { label: "Đang giao",     cls: "bg-sky-50 text-sky-700 border-sky-200" },
        DELIVERED:        { label: "Đã giao hàng", cls: "bg-emerald-50 text-emerald-700 border-emerald-200" },
        CANCELLED:        { label: "Đã hủy",        cls: "bg-rose-50 text-rose-700 border-rose-200" },
        RETURN_REQUESTED: { label: "Yêu cầu hoàn", cls: "bg-amber-50 text-amber-700 border-amber-200" },
        RETURNED:         { label: "Đã hoàn",       cls: "bg-amber-50 text-amber-700 border-amber-200" },
        REFUNDED:         { label: "Đã hoàn tiền",  cls: "bg-emerald-50 text-emerald-700 border-emerald-200" },
    };
    const cfg = map[status] ?? { label: status, cls: "bg-slate-100 text-slate-500 border-slate-200" };
    return (
        <span className={`px-2 py-0.5 rounded text-[9px] font-sans font-bold normal-case border ${cfg.cls}`}>
            {cfg.label}
        </span>
    );
}

function HistoryStatusIcon({ status }: { status: string }) {
    switch (status) {
        case "PENDING":   return <Clock className="w-3.5 h-3.5 text-amber-700" />;
        case "CONFIRMED":
        case "PAID":
        case "PROCESSING": return <CheckCircle2 className="w-3.5 h-3.5 text-sky-600" />;
        case "SHIPPED":   return <Truck className="w-3.5 h-3.5 text-blue-600" />;
        case "DELIVERED": return <CheckCircle2 className="w-3.5 h-3.5 text-sky-700" />;
        case "CANCELLED": return <XCircle className="w-3.5 h-3.5 text-rose-700" />;
        case "RETURN_REQUESTED":
        case "RETURNED":
        case "REFUNDED":  return <RotateCcw className="w-3.5 h-3.5 text-orange-600" />;
        default:          return <Package className="w-3.5 h-3.5 text-slate-500" />;
    }
}

export default function OrdersView({
    selectedOrderId = "",
    setSelectedAddressOrderId = () => {},
    showToast,
}: OrdersViewProps) {
    const navigate = useNavigate();
    const {
        orders: storeOrders,
        fetchOrders,
        fetchOrderHistories,
        orderHistories,
        isLoading,
        isLoadingHistories,
        retryPayment,
        cancelOrder,
        requestReturn,
        error,
    } = useOrderStore();

    const [activeId, setActiveId] = useState<string>(selectedOrderId || "");
    const [isCancelling, setIsCancelling] = useState<boolean>(false);
    const [showReturnModal, setShowReturnModal] = useState<boolean>(false);
    const [returnReason, setReturnReason] = useState<string>("");
    const [isSubmittingReturn, setIsSubmittingReturn] = useState<boolean>(false);
    const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const [lastRefresh, setLastRefresh] = useState<Date>(new Date());

    const handleCancelOrder = async (orderId: number) => {
        if (!window.confirm(`Bạn có chắc chắn muốn hủy đơn hàng #${orderId} không?`)) return;
        setIsCancelling(true);
        try {
            await cancelOrder(orderId);
            showToast("Đã hủy đơn hàng thành công!");
        } catch (err: any) {
            showToast(err?.response?.data?.message || err?.message || "Không thể hủy đơn hàng!");
        } finally {
            setIsCancelling(false);
        }
    };

    const handleRequestReturn = async (orderId: number) => {
        setIsSubmittingReturn(true);
        try {
            await requestReturn(orderId, returnReason);
            showToast("Đã gửi yêu cầu trả hàng thành công! Vui lòng chờ bộ phận CSKH xử lý.");
            setShowReturnModal(false);
            setReturnReason("");
        } catch (err: any) {
            showToast(err?.response?.data?.message || err?.message || "Gửi yêu cầu trả hàng thất bại!");
        } finally {
            setIsSubmittingReturn(false);
        }
    };

    // Initial load + polling every 30s
    useEffect(() => {
        const refresh = () => {
            fetchOrders();
            fetchOrderHistories();
            setLastRefresh(new Date());
        };

        refresh();
        intervalRef.current = setInterval(refresh, 30_000);

        return () => {
            if (intervalRef.current) clearInterval(intervalRef.current);
        };
    }, [fetchOrderHistories, fetchOrders]);

    // Map Backend OrderResponse[] → UI OrderItemRecord[]
    const displayOrders: OrderItemRecord[] = useMemo(() => {
        if (storeOrders.length > 0) {
            return storeOrders.map((ord) => {
                const itemsMapped = (ord.orderItems || []).flatMap((it) => {
                    if (!it.product) return [];
                    const actualPrice = Number(it.priceAtPurchase ?? 0);
                    const variant = it.product.variants?.find((item) => item.id === it.productVariantId);
                    const prod = mapProductResponseToLaptopProduct(it.product, it.productVariantId ?? undefined);
                    return [{
                        product: prod,
                        quantity: it.quantity,
                        selectedRam: variant?.ram || "",
                        selectedStorage: variant?.rom || "",
                        selectedColorName: variant?.color?.name || "",
                        selectedColorHex: variant?.color?.hexCode || "",
                        price: actualPrice,
                    }];
                });

                return {
                    id: ord.id.toString(),
                    orderNumber: String(ord.id),
                    date: ord.createdAt ? new Date(ord.createdAt).toLocaleString("vi-VN") : "Chưa cập nhật",
                    status: ord.status,
                    paymentMethod: ord.paymentProvider,
                    trackingNumber: ord.trackingNumber || "",
                    items: itemsMapped,
                    subTotal: Number(ord.subTotal) || 0,
                    discountAmount: Number(ord.discountAmount) || 0,
                    shippingFee: Number(ord.shippingFee) || 0,
                    finalAmount: Number(ord.finalAmount) || 0,
                    shippingAddress: {
                        id: ord.shippingAddress?.id || 0,
                        fullName: ord.shippingAddress?.receiverName || "",
                        phone: ord.shippingAddress?.phone || "",
                        addressDetail: ord.shippingAddress?.street || "",
                        ward: ord.shippingAddress?.ward || "",
                        district: ord.shippingAddress?.district || "",
                        city: ord.shippingAddress?.city || "",
                        isDefault: false,
                    },
                };
            });
        }
        return [];
    }, [storeOrders]);

    // Sync active order when list loads
    useEffect(() => {
        if (displayOrders.length > 0 && !activeId) {
            setActiveId(displayOrders[0].id);
        }
    }, [activeId, displayOrders]);

    const currentOrder = displayOrders.find((o) => o.id === activeId) || displayOrders[0];

    const currentStepIdx =
        currentOrder && currentOrder.status !== "CANCELLED"
            ? getStepIndex(currentOrder.status)
            : -1;

    // Filter histories for current order
    const currentOrderHistories = useMemo(() => {
        if (!currentOrder) return [];
        return orderHistories.filter((h) => h.orderId === Number(currentOrder.id));
    }, [orderHistories, currentOrder]);

    const handleSelectOrder = (id: string) => {
        setActiveId(id);
        setSelectedAddressOrderId(id);
    };

    const manualRefresh = () => {
        fetchOrders();
        fetchOrderHistories();
        setLastRefresh(new Date());
    };

    return (
        <main className="container mx-auto px-4 sm:px-6 py-10 max-w-6xl">
            {/* ── Page Header ── */}
            <div className="flex items-start justify-between border-b border-slate-200 pb-6 mb-8 gap-4">
                <div>
                    <span className="text-[10px] font-sans normal-case tracking-normal text-sky-700">
                        LADUX CUSTOMER PORTAL
                    </span>
                    <h1 className="text-2xl sm:text-3xl font-semibold text-slate-800 tracking-tight mt-1">
                        QUẢN LÝ &amp; THEO DÕI ĐƠN HÀNG
                    </h1>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                    {/* Live refresh indicator */}
                    <div className="hidden sm:flex items-center gap-2 text-[10px] text-slate-500 font-sans">
                        <span className="w-1.5 h-1.5 rounded-full bg-sky-600 " />
                        <span>Cập nhật lúc {lastRefresh.toLocaleTimeString("vi-VN")}</span>
                    </div>
                    <button
                        onClick={manualRefresh}
                        disabled={isLoading}
                        className="flex items-center gap-1.5 text-[10px] font-bold normal-case tracking-normal text-slate-500 hover:text-sky-700 border border-slate-200 hover:border-sky-700/40 px-3 py-2 rounded-lg transition disabled:opacity-50"
                    >
                        <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? "animate-spin" : ""}`} />
                        <span className="hidden sm:inline">Làm mới</span>
                    </button>
                    <button
                        onClick={() => navigate(ROUTES.account)}
                        className="text-[10px] font-bold normal-case tracking-normal text-slate-500 hover:text-sky-700 transition"
                    >
                        ← Về tài khoản
                    </button>
                </div>
            </div>

            {error && (
                <div className="bg-red-500/20 border border-red-500 text-rose-700 p-4 rounded-xl text-xs font-sans text-center mb-6">
                    [Lỗi hệ thống]: {error}
                </div>
            )}

            {isLoading && displayOrders.length === 0 ? (
                <div className="text-center py-24 bg-white rounded-2xl border border-slate-200">
                    <div className="inline-block w-8 h-8 border-2 border-sky-700 border-t-transparent rounded-full animate-spin mb-4" />
                    <p className="text-slate-500 text-xs font-sans">Đang tải lịch sử đơn hàng từ hệ thống...</p>
                </div>
            ) : displayOrders.length === 0 ? (
                <div className="text-center py-20 bg-white rounded-2xl border border-slate-200 space-y-4">
                    <ShoppingBag className="w-14 h-14 text-slate-600 mx-auto" />
                    <h2 className="text-lg font-bold text-slate-800">Bạn chưa có đơn hàng nào!</h2>
                    <p className="text-xs text-slate-500 max-w-sm mx-auto leading-relaxed">
                        Hãy khám phá danh mục Laptop chính hãng tại Ladux và thực hiện đơn hàng đầu tiên.
                    </p>
                    <button
                        onClick={() => navigate(ROUTES.products)}
                        className="inline-flex bg-sky-600 text-white px-6 py-3 rounded-xl text-xs font-semibold normal-case hover:bg-sky-600 transition shadow-lg shadow-sky-700/20"
                    >
                        Khám phá sản phẩm ngay
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                    {/* ── LEFT: Order List ── */}
                    <div className="lg:col-span-4 space-y-2">
                        <h2 className="text-[10px] font-bold normal-case tracking-normal text-slate-500 mb-3">
                            Danh sách đơn hàng ({displayOrders.length})
                        </h2>
                        <div className="custom-scrollbar space-y-2 max-h-[calc(100vh-280px)] overflow-y-auto pr-1">
                            {displayOrders.map((ord) => (
                                <button
                                    key={ord.id}
                                    onClick={() => handleSelectOrder(ord.id)}
                                    className={`w-full text-left p-4 rounded-xl border cursor-pointer transition-all duration-200 space-y-2.5 ${
                                        activeId === ord.id
                                            ? "border-sky-700 bg-sky-50 shadow-sm"
                                            : "border-slate-200 bg-white hover:border-slate-200 hover:bg-white"
                                    }`}
                                >
                                    <div className="flex items-center justify-between text-xs">
                                        <span className="font-sans font-bold text-sky-700 tracking-wide">
                                            #{ord.orderNumber}
                                        </span>
                                        <span className="text-[10px] text-slate-400 font-sans">{ord.date.split(",")[0]}</span>
                                    </div>
                                    <p className="text-xs font-semibold text-slate-800 line-clamp-1 leading-snug">
                                        {ord.items[0]?.product?.name || "Không còn thông tin sản phẩm"}
                                        {ord.items.length > 1 && (
                                            <span className="text-slate-500 font-normal"> (+{ord.items.length - 1} khác)</span>
                                        )}
                                    </p>
                                    <div className="flex items-center justify-between pt-0.5">
                                        <span className="font-sans text-[12px] text-slate-600 font-bold">
                                            {formatVND(ord.finalAmount)}
                                        </span>
                                        <StatusBadge status={ord.status} />
                                    </div>
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* ── RIGHT: Order Detail ── */}
                    <div className="lg:col-span-8">
                        {!currentOrder ? (
                            <div className="p-10 bg-white border border-slate-200 rounded-2xl text-center text-slate-500 text-xs">
                                Chọn một đơn hàng bên trái để xem chi tiết.
                            </div>
                        ) : (
                            <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
                                {/* ── Section 1: Header metadata ── */}
                                <div className="p-6 sm:p-8 border-b border-slate-200">
                                    <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-5">
                                        <div>
                                            <span className="text-[10px] font-sans normal-case tracking-normal text-slate-500">
                                                MÃ ĐƠN HÀNG CHI TIẾT
                                            </span>
                                            <h2 className="text-2xl sm:text-3xl font-semibold font-sans text-sky-700 mt-0.5 tracking-tight">
                                                #{currentOrder.orderNumber}
                                            </h2>
                                            <p className="text-[11px] text-slate-500 mt-1.5 font-sans">
                                                Khởi tạo lúc{" "}
                                                <span className="text-slate-600">{currentOrder.date}</span>
                                                {" · "}Cổng thanh toán:{" "}
                                                <span className="font-bold text-slate-800">{currentOrder.paymentMethod || "Chưa cập nhật"}</span>
                                            </p>
                                        </div>
                                        <div className="sm:text-right shrink-0">
                                            <span className="text-[10px] font-sans normal-case tracking-normal text-slate-500 block mb-1.5">
                                                MÃ VẬN ĐƠN (TRACKING)
                                            </span>
                                            <span className="inline-flex items-center gap-2 text-xs font-sans font-bold text-slate-800 bg-white border border-slate-200 px-3 py-2 rounded-lg">
                                                <Truck className="w-3.5 h-3.5 text-sky-700" />
                                                {currentOrder.trackingNumber || "Chưa có mã vận đơn"}
                                            </span>
                                        </div>
                                    </div>
                                </div>

                                {/* ── Section 2: Status Stepper ── */}
                                <div className="px-6 sm:px-8 py-6 border-b border-slate-200">
                                    {currentOrder.status === "CANCELLED" ? (
                                        <div className="p-4 bg-red-500/[0.08] border border-red-500/25 rounded-xl text-rose-700 text-xs font-semibold flex items-center gap-2.5">
                                            <AlertCircle className="w-4 h-4 shrink-0" />
                                            Đơn hàng này đã bị hủy theo yêu cầu.
                                        </div>
                                    ) : (
                                        <div className="space-y-4">
                                            <h3 className="text-[10px] font-bold normal-case tracking-normal text-slate-500">
                                                Trạng thái tiến độ đơn hàng
                                            </h3>

                                            {/* Connected stepper */}
                                            <div className="relative flex items-start">
                                                {STATUS_STEPS.map((step, i) => {
                                                    const isDone = i <= currentStepIdx;
                                                    const isCurrent = i === currentStepIdx;
                                                    const isLast = i === STATUS_STEPS.length - 1;
                                                    return (
                                                        <React.Fragment key={step.code}>
                                                            {/* Step node */}
                                                            <div className="flex flex-col items-center gap-2 z-10">
                                                                <div
                                                                    className={`w-10 h-10 rounded-full flex items-center justify-center font-sans font-bold text-sm transition-all duration-500 ${
                                                                        isDone
                                                                            ? "bg-sky-600 text-white shadow-sm"
                                                                            : "bg-white border-2 border-slate-200 text-slate-400"
                                                                    }`}
                                                                >
                                                                    {isDone ? (
                                                                        <Check className="w-5 h-5 stroke-[3]" />
                                                                    ) : (
                                                                        <span>{i + 1}</span>
                                                                    )}
                                                                </div>
                                                                <span
                                                                    className={`text-[10px] sm:text-[11px] font-semibold text-center leading-tight max-w-[70px] ${
                                                                        isCurrent
                                                                            ? "text-sky-700 font-bold"
                                                                            : isDone
                                                                            ? "text-slate-800"
                                                                            : "text-slate-400"
                                                                    }`}
                                                                >
                                                                    {step.label}
                                                                </span>
                                                            </div>

                                                            {/* Connector line between steps */}
                                                            {!isLast && (
                                                                <div className="flex-1 relative top-5 mx-1">
                                                                    <div className="h-0.5 w-full bg-slate-100 rounded-full overflow-hidden">
                                                                        <div
                                                                            className={`h-full rounded-full transition-all duration-700 ${
                                                                                i < currentStepIdx
                                                                                    ? "w-full bg-sky-600"
                                                                                    : "w-0 bg-sky-600"
                                                                            }`}
                                                                        />
                                                                    </div>
                                                                </div>
                                                            )}
                                                        </React.Fragment>
                                                    );
                                                })}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* ── Section 3: Products ── */}
                                <div className="px-6 sm:px-8 py-6 border-b border-slate-200">
                                    <h3 className="text-[10px] font-bold normal-case tracking-normal text-slate-500 mb-4">
                                        Sản phẩm trong đơn ({currentOrder.items.length})
                                    </h3>
                                    <div className="space-y-3">
                                        {currentOrder.items.map((it, i) => (
                                            <div
                                                key={i}
                                                className="flex items-center justify-between gap-4 p-4 bg-white border border-slate-200 rounded-xl"
                                            >
                                                <div className="flex items-center gap-3 min-w-0">
                                                    {it.product.images?.[0] ? (
                                                        <img
                                                            src={it.product.images[0]}
                                                            alt={it.product.name}
                                                            className="w-16 h-16 shrink-0 bg-white rounded-lg object-contain p-1.5 border border-slate-200"
                                                        />
                                                    ) : (
                                                        <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-white text-[9px] text-slate-500">
                                                            Chưa có ảnh
                                                        </div>
                                                    )}
                                                    <div className="min-w-0">
                                                        <h4 className="text-xs font-bold text-slate-800 leading-snug line-clamp-2">
                                                            {it.product.name}
                                                        </h4>
                                                        {/* Specs line */}
                                                        {(it.selectedRam || it.selectedStorage || it.selectedColorName) && (
                                                            <p className="text-[10px] font-sans text-slate-500 mt-1 truncate">
                                                                {[
                                                                    it.selectedRam,
                                                                    it.selectedStorage,
                                                                    it.selectedColorName ? `Màu: ${it.selectedColorName}` : null,
                                                                ]
                                                                    .filter(Boolean)
                                                                    .join(" · ")}
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>
                                                <div className="text-right font-sans shrink-0">
                                                    <span className="text-xs font-bold text-slate-800 block">
                                                        {formatVND(it.price * it.quantity)}
                                                    </span>
                                                    <span className="text-[10px] text-slate-400">
                                                        {formatVND(it.price)} × {it.quantity}
                                                    </span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {/* ── Section 4: Shipping + Payment Summary ── */}
                                <div className="px-6 sm:px-8 py-6 border-b border-slate-200 grid grid-cols-1 sm:grid-cols-2 gap-5">
                                    {/* Shipping Info */}
                                    <div className="bg-white rounded-xl p-5 space-y-2">
                                        <div className="flex items-center gap-2 mb-3">
                                            <MapPin className="w-3.5 h-3.5 text-sky-700" />
                                            <span className="text-[10px] font-sans normal-case tracking-normal text-slate-500">
                                                Nơi nhận hàng
                                            </span>
                                        </div>
                                        <p className="text-sm font-bold text-slate-800">
                                            {currentOrder.shippingAddress.fullName}
                                        </p>
                                        <p className="text-[11px] font-sans text-slate-500">
                                            {currentOrder.shippingAddress.phone}
                                        </p>
                                        <p className="text-[11px] text-slate-600 leading-relaxed">
                                            {[
                                                currentOrder.shippingAddress.addressDetail,
                                                currentOrder.shippingAddress.ward,
                                                currentOrder.shippingAddress.district,
                                                currentOrder.shippingAddress.city,
                                            ]
                                                .filter(Boolean)
                                                .join(", ")}
                                        </p>

                                        {currentOrder.status === "PENDING" && (
                                            <div className="pt-3 mt-3 border-t border-slate-200">
                                                <button
                                                    type="button"
                                                    disabled={isCancelling}
                                                    onClick={() => handleCancelOrder(Number(currentOrder.id))}
                                                    className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs font-sans font-bold text-rose-700 hover:text-rose-700 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 hover:border-red-500/50 rounded-lg transition-all disabled:opacity-50"
                                                >
                                                    <XCircle className="w-4 h-4" />
                                                    {isCancelling ? "ĐANG HỦY ĐƠN..." : "HỦY ĐƠN HÀNG"}
                                                </button>
                                            </div>
                                        )}
                                    </div>

                                    {/* Payment Summary */}
                                    <div className="bg-white rounded-xl p-5 space-y-2.5">
                                        <div className="flex items-center gap-2 mb-3">
                                            <CreditCard className="w-3.5 h-3.5 text-sky-700" />
                                            <span className="text-[10px] font-sans normal-case tracking-normal text-slate-500">
                                                Tổng kết thanh toán
                                            </span>
                                        </div>
                                        <div className="flex justify-between text-xs font-sans text-slate-500">
                                            <span>Tạm tính:</span>
                                            <span className="text-slate-800">{formatVND(currentOrder.subTotal)}</span>
                                        </div>
                                        {currentOrder.discountAmount > 0 && (
                                            <div className="flex justify-between text-xs font-sans text-sky-700">
                                                <span>Giảm giá:</span>
                                                <span>-{formatVND(currentOrder.discountAmount)}</span>
                                            </div>
                                        )}
                                        <div className="flex justify-between text-xs font-sans text-slate-500">
                                            <span>Phí vận chuyển:</span>
                                            {currentOrder.shippingFee > 0 ? (
                                                <span className="text-slate-800">{formatVND(currentOrder.shippingFee)}</span>
                                            ) : (
                                                <span className="text-sky-700 font-bold">MIỄN PHÍ</span>
                                            )}
                                        </div>
                                        <div className="flex justify-between text-sm font-bold text-slate-800 pt-2.5 border-t border-slate-200 mt-1">
                                            <span>Tổng cộng:</span>
                                            <span className="text-sky-700 text-base">{formatVND(currentOrder.finalAmount)}</span>
                                        </div>
                                    </div>
                                </div>

                                {/* ── Section 5: Order History Timeline ── */}
                                <div className="px-6 sm:px-8 py-6 border-b border-slate-200">
                                    <h3 className="text-[10px] font-bold normal-case tracking-normal text-slate-500 mb-4">
                                        Lịch sử cập nhật đơn hàng
                                    </h3>

                                    {isLoadingHistories ? (
                                        <div className="flex items-center gap-2 text-xs text-slate-500 py-2">
                                            <div className="w-3.5 h-3.5 border border-sky-700 border-t-transparent rounded-full animate-spin" />
                                            Đang tải lịch sử...
                                        </div>
                                    ) : currentOrderHistories.length === 0 ? (
                                        <p className="text-[11px] text-slate-400 font-sans py-2">
                                            Chưa có bản ghi lịch sử cho đơn hàng này.
                                        </p>
                                    ) : (
                                        <div className="relative pl-5 space-y-0">
                                            {/* Vertical line */}
                                            <div className="absolute left-[7px] top-1 bottom-1 w-px bg-slate-100" />

                                            {currentOrderHistories.map((h, i) => (
                                                <div key={h.id} className="relative flex gap-4 pb-5 last:pb-0">
                                                    {/* Dot */}
                                                    <div className="absolute -left-[13px] top-0.5 w-4 h-4 rounded-full bg-white border border-slate-200 flex items-center justify-center">
                                                        <HistoryStatusIcon status={h.status} />
                                                    </div>

                                                    <div className="flex-1 min-w-0 pl-2">
                                                        <div className="flex items-center gap-2 flex-wrap">
                                                            <StatusBadge status={h.status} />
                                                            <span className="text-[10px] font-sans text-slate-400">
                                                                {h.createdAt
                                                                    ? new Date(h.createdAt).toLocaleString("vi-VN")
                                                                    : ""}
                                                            </span>
                                                        </div>
                                                        {h.description && (
                                                            <p className="text-[11px] text-slate-500 mt-1 leading-relaxed">
                                                                {h.description}
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                 {/* ── Section 6: Action Buttons ── */}
                                <div className="px-6 sm:px-8 py-5 flex flex-wrap items-center gap-3">
                                    {currentOrder.status === "PENDING" && currentOrder.paymentMethod === "VNPAY" && (
                                        <button
                                            onClick={async () => {
                                                try {
                                                    const res = await retryPayment(Number(currentOrder.id));
                                                    redirectToVNPay(res.paymentUrl, navigate);
                                                } catch (err: any) {
                                                    showToast(err?.response?.data?.message || err?.message || "Lỗi khi kết nối đến VNPay.");
                                                }
                                            }}
                                            className="flex items-center gap-2 bg-sky-600 text-white px-5 py-2.5 rounded-xl text-xs font-semibold normal-case hover:bg-sky-600 transition shadow-lg shadow-sky-700/20"
                                        >
                                            <RefreshCw className="w-3.5 h-3.5" />
                                            Thanh toán lại VNPay
                                        </button>
                                    )}

                                    {currentOrder.status === "DELIVERED" && (
                                        <button
                                            onClick={() => setShowReturnModal(true)}
                                            className="flex items-center gap-2 bg-amber-500/10 border border-amber-500/40 hover:bg-amber-500 hover:text-white text-amber-700 px-5 py-2.5 rounded-xl text-xs font-semibold normal-case transition shadow-md shadow-amber-500/10"
                                        >
                                            <RotateCcw className="w-3.5 h-3.5" />
                                            Yêu cầu trả hàng
                                        </button>
                                    )}

                                    {currentOrder.status === "RETURN_REQUESTED" && (
                                        <div className="flex items-center gap-2 bg-orange-500/10 border border-orange-500/30 text-orange-600 px-4 py-2.5 rounded-xl text-xs font-semibold">
                                            <Clock className="w-3.5 h-3.5 " />
                                            Đang chờ cửa hàng xem xét yêu cầu trả hàng
                                        </div>
                                    )} 
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Modal Yêu Cầu Trả Hàng */}
            {showReturnModal && currentOrder && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/25 backdrop-blur-sm animate-fadeIn">
                    <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-md w-full shadow-xl shadow-sky-100/50 space-y-4">
                        <div className="flex items-center gap-3 text-amber-700">
                            <RotateCcw className="w-6 h-6" />
                            <h3 className="text-lg font-bold text-slate-800">Yêu Cầu Trả Hàng / Hoàn Tiền</h3>
                        </div>
                        <p className="text-xs text-slate-500 leading-relaxed">
                            Bạn đang yêu cầu trả hàng cho đơn hàng <span className="font-sans text-slate-800 font-bold">#{currentOrder.id}</span>. Bộ phận CSKH sẽ xem xét và quyết định hoàn tiền.
                        </p>
                        <div>
                            <label className="block text-xs font-semibold text-slate-600 mb-1">
                                Lý do trả hàng (không bắt buộc):
                            </label>
                            <textarea
                                value={returnReason}
                                onChange={(e) => setReturnReason(e.target.value)}
                                placeholder="Nhập lý do trả hàng (ví dụ: sản phẩm không vừa ý, lỗi nhà sản xuất...)"
                                rows={3}
                                className="w-full bg-white border border-slate-200 rounded-xl p-3 text-xs text-slate-800 placeholder-neutral-600 focus:outline-none focus:border-amber-500 transition resize-none"
                            />
                        </div>
                        <div className="flex items-center justify-end gap-3 pt-2">
                            <button
                                onClick={() => {
                                    setShowReturnModal(false);
                                    setReturnReason("");
                                }}
                                disabled={isSubmittingReturn}
                                className="px-4 py-2 text-xs font-semibold text-slate-500 hover:text-slate-800 transition"
                            >
                                Hủy bỏ
                            </button>
                            <button
                                onClick={() => handleRequestReturn(Number(currentOrder.id))}
                                disabled={isSubmittingReturn}
                                className="flex items-center gap-2 bg-amber-500 hover:bg-amber-400 text-white px-5 py-2.5 rounded-xl text-xs font-bold normal-case transition disabled:opacity-50"
                            >
                                {isSubmittingReturn ? (
                                    <>
                                        <div className="w-3.5 h-3.5 border border-black border-t-transparent rounded-full animate-spin" />
                                        Đang gửi...
                                    </>
                                ) : (
                                    "Xác nhận gửi yêu cầu"
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </main>
    );
}
