import React, { useState } from "react";
import { ArrowLeft, UserPlus } from "lucide-react";
import { useAuthStore } from "../stores";

export interface RegisterViewProps {
    onRegister: () => void;
    onGoLogin: () => void;
    onBack: () => void;
}

export default function RegisterView({ onRegister, onGoLogin, onBack }: RegisterViewProps) {
    const [name, setName] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirm, setConfirm] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const { register, login } = useAuthStore();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (password !== confirm) {
            setError("Mật khẩu xác nhận không khớp.");
            return;
        }
        setError("");
        setLoading(true);

        try {
            // 1. Gọi API đăng ký tài khoản xuống Backend
            await register({
                fullName: name.trim(),
                username: username.trim(),
                password: password,
            });

            // 2. Tự động đăng nhập với tài khoản vừa tạo
            await login({
                username: username.trim(),
                password: password,
            });

            setLoading(false);
            onRegister();
        } catch (err: any) {
            setLoading(false);
            const errMsg =
                err?.response?.data?.message ||
                err?.message ||
                "Đăng ký thất bại. Vui lòng kiểm tra lại!";
            setError(errMsg);
        }
    };

    return (
        <main className="min-h-[80vh] flex items-center justify-center px-4 py-20">
            <div className="w-full max-w-md">
                <button
                    onClick={onBack}
                    className="flex items-center gap-2 text-xs font-bold normal-case tracking-normal text-slate-500 hover:text-sky-700 mb-10 transition-colors"
                >
                    <ArrowLeft className="w-3.5 h-3.5" /> Quay lại cửa hàng
                </button>

                <div className="store-card rounded-3xl border border-sky-100 bg-white p-8 sm:p-10">
                    <div className="mb-8 text-center">
                        <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-sky-50 border border-sky-700/30 mb-4">
                            <UserPlus className="w-6 h-6 text-sky-700" />
                        </div>
                        <h1 className="text-2xl font-semibold tracking-tight text-slate-800">Tạo tài khoản</h1>
                        <p className="mt-1.5 text-xs text-slate-500">Tham gia cộng đồng thành viên LADUX</p>
                    </div>

                    {error && (
                        <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-600 text-xs text-center font-medium">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label className="block text-[11px] font-bold normal-case tracking-normal text-slate-500 mb-2">
                                Tên đăng nhập (Username)
                            </label>
                            <input
                                type="text"
                                required
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="nguyenvana"
                                className="w-full rounded-xl border border-sky-100 bg-white px-4 py-3 text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-sky-700/60 focus:ring-1 focus:ring-sky-700/30 transition"
                            />
                        </div>

                        <div>
                            <label className="block text-[11px] font-bold normal-case tracking-normal text-slate-500 mb-2">
                                Họ & Tên
                            </label>
                            <input
                                type="text"
                                required
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Nguyễn Văn A"
                                className="w-full rounded-xl border border-sky-100 bg-white px-4 py-3 text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-sky-700/60 focus:ring-1 focus:ring-sky-700/30 transition"
                            />
                        </div>


                        <div>
                            <label className="block text-[11px] font-bold normal-case tracking-normal text-slate-500 mb-2">
                                Mật khẩu
                            </label>
                            <input
                                type="password"
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Tối thiểu 8 ký tự"
                                className="w-full rounded-xl border border-sky-100 bg-white px-4 py-3 text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-sky-700/60 focus:ring-1 focus:ring-sky-700/30 transition"
                            />
                        </div>

                        <div>
                            <label className="block text-[11px] font-bold normal-case tracking-normal text-slate-500 mb-2">
                                Xác nhận mật khẩu
                            </label>
                            <input
                                type="password"
                                required
                                value={confirm}
                                onChange={(e) => setConfirm(e.target.value)}
                                placeholder="Nhập lại mật khẩu"
                                className="w-full rounded-xl border border-sky-100 bg-white px-4 py-3 text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-sky-700/60 focus:ring-1 focus:ring-sky-700/30 transition"
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full rounded-xl bg-sky-600 py-3.5 text-sm font-semibold text-white normal-case tracking-normal hover:bg-sky-600 disabled:opacity-60 transition-all hover:scale-[1.02] active:scale-100 mt-2"
                        >
                            {loading ? "Đang xử lý API..." : "Đăng Ký"}
                        </button>
                    </form>

                    <p className="mt-7 text-center text-xs text-slate-500">
                        Đã có tài khoản?{" "}
                        <button
                            onClick={onGoLogin}
                            className="font-bold text-sky-700 hover:underline"
                        >
                            Đăng nhập
                        </button>
                    </p>
                </div>

                <p className="mt-6 text-center text-[10px] text-slate-400 font-sans normal-case tracking-normal">
                    Bảo mật SSL · Dữ liệu mã hóa 256-bit
                </p>
            </div>
        </main>
    );
}
