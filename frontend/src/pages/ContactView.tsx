import React, { useState } from "react";
import { MapPin, Phone, Mail, Send, Loader2 } from "lucide-react";
import { STOREFRONT_CONTACT } from "../config/storefront";
import { contactService } from "../services/contactService";

export interface ContactViewProps {
    showToast: (msg: string) => void;
}

export default function ContactView({ showToast }: ContactViewProps) {
    const [fullName, setFullName] = useState("");
    const [contact, setContact] = useState("");
    const [message, setMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        if (!fullName.trim() || !contact.trim() || !message.trim()) {
            showToast("Vui lòng điền đầy đủ thông tin liên hệ.");
            return;
        }

        try {
            setIsSubmitting(true);
            const res = await contactService.sendContactMessage({
                fullName: fullName.trim(),
                contact: contact.trim(),
                message: message.trim(),
            });
            showToast(res.message || "Yêu cầu của bạn đã được gửi thành công!");
            setFullName("");
            setContact("");
            setMessage("");
        } catch (error: any) {
            const errResponse = error?.response?.data?.message;
            if (errResponse) {
                showToast(errResponse);
            } else {
                // Fallback to mailto if email server is offline
                showToast("Đang mở trình duyệt email để gửi thư...");
                const subject = encodeURIComponent(`Yêu cầu hỗ trợ từ ${fullName}`);
                const body = encodeURIComponent(`Họ tên: ${fullName}\nLiên hệ: ${contact}\n\n${message}`);
                window.location.href = `mailto:${STOREFRONT_CONTACT.email}?subject=${subject}&body=${body}`;
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <main className="container mx-auto px-6 py-16 max-w-4xl space-y-12">
            <div className="text-center space-y-4">
                <span className="text-xs font-sans text-sky-700 normal-case tracking-normal">
                    CONTACT US
                </span>
                <h1 className="text-4xl font-semibold">LIÊN HỆ VỚI LADUX</h1>
                <p className="text-slate-500 text-sm">
                    Chúng tôi luôn sẵn sàng hỗ trợ bạn 24/7 về các giải pháp Laptop cao cấp.
                </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
                <form
                    onSubmit={handleSubmit}
                    className="p-6 bg-white border border-slate-200 rounded-2xl space-y-4 shadow-xl"
                >
                    <h3 className="font-bold text-sm text-slate-800 normal-case mb-4 flex items-center gap-2">
                        <Send className="w-4 h-4 text-sky-700" />
                        <span>Gửi Tin Nhắn Cho Chúng Tôi</span>
                    </h3>
                    <div>
                        <label className="block text-slate-500 text-xs mb-1">Họ & Tên</label>
                        <input
                            required
                            value={fullName}
                            onChange={(event) => setFullName(event.target.value)}
                            placeholder="Nhập họ tên của bạn..."
                            className="w-full p-3 bg-white border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:border-sky-700 transition font-medium"
                        />
                    </div>
                    <div>
                        <label className="block text-slate-500 text-xs mb-1">Email / Số Điện Thoại</label>
                        <input
                            required
                            value={contact}
                            onChange={(event) => setContact(event.target.value)}
                            placeholder="Nhập email hoặc số điện thoại..."
                            className="w-full p-3 bg-white border border-slate-200 rounded-xl text-xs text-slate-800 font-sans focus:outline-none focus:border-sky-700 transition font-medium"
                        />
                    </div>
                    <div>
                        <label className="block text-slate-500 text-xs mb-1">Nội Dung</label>
                        <textarea
                            rows={4}
                            required
                            value={message}
                            onChange={(event) => setMessage(event.target.value)}
                            placeholder="Cần hỗ trợ về dòng sản phẩm nào?..."
                            className="w-full p-3 bg-white border border-slate-200 rounded-xl text-xs text-slate-800 focus:outline-none focus:border-sky-700 transition font-medium resize-none"
                        ></textarea>
                    </div>
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="w-full bg-sky-600 hover:bg-sky-600 text-white py-3 rounded-xl text-xs font-semibold normal-case tracking-normal flex items-center justify-center gap-2 transition cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-sky-700/20"
                    >
                        {isSubmitting ? (
                            <>
                                <Loader2 className="w-4 h-4 animate-spin text-white" />
                                <span>Đang gửi yêu cầu...</span>
                            </>
                        ) : (
                            <>
                                <Send className="w-4 h-4 text-white stroke-[2.5]" />
                                <span>Gửi Yêu Cầu</span>
                            </>
                        )}
                    </button>
                </form>

                <div className="space-y-6 text-xs">
                    <div className="flex items-start gap-4 p-4 bg-white border border-slate-200 rounded-xl">
                        <MapPin className="w-5 h-5 text-sky-700 shrink-0" />
                        <div>
                            <h4 className="font-bold text-slate-800 mb-1">Showroom Chính</h4>
                            <p className="text-slate-500">{STOREFRONT_CONTACT.address || "Chưa cập nhật"}</p>
                        </div>
                    </div>
                    <div className="flex items-start gap-4 p-4 bg-white border border-slate-200 rounded-xl">
                        <Phone className="w-5 h-5 text-sky-700 shrink-0" />
                        <div>
                            <h4 className="font-bold text-slate-800 mb-1">Hotline Hỗ Trợ Khách Hàng</h4>
                            <p className="text-slate-500 font-sans">{STOREFRONT_CONTACT.phone || "Chưa cập nhật"}</p>
                        </div>
                    </div>
                    <div className="flex items-start gap-4 p-4 bg-white border border-slate-200 rounded-xl">
                        <Mail className="w-5 h-5 text-sky-700 shrink-0" />
                        <div>
                            <h4 className="font-bold text-slate-800 mb-1">Email Liên Hệ</h4>
                            <p className="text-slate-500 font-sans">
                                {STOREFRONT_CONTACT.email || "Chưa cập nhật"}
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    );
}
