import React from "react";
import { Award, Users, ShieldCheck } from "lucide-react";

export default function AboutView() {
    return (
        <main className="container mx-auto px-6 py-16 max-w-4xl space-y-16">
            <div className="text-center space-y-4">
                <span className="text-xs font-sans text-sky-700 normal-case tracking-normal">
                    ABOUT LADUX STORE
                </span>
                <h1 className="text-4xl sm:text-5xl font-semibold">
                    HỆ THỐNG PHÂN PHỐI LAPTOP CAO CẤP
                </h1>
                <p className="text-slate-500 text-sm max-w-2xl mx-auto leading-relaxed">
                    Ladux được thành lập với sứ mệnh định hình lại thị trường Laptop tại Việt Nam — tập
                    trung chuyên biệt 100% vào các dòng Laptop cao cấp nhất thế giới.
                </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                <div className="p-6 bg-white border border-slate-200 rounded-xl space-y-3">
                    <Award className="w-8 h-8 text-sky-700" />
                    <h3 className="font-bold text-sm text-slate-800">Chính Hãng 100%</h3>
                    <p className="text-xs text-slate-500 leading-relaxed">
                        Cam kết chỉ bán sản phẩm Laptop chính hãng nguyên seal nhập khẩu chính ngạch.
                    </p>
                </div>
                <div className="p-6 bg-white border border-slate-200 rounded-xl space-y-3">
                    <Users className="w-8 h-8 text-sky-700" />
                    <h3 className="font-bold text-sm text-slate-800">Chuyên Gia Tư Vấn</h3>
                    <p className="text-xs text-slate-500 leading-relaxed">
                        Đội ngũ kỹ thuật viên giàu kinh nghiệm trực tiếp hỗ trợ cài đặt AI & cân màu màn hình.
                    </p>
                </div>
                <div className="p-6 bg-white border border-slate-200 rounded-xl space-y-3">
                    <ShieldCheck className="w-8 h-8 text-sky-700" />
                    <h3 className="font-bold text-sm text-slate-800">Bảo Hành Vượt Trội</h3>
                    <p className="text-xs text-slate-500 leading-relaxed">
                        Bảo hành độc quyền 24 tháng, hỗ trợ máy thay thế trong thời gian bảo hành.
                    </p>
                </div>
            </div>
        </main>
    );
}
