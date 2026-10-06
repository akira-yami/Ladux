import React, { useMemo, useState } from "react";
import { ArrowLeft, Lock, AlertCircle } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { authService } from "../services";
import { useAuthStore } from "../stores"; // Import Store Auth

export interface LoginViewProps {
    onLoginSuccess?: () => void;
    onLogin?: () => void;
    onGoRegister: () => void;
    onBack: () => void;
    oauthReturnTo?: string;
}

function GoogleIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" className="h-5 w-5 shrink-0">
            <path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.4-.18-2.07H12v3.92h5.38a4.6 4.6 0 0 1-2 3.02v2.54h3.24c1.9-1.75 2.98-4.33 2.98-7.41Z" />
            <path fill="#34A853" d="M12 22c2.7 0 4.97-.9 6.62-2.36l-3.24-2.54c-.9.6-2.05.96-3.38.96-2.61 0-4.82-1.76-5.61-4.13H3.04v2.62A10 10 0 0 0 12 22Z" />
            <path fill="#FBBC05" d="M6.39 13.93A6.02 6.02 0 0 1 6.08 12c0-.67.11-1.32.31-1.93V7.45H3.04A10 10 0 0 0 2 12c0 1.61.39 3.14 1.04 4.55l3.35-2.62Z" />
            <path fill="#EA4335" d="M12 5.94c1.47 0 2.79.5 3.83 1.5l2.87-2.87A9.63 9.63 0 0 0 12 2a10 10 0 0 0-8.96 5.45l3.35 2.62C7.18 7.7 9.39 5.94 12 5.94Z" />
        </svg>
    );
}

export default function LoginView({
    onLoginSuccess,
    onLogin,
    onGoRegister,
    onBack,
    oauthReturnTo,
}: LoginViewProps) {
    const [username, setUsername] = useState(""); // Backend dùng username hoặc email
    const [password, setPassword] = useState("");
    const [localError, setLocalError] = useState<string | null>(null);
    const [isOAuthRedirecting, setIsOAuthRedirecting] = useState(false);
    const [searchParams] = useSearchParams();
    
    // Lấy state và hàm login từ Zustand Auth Store
    const { login, isLoading, error, clearError } = useAuthStore();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        clearError();
        setLocalError(null);
        try {
            // Bắn API POST /api/v1/auth/login xuống Spring Boot
            await login({ username, password });

            // Chỉ chuyển trang nếu thực sự đăng nhập thành công
            const isSuccess = useAuthStore.getState().isLoggedIn;
            if (isSuccess) {
                if (onLoginSuccess) {
                    onLoginSuccess();
                } else if (onLogin) {
                    onLogin();
                }
            } else {
                setLocalError("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!");
            }
        } catch (err: any) {
            console.error("Lỗi đăng nhập:", err);
            const errMsg =
                err?.response?.data?.message ||
                err?.message ||
                "Sai tên đăng nhập hoặc mật khẩu. Vui lòng kiểm tra lại!";
            setLocalError(errMsg);
            // Giữ nguyên ở trang đăng nhập, KHÔNG chuyển hướng
        }
    };

    const oauthError = useMemo(() => {
        const reason = searchParams.get("reason");
        if (searchParams.get("oauth2Error") !== "true" && !reason) return null;
        if (reason === "session_not_established") {
            return "Google đã xác thực nhưng LADUX không thể tạo phiên đăng nhập. Vui lòng thử lại.";
        }
        return "Không thể đăng nhập bằng Google. Vui lòng thử lại hoặc dùng tài khoản LADUX.";
    }, [searchParams]);

    const handleGoogleLogin = () => {
        clearError();
        setLocalError(null);
        setIsOAuthRedirecting(true);
        authService.startGoogleLogin(oauthReturnTo);
    };

    const activeError = error || localError || oauthError;

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
                            <Lock className="w-6 h-6 text-sky-700" />
                        </div>
                        <h1 className="text-2xl font-semibold tracking-tight text-slate-800">Đăng nhập</h1>
                        <p className="mt-1.5 text-xs text-slate-500">Chào mừng trở lại với LADUX</p>
                    </div>

                    {/* Hiển thị thông báo lỗi thật từ Backend nếu có */}
                    {activeError && (
                        <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-600 text-xs font-bold flex items-center gap-2.5 animate-in fade-in zoom-in duration-200">
                            <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
                            <span>{activeError}</span>
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label className="block text-[11px] font-bold normal-case tracking-normal text-slate-500 mb-2">
                                Tên đăng nhập
                            </label>
                            <input
                                type="text"
                                required
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="Nhập tên đăng nhập..."
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
                                placeholder="••••••••"
                                className="w-full rounded-xl border border-sky-100 bg-white px-4 py-3 text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-sky-700/60 focus:ring-1 focus:ring-sky-700/30 transition"
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full rounded-xl bg-sky-600 py-3.5 text-sm font-semibold text-white normal-case tracking-normal hover:bg-sky-600 disabled:opacity-60 transition-all hover:scale-[1.02] active:scale-100 mt-2"
                        >
                            {isLoading ? "Đang xác thực API..." : "Đăng Nhập"}
                        </button>
                    </form>

                    <div className="my-6 flex items-center gap-3" aria-hidden="true">
                        <span className="h-px flex-1 bg-sky-50" />
                        <span className="text-[10px] font-bold normal-case tracking-normal text-slate-400">Hoặc</span>
                        <span className="h-px flex-1 bg-sky-50" />
                    </div>

                    <button
                        type="button"
                        onClick={handleGoogleLogin}
                        disabled={isLoading || isOAuthRedirecting}
                        className="flex w-full items-center justify-center gap-3 rounded-xl border border-sky-100 bg-white px-4 py-3.5 text-sm font-semibold text-slate-800 transition-all hover:border-sky-100 hover:bg-neutral-100 disabled:cursor-wait disabled:opacity-60"
                    >
                        <GoogleIcon />
                        <span>{isOAuthRedirecting ? "Đang chuyển tới Google..." : "Đăng nhập bằng Google"}</span>
                    </button>

                    <div className="mt-6 text-center text-xs text-slate-500">
                        Nếu chưa có tài khoản:{" "}
                        <button
                            type="button"
                            onClick={onGoRegister}
                            className="font-bold text-sky-700 hover:underline cursor-pointer transition-colors"
                        >
                            Đăng ký ngay
                        </button>
                    </div>
                </div>
            </div>
        </main>
    );
}
