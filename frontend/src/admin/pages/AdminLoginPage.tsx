import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ArrowLeft, ArrowRight, Eye, EyeOff, LoaderCircle, LockKeyhole, ShieldCheck, UserRound } from "lucide-react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import BrandMark from "../../components/common/BrandMark";
import ThemeToggle from "../../components/common/ThemeToggle";
import operationsArt from "../../assets/ladux/operations.webp";
import "../../styles/admin-v2.css";
import { useAdminAuth } from "../auth/AdminAuthProvider";
import { AdminButton, fieldClassName, LoadingScreen } from "../components/AdminUI";

const loginSchema = z.object({
  username: z.string().trim().min(1, "Vui lòng nhập tên đăng nhập"),
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});
type LoginValues = z.infer<typeof loginSchema>;

export default function AdminLoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const { status, login, isLoggingIn } = useAdminAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { username: "", password: "" } });

  if (status === "checking") return <LoadingScreen />;
  if (status === "authenticated") return <Navigate to="/admin/dashboard" replace />;

  const onSubmit = form.handleSubmit(async values => {
    setServerError(null);
    try {
      await login(values);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from?.startsWith("/admin") ? from : "/admin/dashboard", { replace: true });
    } catch (error) { setServerError(error instanceof Error ? error.message : "Đăng nhập thất bại"); }
  });

  return <div className="admin-login">
    <header className="admin-login-header"><Link to="/"><BrandMark /></Link><div className="flex items-center gap-4"><Link to="/" className="admin-login-back"><ArrowLeft size={14} />Về cửa hàng</Link><ThemeToggle /></div></header>
    <main className="admin-login-main">
      <section className="admin-login-story">
        <div className="admin-login-story-copy"><p className="section-eyebrow">LADUX WORKSPACE</p><h1>Mọi vận hành.<br /><span>Một không gian.</span></h1><p>Đơn hàng, sản phẩm và khách hàng.<br />Mọi thứ bạn cần để tạo nên trải nghiệm tốt hơn.</p></div>
        <img src={operationsArt} width={1254} height={1254} alt="Minh họa các khối hàng, laptop bạc và vòng kim loại cam cho Ladux Workspace" />
        <span className="admin-login-story-note"><ShieldCheck size={14} />Không gian dành riêng cho đội ngũ Ladux</span>
      </section>
      <section className="admin-login-form-section">
        <div className="admin-login-form">
          <span className="admin-login-icon"><LockKeyhole size={23} strokeWidth={1.5} /></span>
          <p className="section-eyebrow">WELCOME BACK</p>
          <h2>Chào mừng trở lại.</h2>
          <p className="admin-login-description">Đăng nhập để bắt đầu ngày làm việc của bạn.</p>
          <form onSubmit={onSubmit} className="mt-8 space-y-5" noValidate>
            {serverError && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{serverError}</div>}
            <div><label htmlFor="admin-username" className="mb-2 block text-xs font-semibold text-slate-700">Tên đăng nhập</label>
              <div className="relative"><UserRound className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" /><input id="admin-username" autoComplete="username" aria-invalid={!!form.formState.errors.username} aria-describedby={form.formState.errors.username ? "admin-username-error" : undefined} className={fieldClassName + " pl-10"} placeholder="Tên đăng nhập của bạn" {...form.register("username")} /></div>
              {form.formState.errors.username && <p id="admin-username-error" className="mt-1.5 text-xs font-medium text-rose-600">{form.formState.errors.username.message}</p>}
            </div>
            <div><label htmlFor="admin-password" className="mb-2 block text-xs font-semibold text-slate-700">Mật khẩu</label>
              <div className="relative"><LockKeyhole className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" /><input id="admin-password" type={showPassword ? "text" : "password"} autoComplete="current-password" aria-invalid={!!form.formState.errors.password} aria-describedby={form.formState.errors.password ? "admin-password-error" : undefined} className={fieldClassName + " px-10"} placeholder="Nhập mật khẩu" {...form.register("password")} /><button type="button" aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"} onClick={() => setShowPassword(value => !value)} className="absolute right-2 top-1.5 flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100">{showPassword ? <EyeOff size={15} /> : <Eye size={15} />}</button></div>
              {form.formState.errors.password && <p id="admin-password-error" className="mt-1.5 text-xs font-medium text-rose-600">{form.formState.errors.password.message}</p>}
            </div>
            <AdminButton type="submit" className="w-full" disabled={isLoggingIn}>{isLoggingIn ? <LoaderCircle size={16} className="animate-spin" /> : null}Đăng nhập workspace<ArrowRight className="ml-auto h-4 w-4" /></AdminButton>
          </form>
          <div className="admin-login-security"><ShieldCheck size={16} /><p>Không gian được bảo vệ.<br /><span>Sử dụng tài khoản được cấp quyền quản trị để tiếp tục.</span></p></div>
        </div>
      </section>
    </main>
    <footer className="admin-login-footer">© {new Date().getFullYear()} Ladux. Thoughtfully built for better work.</footer>
  </div>;
}
