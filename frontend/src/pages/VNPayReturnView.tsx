import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { CheckCircle2, XCircle, Loader2, RefreshCw, ShoppingBag, ArrowRight } from "lucide-react";
import { paymentService, PaymentCallbackResponse } from "../services";
import { orderService } from "../services";
import { formatVND } from "../types";
import { ROUTES } from "../app/routePaths";

export default function VNPayReturnView() {
    const location = useLocation();
    const navigate = useNavigate();

    const [payment, setPayment] = useState<PaymentCallbackResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);
    const [isRetrying, setIsRetrying] = useState(false);

    const queryParams = new URLSearchParams(location.search);
    const txnRef = queryParams.get("vnp_TxnRef");
    const responseCode = queryParams.get("vnp_ResponseCode");

    useEffect(() => {
        if (!txnRef) {
            setErrorMsg("Không tìm thấy mã giao dịch (vnp_TxnRef) trong URL phản hồi.");
            setLoading(false);
            return;
        }

        let isSubscribed = true;
        let pollCount = 0;
        const maxPolls = 10; // Poll max 10 times (20 seconds)

        const checkStatus = async () => {
            try {
                const res = await paymentService.getMyPaymentByTxnRef(txnRef);
                if (!isSubscribed) return;

                setPayment(res);

                // If still pending after callback redirect, poll backend for background IPN updates
                if (res.status === "PENDING" && pollCount < maxPolls) {
                    pollCount++;
                    setTimeout(checkStatus, 2000);
                } else {
                    setLoading(false);
                }
            } catch (err: any) {
                if (!isSubscribed) return;
                console.error("Lỗi tra cứu thông tin thanh toán:", err);
                setErrorMsg(err?.response?.data?.message || "Không thể lấy thông tin giao dịch thanh toán từ máy chủ.");
                setLoading(false);
            }
        };

        checkStatus();

        return () => {
            isSubscribed = false;
        };
    }, [txnRef]);

    const handleRetryPayment = async () => {
        if (!payment?.orderId) return;
        setIsRetrying(true);
        try {
            const res = await orderService.retryPayment(payment.orderId);
            if (res.paymentUrl) {
                window.location.href = res.paymentUrl;
            } else {
                navigate(ROUTES.orders);
            }
        } catch (err: any) {
            console.error("Lỗi khởi tạo lại thanh toán:", err);
            setErrorMsg(err?.response?.data?.message || "Không thể khởi tạo lại giao dịch VNPay.");
            setIsRetrying(false);
        }
    };

    return (
        <main className="container mx-auto px-6 py-16 max-w-2xl text-slate-800">
            <div className="bg-white border border-slate-200 rounded-3xl p-8 shadow-xl shadow-sky-100/50 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-64 h-64 bg-[radial-gradient(circle_at_top_right,rgba(0,255,65,0.1),transparent_70%)] pointer-events-none" />

                {loading ? (
                    <div className="py-12 text-center space-y-4">
                        <Loader2 className="w-12 h-12 text-sky-700 animate-spin mx-auto" />
                        <h2 className="text-xl font-bold">Đang đối soát kết quả thanh toán VNPay...</h2>
                        <p className="text-xs text-slate-500 font-sans">
                            Vui lòng không đóng trình duyệt. LADUX đang xác thực dữ liệu từ cổng thanh toán.
                        </p>
                    </div>
                ) : errorMsg ? (
                    <div className="py-8 text-center space-y-6">
                        <div className="w-16 h-16 rounded-full bg-red-500/10 border border-red-500/30 flex items-center justify-center mx-auto text-red-500">
                            <XCircle className="w-8 h-8" />
                        </div>
                        <div className="space-y-2">
                            <h2 className="text-2xl font-semibold tracking-tight text-slate-800">Xác nhận thanh toán thất bại</h2>
                            <p className="text-sm text-slate-500">{errorMsg}</p>
                        </div>

                        <div className="pt-4 flex gap-4 justify-center">
                            <button
                                onClick={() => navigate(ROUTES.orders)}
                                className="px-6 py-3 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-600 hover:text-slate-800 transition"
                            >
                                Xem danh sách đơn hàng
                            </button>
                        </div>
                    </div>
                ) : payment?.status === "SUCCESS" ? (
                    <div className="py-6 text-center space-y-6">
                        <div className="w-16 h-16 rounded-full bg-sky-50 border border-sky-700/30 flex items-center justify-center mx-auto text-sky-700">
                            <CheckCircle2 className="w-8 h-8" />
                        </div>

                        <div className="space-y-2">
                            <span className="text-xs font-sans normal-case tracking-normal text-sky-700">
                                THANH TOÁN THÀNH CÔNG
                            </span>
                            <h2 className="text-3xl font-semibold tracking-tight text-slate-800">Cảm ơn bạn đã mua hàng!</h2>
                            <p className="text-xs text-slate-500">
                                Đơn hàng #{payment.orderId} đã được thanh toán thành công và đang được chuẩn bị để giao tới bạn.
                            </p>
                        </div>

                        {/* Payment Details Card */}
                        <div className="bg-white border border-slate-200 rounded-2xl p-5 text-xs font-sans space-y-3 text-left">
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">Mã giao dịch LADUX:</span>
                                <span className="text-slate-800 font-bold">{payment.merchantTxnRef || "N/A"}</span>
                            </div>
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">Mã giao dịch VNPay:</span>
                                <span className="text-slate-800 font-bold">{payment.transactionNo || queryParams.get("vnp_TransactionNo") || "N/A"}</span>
                            </div>
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">Số tiền thanh toán:</span>
                                <span className="text-sky-700 font-bold">{formatVND(payment.amount)}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-slate-500">Cổng thanh toán:</span>
                                <span className="text-slate-800 font-bold">{payment.provider}</span>
                            </div>
                        </div>

                        <div className="pt-4 flex flex-col sm:flex-row gap-4">
                            <button
                                onClick={() => navigate(ROUTES.orders)}
                                className="flex-1 py-4 bg-sky-600 text-white rounded-xl font-semibold text-xs normal-case tracking-normal hover:bg-sky-600 transition flex items-center justify-center gap-2 shadow-lg shadow-sky-700/20"
                            >
                                <ShoppingBag className="w-4 h-4" /> Xem đơn hàng của tôi
                            </button>
                            <button
                                onClick={() => navigate(ROUTES.products)}
                                className="flex-1 py-4 bg-white border border-slate-200 rounded-xl font-bold text-xs normal-case tracking-normal text-slate-600 hover:text-slate-800 transition flex items-center justify-center gap-2"
                            >
                                Tiếp tục mua sắm <ArrowRight className="w-4 h-4" />
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="py-6 text-center space-y-6">
                        <div className="w-16 h-16 rounded-full bg-red-500/10 border border-red-500/30 flex items-center justify-center mx-auto text-red-500">
                            <XCircle className="w-8 h-8" />
                        </div>

                        <div className="space-y-2">
                            <span className="text-xs font-sans normal-case tracking-normal text-red-600">
                                THANH TOÁN KHÔNG THÀNH CÔNG
                            </span>
                            <h2 className="text-2xl font-semibold tracking-tight text-slate-800">Giao dịch chưa hoàn tất</h2>
                            <p className="text-xs text-slate-500">
                                Giao dịch VNPay của bạn không thành công hoặc đã bị hủy (Mã phản hồi: {responseCode || "N/A"}).
                            </p>
                        </div>

                        {/* Detail Info */}
                        <div className="bg-white border border-slate-200 rounded-2xl p-5 text-xs font-sans space-y-3 text-left">
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">Mã đơn hàng:</span>
                                <span className="text-slate-800 font-bold">#{payment?.orderId}</span>
                            </div>
                            <div className="flex justify-between border-b border-slate-200 pb-2">
                                <span className="text-slate-500">Mã tham chiếu:</span>
                                <span className="text-slate-800 font-bold">{payment?.merchantTxnRef || txnRef}</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-slate-500">Số tiền:</span>
                                <span className="text-slate-800 font-bold">{payment ? formatVND(payment.amount) : "N/A"}</span>
                            </div>
                        </div>

                        <div className="pt-4 flex flex-col sm:flex-row gap-4">
                            <button
                                onClick={handleRetryPayment}
                                disabled={isRetrying}
                                className="flex-1 py-4 bg-sky-600 text-white rounded-xl font-semibold text-xs normal-case tracking-normal hover:bg-sky-600 transition flex items-center justify-center gap-2 disabled:opacity-50"
                            >
                                <RefreshCw className={`w-4 h-4 ${isRetrying ? "animate-spin" : ""}`} />
                                <span>{isRetrying ? "Đang tạo liên kết mới..." : "Thanh toán lại VNPay"}</span>
                            </button>
                            <button
                                onClick={() => navigate(ROUTES.orders)}
                                className="flex-1 py-4 bg-white border border-slate-200 rounded-xl font-bold text-xs normal-case tracking-normal text-slate-600 hover:text-slate-800 transition"
                            >
                                Xem danh sách đơn hàng
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </main>
    );
}
