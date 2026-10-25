import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { CheckCircle2, XCircle, Loader2, RefreshCw, ShoppingBag, Clock3, AlertCircle } from "lucide-react";
import { paymentService, orderService, type PaymentCallbackResponse } from "../services";
import { formatVND } from "../types";
import { ROUTES } from "../app/routePaths";
import { parseVNPayReturn, paymentOutcome, requireVNPayUrl } from "../utils/paymentFlow";

const copy = {
    success: { title: "Thanh toán thành công", description: "Đơn hàng đã được xác nhận thanh toán. Bạn có thể theo dõi tiến trình trong đơn hàng.", Icon: CheckCircle2 },
    pending: { title: "Đang chờ xác nhận thanh toán", description: "Chưa có kết quả cuối cùng từ VNPay. Nếu tài khoản đã bị trừ tiền, vui lòng không thanh toán thêm. Bạn có thể kiểm tra lại sau khoảng 5 phút hoặc liên hệ hỗ trợ.", Icon: Clock3 },
    failed: { title: "Giao dịch chưa hoàn tất", description: "Máy chủ đã ghi nhận giao dịch không thành công. Bạn chỉ có thể thanh toán lại khi đơn còn trong thời hạn thanh toán.", Icon: XCircle },
    refunded: { title: "Giao dịch đã được hoàn tiền", description: "Yêu cầu hoàn tiền đã được xác nhận. Vui lòng kiểm tra tài khoản ngân hàng theo thời gian xử lý của ngân hàng.", Icon: CheckCircle2 },
    review: { title: "Đã nhận thanh toán — cần đối soát", description: "VNPay đã xác nhận nhận tiền nhưng đơn hàng đã bị hủy. Ladux cần đối soát giao dịch để xử lý hoàn tiền. Vui lòng không thanh toán lại.", Icon: AlertCircle },
};

export default function VNPayReturnView() {
    const { search } = useLocation();
    const navigate = useNavigate();
    const [payment, setPayment] = useState<PaymentCallbackResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [checking, setChecking] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [retrying, setRetrying] = useState(false);
    const [checkVersion, setCheckVersion] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        let timer: ReturnType<typeof setTimeout> | undefined;
        setPayment(null);
        setLoading(true);
        setChecking(false);
        setError(null);
        const run = async () => {
            try {
                const fields = parseVNPayReturn(search);
                let current = await paymentService.verifyVNPayReturn(fields, controller.signal);
                if (controller.signal.aborted) return;
                setPayment(current);
                setLoading(false);
                if (current.status !== "PENDING") return;
                setChecking(true);
                // A signed server-to-server query also works when IPN cannot reach localhost.
                try { current = await paymentService.reconcileVNPay(fields.vnp_TxnRef, controller.signal); }
                catch { /* An unavailable query is still pending, never proof of failure. */ }
                if (controller.signal.aborted) return;
                setPayment(current);
                let polls = 0;
                const poll = async () => {
                    if (controller.signal.aborted) return;
                    try {
                        current = await paymentService.getMyPaymentByTxnRef(fields.vnp_TxnRef);
                        if (controller.signal.aborted) return;
                        setPayment(current);
                    } catch { /* Keep the last authenticated status on a transient failure. */ }
                    if (controller.signal.aborted) return;
                    if (current.status === "PENDING" && ++polls < 10) timer = setTimeout(poll, 2000);
                    else setChecking(false);
                };
                if (current.status === "PENDING") timer = setTimeout(poll, 2000);
                else setChecking(false);
            } catch (err: any) {
                if (!controller.signal.aborted) {
                    setError(err?.response?.data?.message || err?.message || "Không thể xác minh kết quả thanh toán.");
                    setLoading(false);
                    setChecking(false);
                }
            }
        };
        void run();
        return () => { controller.abort(); if (timer) clearTimeout(timer); };
    }, [search, checkVersion]);

    const retry = async () => {
        if (!payment?.orderId || !payment.canRetry || retrying) return;
        setRetrying(true);
        setError(null);
        try {
            const result = await orderService.retryPayment(payment.orderId);
            window.location.assign(requireVNPayUrl(result.paymentUrl));
        } catch (err: any) {
            setError(err?.response?.data?.message || err?.message || "Không thể tạo liên kết thanh toán lại.");
            setRetrying(false);
        }
    };

    const outcome = payment ? paymentOutcome(payment) : null;
    const content = outcome ? copy[outcome] : null;

    return <main className="container mx-auto max-w-2xl px-5 py-12 text-slate-800">
        <div className="store-card rounded-3xl border bg-white p-6 sm:p-9">
            {loading ? <div role="status" className="space-y-4 py-8 text-center">
                <Loader2 className="mx-auto h-10 w-10 animate-spin text-sky-700" />
                <h1 className="text-xl font-semibold">Đang xác minh kết quả VNPay...</h1>
            </div> : <>
                {content && <div className="space-y-4 text-center" role="status" aria-live="polite">
                    <content.Icon className={`mx-auto h-14 w-14 ${outcome === "success" ? "text-emerald-600" : "text-amber-600"}`} />
                    <h1 className="text-2xl font-semibold">{content.title}</h1>
                    <p className="text-sm leading-7 text-slate-500">{content.description}</p>
                    {checking && <p className="flex items-center justify-center gap-2 text-xs text-slate-500"><Loader2 className="h-4 w-4 animate-spin" />Đang kiểm tra cập nhật từ máy chủ...</p>}
                </div>}
                {error && <div role="alert" className="my-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
                {payment && <dl className="my-7 space-y-3 rounded-2xl border border-slate-200 p-4 text-sm">
                    <div className="flex justify-between gap-4"><dt>Mã đơn hàng</dt><dd className="font-semibold">#{payment.orderId}</dd></div>
                    <div className="flex justify-between gap-4"><dt>Mã tham chiếu</dt><dd className="break-all text-right">{payment.merchantTxnRef}</dd></div>
                    {payment.transactionNo && <div className="flex justify-between gap-4"><dt>Mã VNPay</dt><dd>{payment.transactionNo}</dd></div>}
                    <div className="flex justify-between gap-4"><dt>Số tiền</dt><dd className="font-bold text-sky-700">{formatVND(payment.amount)}</dd></div>
                </dl>}
                <div className="mt-6 flex flex-wrap justify-center gap-3">
                    {payment?.status === "PENDING" && <button type="button" disabled={checking} onClick={() => setCheckVersion(v => v + 1)} className="design-button disabled:opacity-50"><RefreshCw size={16} />Kiểm tra lại</button>}
                    {payment?.canRetry && <button type="button" disabled={retrying} onClick={() => void retry()} className="design-button disabled:opacity-50"><RefreshCw size={16} />{retrying ? "Đang tạo liên kết..." : "Thanh toán lại VNPay"}</button>}
                    <button type="button" onClick={() => navigate(ROUTES.orders)} className="design-button secondary"><ShoppingBag size={16} />Xem đơn hàng</button>
                    <button type="button" onClick={() => navigate(ROUTES.products)} className="design-button secondary">Tiếp tục mua sắm</button>
                </div>
            </>}
        </div>
    </main>;
}
