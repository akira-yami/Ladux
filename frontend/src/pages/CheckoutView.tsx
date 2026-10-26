import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
    MapPin,
    Plus,
    Zap,
    AlertCircle,
    Trash2,
    Lock,
    CheckCircle2,
    Loader2,
    ShoppingBag,
} from "lucide-react";
import {
    CouponItem,
    PaymentProvider,
    formatVND,
    mapProductResponseToLaptopProduct,
} from "../types";
import { orderService, paymentService, couponService } from "../services";
import { useCartStore, useAddressStore, useOrderStore } from "../stores";
import { ROUTES } from "../app/routePaths";
import { redirectToVNPay } from "../utils/vnpayNavigation";
import { couponDiscount } from "../utils/checkoutPricing";
import type { CouponApplyResponse } from "../services/couponService";

export interface CheckoutViewProps {
    setSelectedAddressOrderId?: (id: string) => void;
    showToast: (msg: string) => void;
}

export default function CheckoutView({
    setSelectedAddressOrderId,
    showToast,
}: CheckoutViewProps) {
    const navigate = useNavigate();
    const { cart, fetchCart, isLoading: cartLoading, error: cartError } = useCartStore();
    const { addresses, createAddress, fetchAddresses } = useAddressStore();
    const { fetchOrders } = useOrderStore();

    const [selectedAddrId, setSelectedAddrId] = useState<number | null>(null);
    const [paymentProvider, setPaymentProvider] = useState<PaymentProvider>("COD");
    const [couponCodeInput, setCouponCodeInput] = useState<string>("");
    const [appliedCouponItem, setAppliedCouponItem] = useState<CouponItem | null>(null);
    const [appliedCoupon, setAppliedCoupon] = useState<CouponApplyResponse | null>(null);
    const [couponErrMsg, setCouponErrMsg] = useState<string>("");

    const [showAddressModal, setShowAddressModal] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const submissionLock = useRef(false);
    const [cartReady, setCartReady] = useState(false);
    const [createdOrderId, setCreatedOrderId] = useState<number | null>(null);

    const [newAddress, setNewAddress] = useState({
        receiverName: "",
        phone: "",
        street: "",
        ward: "",
        district: "",
        city: "",
        isDefault: false,
    });

    useEffect(() => {
        void fetchAddresses();
    }, [fetchAddresses]);

    useEffect(() => {
        let mounted = true;
        const refresh = () => {
            setCartReady(false);
            void fetchCart().finally(() => { if (mounted) setCartReady(true); });
        };
        const onPageShow = (event: PageTransitionEvent) => { if (event.persisted) refresh(); };
        refresh();
        window.addEventListener('pageshow', onPageShow);
        return () => { mounted = false; window.removeEventListener('pageshow', onPageShow); };
    }, [fetchCart]);

    useEffect(() => {
        if (addresses.length > 0 && selectedAddrId === null) {
            const def = addresses.find((a) => a.isDefault) || addresses[0];
            setSelectedAddrId(def.id);
        }
    }, [addresses, selectedAddrId]);

    const cartItemsList = cart?.items || [];
    const subTotal = Math.round(Number(cart?.totalPrice || 0));
    const shippingFee = Number(cart?.shippingFee || 0);
    const discountAmount = couponDiscount(subTotal, appliedCoupon);
    const finalAmount = Math.round(Math.max(0, subTotal - discountAmount + shippingFee));

    const handleApplyCoupon = async () => {
        if (!couponCodeInput.trim()) {
            setCouponErrMsg("Vui lòng nhập mã giảm giá");
            return;
        }
        setCouponErrMsg("");
        try {
            const res = await couponService.applyCoupon(couponCodeInput.trim());

            if (res.minOrderValue && subTotal < res.minOrderValue) {
                setCouponErrMsg(`Đơn hàng tối thiểu phải từ ${formatVND(res.minOrderValue)} để áp dụng mã này!`);
                return;
            }

            const isPercent = res.discountType === "PERCENT" || (res.discountType as string) === "PERCENTAGE";
            const calculatedDiscount = couponDiscount(subTotal, res);
            setAppliedCoupon(res);

            setAppliedCouponItem({
                code: res.code || couponCodeInput.trim().toUpperCase(),
                discountAmount: calculatedDiscount,
                minSubtotal: res.minOrderValue || 0,
                description: isPercent
                    ? `Giảm ${res.discountValue}%`
                    : `Giảm ${formatVND(res.discountValue)}`,
            });
            showToast("Đã áp dụng mã giảm giá thành công!");
        } catch (err: any) {
            const msg = err?.response?.data?.message || "Mã giảm giá không hợp lệ hoặc đã hết hạn!";
            setCouponErrMsg(msg);
        }
    };

    const handlePlaceOrder = async () => {
        if (submissionLock.current) return;
        if (createdOrderId) { navigate(ROUTES.orders, { replace: true }); return; }
        if (!cartItemsList.length) { showToast("Giỏ hàng đang trống. Hãy kiểm tra đơn hàng đã tạo."); return; }
        const selectedAddr = addresses.find((a) => a.id === selectedAddrId) || addresses[0];

        if (!selectedAddr) {
            showToast("Vui lòng chọn hoặc thêm địa chỉ giao hàng!");
            return;
        }

        const cleanPhone = (selectedAddr.phone || "").trim().replace(/[\s\-\.]/g, "");

        submissionLock.current = true;
        setIsSubmitting(true);
        try {
            // Bắn API Tạo Đơn Hàng Thật xuống Backend
            const orderRes = await orderService.createOrder({
                expectedAmount: finalAmount,
                couponCode: appliedCouponItem?.code,
                paymentProvider: paymentProvider,
                shippingAddress: {
                    receiverName: (selectedAddr.receiverName || (selectedAddr as any).fullName || "").trim(),
                    phone: cleanPhone,
                    street: (selectedAddr.street || (selectedAddr as any).addressLine || (selectedAddr as any).addressDetail || "").trim(),
                    ward: (selectedAddr.ward || "").trim(),
                    district: (selectedAddr.district || "").trim(),
                    city: (selectedAddr.city || "").trim(),
                },
            });

            setCreatedOrderId(orderRes.id);

            // The order transaction already clears the server cart. Never issue another DELETE.
            void fetchCart();
            void fetchOrders();
            setSelectedAddressOrderId?.(String(orderRes.id));

            // Xử lý luồng VNPay
            if (paymentProvider === "VNPAY") {
                showToast("Đang tạo cổng thanh toán VNPay...");
                try {
                    const payRes = await paymentService.createPayment({
                        orderId: orderRes.id,
                        provider: "VNPAY",
                    });
                    redirectToVNPay(payRes.paymentUrl, navigate);
                    return;
                } catch (payErr: any) {
                    console.error("Lỗi khởi tạo thanh toán VNPay:", payErr);
                    const errorMsg = payErr?.response?.data?.message || payErr?.message || "Không thể tạo URL thanh toán VNPay!";
                    navigate(ROUTES.orders, { replace: true });
                    showToast(`Đơn #${orderRes.id} đã được tạo. ${errorMsg} Bạn có thể tiếp tục thanh toán trong đơn hàng.`);
                    return;
                }
            }

            // Xử lý luồng COD
            if (setSelectedAddressOrderId) {
                setSelectedAddressOrderId(orderRes.id.toString());
            }
            navigate(ROUTES.orders, { replace: true });
            showToast(`Đặt hàng thành công! Mã đơn: #${orderRes.id}`);

        } catch (err: any) {
            console.error("Lỗi đặt hàng:", err);
            const errorMsg = err?.response?.data?.message || "Đặt hàng thất bại. Vui lòng thử lại!";
            void fetchCart();
            showToast(errorMsg);
        } finally {
            submissionLock.current = false;
            setIsSubmitting(false);
        }
    };

    if (createdOrderId || !cartReady || cartLoading || cartError || cartItemsList.length === 0) {
        const loading = !createdOrderId && (!cartReady || cartLoading);
        return <main className="container mx-auto max-w-2xl px-6 py-12">
            <div className="store-card space-y-5 rounded-2xl border border-slate-200 bg-white p-8 text-center text-slate-800">
                {loading ? <>
                    <Loader2 className="mx-auto h-10 w-10 animate-spin text-sky-700" />
                    <h1 role="status" className="text-xl font-semibold">Đang kiểm tra giỏ hàng...</h1>
                </> : <>
                    <ShoppingBag className="mx-auto h-10 w-10 text-sky-700" />
                    <h1 className="text-xl font-semibold">{createdOrderId ? `Đơn #${createdOrderId} đã được tạo` : cartError ? 'Chưa tải được giỏ hàng' : 'Giỏ hàng hiện đang trống'}</h1>
                    <p className="text-sm leading-7 text-slate-500">{createdOrderId
                        ? 'Bạn có thể xem đơn và tiếp tục thanh toán VNPay trong Đơn hàng của tôi.'
                        : cartError || 'Nếu vừa quay lại từ VNPay, đơn hàng của bạn đã được tạo. Hãy mở Đơn hàng của tôi để xem trạng thái và tiếp tục thanh toán.'}</p>
                    <div className="flex flex-wrap justify-center gap-3">
                        <button className="design-button" onClick={() => navigate(ROUTES.orders, { replace: true })}>Đơn hàng của tôi</button>
                        {cartError && <button className="design-button secondary" onClick={() => void fetchCart()}>Tải lại giỏ hàng</button>}
                        <button className="design-button secondary" onClick={() => navigate(ROUTES.products)}>Tiếp tục mua sắm</button>
                    </div>
                </>}
            </div>
        </main>;
    }

    return (
        <main className="container mx-auto px-6 py-12 max-w-6xl">
            <div className="flex items-center justify-between border-b border-slate-200 pb-6 mb-8">
                <div>
                    <span className="text-xs font-sans normal-case tracking-normal text-sky-700">
                        XÁC NHẬN ĐƠN HÀNG LADUX
                    </span>
                    <h1 className="text-3xl font-semibold text-slate-800 tracking-tight mt-1">
                        Thanh toán đơn hàng
                    </h1>
                </div>
                <button
                    onClick={() => navigate(ROUTES.cart)}
                    className="text-xs font-bold normal-case tracking-normal text-slate-500 hover:text-sky-700 transition"
                >
                    ← Trở lại giỏ hàng
                </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                {/* Left Column: Shipping Address, Coupons, Payment Method */}
                <div className="lg:col-span-7 space-y-8">
                    {/* 1. Shipping Address Selector */}
                    <div className="p-6 bg-white rounded-2xl border border-slate-200 space-y-5">
                        <div className="flex items-center justify-between">
                            <h2 className="text-sm font-semibold text-slate-800 normal-case tracking-normal flex items-center gap-2">
                                <MapPin className="w-4 h-4 text-sky-700" />
                                <span>1. ĐỊA CHỈ NHẬN HÀNG</span>
                            </h2>
                            <button
                                onClick={() => setShowAddressModal(true)}
                                className="text-xs font-bold text-sky-700 hover:underline flex items-center gap-1"
                            >
                                <Plus className="w-3.5 h-3.5" /> Thêm địa chỉ mới
                            </button>
                        </div>

                        <div className="space-y-3">
                            {addresses.length === 0 ? (
                                <p className="text-xs text-slate-500 py-2">
                                    Chưa có địa chỉ giao hàng nào. Vui lòng bấm "Thêm địa chỉ mới".
                                </p>
                            ) : (
                                addresses.map((addr) => (
                                    <div
                                        key={addr.id}
                                        onClick={() => setSelectedAddrId(addr.id)}
                                        className={`p-4 rounded-xl border cursor-pointer transition flex items-start justify-between gap-4 ${
                                            selectedAddrId === addr.id
                                                ? "border-sky-700 bg-sky-50"
                                                : "border-slate-200 bg-white hover:border-slate-200"
                                        }`}
                                    >
                                        <div className="space-y-1 text-xs">
                                            <div className="flex items-center gap-2">
                                                <span className="font-bold text-slate-800 text-sm">
                                                    {addr.receiverName || (addr as any).fullName}
                                                </span>
                                                <span className="font-sans text-slate-500">({addr.phone})</span>
                                                {addr.isDefault && (
                                                    <span className="bg-sky-50 border border-sky-700/40 text-sky-700 text-[9px] font-sans font-bold px-2 py-0.5 rounded">
                                                        Default
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-slate-600">
                                                {addr.street || (addr as any).addressLine}
                                                {addr.ward ? `, ${addr.ward}` : ""}
                                                {addr.district ? `, ${addr.district}` : ""}
                                                {addr.city ? `, ${addr.city}` : ""}
                                            </p>
                                        </div>
                                        <div
                                            className={`w-5 h-5 rounded-full border flex items-center justify-center shrink-0 mt-0.5 ${
                                                selectedAddrId === addr.id
                                                    ? "border-sky-700 bg-sky-600"
                                                    : "border-slate-200"
                                            }`}
                                        >
                                            {selectedAddrId === addr.id && (
                                                <div className="w-2 h-2 rounded-full bg-white" />
                                            )}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>

                    {/* Add Address Modal */}
                    {showAddressModal && (
                        <div className="fixed inset-0 z-50 bg-slate-900/25 backdrop-blur-sm flex items-center justify-center p-4">
                            <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-md w-full space-y-4">
                                <h3 className="text-base font-bold text-slate-800">Thêm Địa Chỉ Giao Hàng Mới</h3>
                                <div className="space-y-3 text-xs">
                                    <div>
                                        <label className="block text-slate-500 mb-1">Họ và tên người nhận</label>
                                        <input
                                            type="text"
                                            value={newAddress.receiverName}
                                            onChange={(e) => setNewAddress({ ...newAddress, receiverName: e.target.value })}
                                            className="w-full p-3 bg-white border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:border-sky-700"
                                            placeholder="Lê Huy"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-slate-500 mb-1">Số điện thoại</label>
                                        <input
                                            type="text"
                                            value={newAddress.phone}
                                            onChange={(e) => setNewAddress({ ...newAddress, phone: e.target.value })}
                                            className="w-full p-3 bg-white border border-slate-200 rounded-xl text-slate-800 font-sans focus:outline-none focus:border-sky-700"
                                            placeholder="0988 123 456"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-slate-500 mb-1">Địa chỉ chi tiết (Số nhà, đường)</label>
                                        <input
                                            type="text"
                                            value={newAddress.street}
                                            onChange={(e) => setNewAddress({ ...newAddress, street: e.target.value })}
                                            className="w-full p-3 bg-white border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:border-sky-700"
                                            placeholder="Số 88 Tôn Thất Thuyết"
                                        />
                                    </div>
                                    <div className="grid grid-cols-3 gap-2">
                                        <div>
                                            <label className="block text-slate-500 mb-1">Phường / Xã</label>
                                            <input
                                                type="text"
                                                value={newAddress.ward}
                                                onChange={(e) => setNewAddress({ ...newAddress, ward: e.target.value })}
                                                className="w-full p-3 bg-white border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:border-sky-700"
                                                placeholder="Mỹ Đình 2"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-slate-500 mb-1">Quận / Huyện</label>
                                            <input
                                                type="text"
                                                value={newAddress.district}
                                                onChange={(e) => setNewAddress({ ...newAddress, district: e.target.value })}
                                                className="w-full p-3 bg-white border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:border-sky-700"
                                                placeholder="Nam Từ Liêm"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-slate-500 mb-1">Tỉnh / Thành phố</label>
                                            <input
                                                type="text"
                                                value={newAddress.city}
                                                onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })}
                                                className="w-full p-3 bg-white border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:border-sky-700"
                                                placeholder="Hà Nội"
                                            />
                                        </div>
                                    </div>
                                </div>

                                <div className="flex gap-3 pt-2">
                                    <button
                                        onClick={() => setShowAddressModal(false)}
                                        className="flex-1 py-3 border border-slate-200 rounded-xl text-xs font-bold text-slate-500 hover:text-slate-800"
                                    >
                                        Hủy
                                    </button>
                                    <button
                                        onClick={async () => {
                                            if (!newAddress.receiverName || !newAddress.phone || !newAddress.street
                                                || !newAddress.ward || !newAddress.district || !newAddress.city) {
                                                showToast("Vui lòng điền đầy đủ thông tin địa chỉ");
                                                return;
                                            }
                                            try {
                                                const created = await createAddress(newAddress);
                                                setSelectedAddrId(created.id);
                                                setShowAddressModal(false);
                                                showToast("Đã lưu địa chỉ giao hàng mới!");
                                            } catch (err) {
                                                showToast("Không thể tạo địa chỉ mới!");
                                            }
                                        }}
                                        className="flex-1 py-3 bg-sky-600 text-white rounded-xl text-xs font-semibold hover:bg-sky-600"
                                    >
                                        Lưu Địa Chỉ
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* 2. Coupon Code Input */}
                    <div className="p-6 bg-white rounded-2xl border border-slate-200 space-y-4">
                        <h2 className="text-sm font-semibold text-slate-800 normal-case tracking-normal flex items-center gap-2">
                            <Zap className="w-4 h-4 text-sky-700" />
                            <span>2. MÃ GIẢM GIÁ / COUPON</span>
                        </h2>

                        <div className="flex gap-3">
                            <input
                                type="text"
                                value={couponCodeInput}
                                onChange={(e) => setCouponCodeInput(e.target.value)}
                                placeholder="Nhập mã giảm giá"
                                className="flex-1 bg-white border border-slate-200 rounded-xl px-4 py-3 text-xs text-slate-800 font-sans placeholder:text-slate-400 focus:outline-none focus:border-sky-700"
                            />
                            <button
                                onClick={handleApplyCoupon}
                                className="bg-sky-600 text-white px-6 py-3 rounded-xl text-xs font-semibold normal-case hover:bg-sky-600 transition shrink-0"
                            >
                                Áp dụng
                            </button>
                        </div>

                        {couponErrMsg && (
                            <p className="text-xs text-red-600 font-semibold flex items-center gap-1.5">
                                <AlertCircle className="w-3.5 h-3.5" />
                                {couponErrMsg}
                            </p>
                        )}

                        {appliedCouponItem && (
                            <div className="p-3 bg-sky-50 border border-sky-700/30 rounded-xl flex items-center justify-between text-xs">
                                <div>
                                    <span className="font-sans font-bold text-sky-700">
                                        {appliedCouponItem.code}
                                    </span>
                                    <p className="text-slate-500 text-[11px]">{appliedCouponItem.description}</p>
                                </div>
                                <button
                                    onClick={() => {
                                        setAppliedCouponItem(null);
                                        setAppliedCoupon(null);
                                        setCouponCodeInput("");
                                        showToast("Đã hủy mã giảm giá.");
                                    }}
                                    className="text-slate-500 hover:text-slate-800"
                                >
                                    <Trash2 className="w-4 h-4" />
                                </button>
                            </div>
                        )}
                    </div>

                    {/* 3. Payment Method Selection */}
                    <div className="p-6 bg-white rounded-2xl border border-slate-200 space-y-4">
                        <h2 className="text-sm font-semibold text-slate-800 normal-case tracking-normal flex items-center gap-2">
                            <Lock className="w-4 h-4 text-sky-700" />
                            <span>3. PHƯƠNG THỨC THANH TOÁN</span>
                        </h2>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            {/* VNPay Option */}
                            <div
                                onClick={() => setPaymentProvider("VNPAY")}
                                className={`p-4 rounded-xl border cursor-pointer transition flex items-center justify-between ${
                                    paymentProvider === "VNPAY"
                                        ? "border-sky-700 bg-sky-50"
                                        : "border-slate-200 bg-white hover:border-slate-200"
                                }`}
                            >
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-lg bg-sky-50 border border-sky-200 flex items-center justify-center font-semibold text-sky-700 text-xs">
                                        VNPay
                                    </div>
                                    <div>
                                        <span className="font-bold text-xs text-slate-800 block">Cổng VNPay</span>
                                        <span className="text-[10px] text-slate-500">Thẻ ATM / QR Code / E-Wallet</span>
                                    </div>
                                </div>
                                <div
                                    className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                        paymentProvider === "VNPAY" ? "border-sky-700 bg-sky-600" : "border-slate-200"
                                    }`}
                                >
                                    {paymentProvider === "VNPAY" && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                                </div>
                            </div>

                            {/* COD Option */}
                            <div
                                onClick={() => setPaymentProvider("COD")}
                                className={`p-4 rounded-xl border cursor-pointer transition flex items-center justify-between ${
                                    paymentProvider === "COD"
                                        ? "border-sky-700 bg-sky-50"
                                        : "border-slate-200 bg-white hover:border-slate-200"
                                }`}
                            >
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-lg bg-emerald-50 border border-emerald-200 flex items-center justify-center font-bold text-emerald-700 text-xs">
                                        COD
                                    </div>
                                    <div>
                                        <span className="font-bold text-xs text-slate-800 block">Thanh toán khi nhận</span>
                                        <span className="text-[10px] text-slate-500">Tiền mặt / Kiểm tra máy trước</span>
                                    </div>
                                </div>
                                <div
                                    className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                                        paymentProvider === "COD" ? "border-sky-700 bg-sky-600" : "border-slate-200"
                                    }`}
                                >
                                    {paymentProvider === "COD" && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column: Order Summary & Place Order Action */}
                <div className="lg:col-span-5 space-y-6">
                    <div className="p-6 bg-white rounded-2xl border border-slate-200 space-y-6 sticky top-24">
                        <h2 className="text-base font-semibold text-slate-800 normal-case tracking-normal pb-4 border-b border-slate-200">
                            TỔNG QUAN ĐƠN HÀNG
                        </h2>

                        {/* Itemized List */}
                        <div className="custom-scrollbar space-y-3 max-h-60 overflow-y-auto pr-1">
                            {cartItemsList.map((item) => {
                                if (!item.product) return null;
                                const mapped = mapProductResponseToLaptopProduct(item.product, item.productVariant?.id);
                                const price = Number(item.productVariant?.discountPrice ?? item.productVariant?.price ?? 0);
                                const imageUrl = mapped.images?.[0];

                                return (
                                    <div
                                        key={item.id}
                                        className="flex items-center justify-between gap-3 text-xs border-b border-slate-200 pb-3"
                                    >
                                        <div className="flex items-center gap-3">
                                            {imageUrl ? (
                                                <img
                                                    src={imageUrl}
                                                    alt={mapped.name}
                                                    className="w-12 h-12 bg-white rounded-lg object-contain p-1 border border-slate-200 shrink-0"
                                                />
                                            ) : (
                                                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-white text-[9px] text-slate-500">
                                                    Chưa có ảnh
                                                </div>
                                            )}
                                            <div>
                                                <p className="font-bold text-slate-800 line-clamp-1">{mapped.name}</p>
                                                <p className="text-[10px] font-sans text-slate-500">
                                                    {[item.productVariant?.ram, item.productVariant?.rom, item.productVariant?.color?.name]
                                                        .filter(Boolean)
                                                        .join(" · ") || "Chưa có cấu hình"} · x{item.quantity}
                                                </p>
                                            </div>
                                        </div>
                                        <span className="font-sans font-bold text-slate-800 shrink-0">
                                            {formatVND(price * item.quantity)}
                                        </span>
                                    </div>
                                );
                            })}
                        </div>

                        {/* Financial Summary */}
                        <div className="space-y-2.5 font-sans text-xs pt-2 border-t border-slate-200">
                            <div className="flex justify-between text-slate-500">
                                <span>Tạm tính:</span>
                                <span className="text-slate-800 font-semibold">
                                    {formatVND(subTotal)}
                                </span>
                            </div>

                            <div className="flex justify-between text-slate-500">
                                <span>Giảm giá:</span>
                                <span className="text-sky-700 font-semibold">
                                    -{formatVND(discountAmount)}
                                </span>
                            </div>

                            <div className="flex justify-between text-slate-500">
                                <span>Phí vận chuyển:</span>
                                <span className="text-slate-800 font-semibold">{formatVND(shippingFee)}</span>
                            </div>

                            <div className="pt-4 border-t border-slate-200 flex justify-between items-baseline">
                                <span className="text-sm font-bold text-slate-800 normal-case">Tổng cộng:</span>
                                <span className="text-2xl font-semibold text-sky-700">
                                    {formatVND(finalAmount)}
                                </span>
                            </div>
                        </div>

                        {/* Place Order CTA */}
                        <button
                            onClick={handlePlaceOrder}
                            disabled={isSubmitting || cartItemsList.length === 0}
                            className="w-full bg-sky-600 text-white py-4 rounded-xl font-semibold text-sm normal-case tracking-normal hover:bg-sky-600 transition shadow-lg shadow-sky-700/20 flex items-center justify-center gap-2 disabled:opacity-50"
                        >
                            <CheckCircle2 className="w-5 h-5" />
                            <span>{isSubmitting ? "ĐANG XỬ LÝ ĐƠN HÀNG..." : "ĐẶT HÀNG NGAY"}</span>
                        </button>

                        <div className="pt-2 flex items-center justify-center gap-4 text-[10px] text-slate-500 font-sans">
                            <span className="flex items-center gap-1">
                                <Lock className="w-3.5 h-3.5 text-sky-700" /> Thanh toán bảo mật
                            </span>
                            <span>•</span>
                            <span>Hỗ trợ đổi trả</span>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    );
}
