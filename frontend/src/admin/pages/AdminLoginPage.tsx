import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ArrowRight, Eye, EyeOff, LoaderCircle, LockKeyhole, ShieldCheck, UserRound } from "lucide-react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import laduxLogo from "../../assets/ladux-logo.png";
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

  const onSubmit = form.handleSubmit(async (values) => {
    setServerError(null);
    try {
      await login(values);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from?.startsWith("/admin") ? from : "/admin/dashboard", { replace: true });
    } catch (error) {
      setServerError(error instanceof Error ? error.message : "Đăng nhập thất bại");
    }
  });

  return (
    <div className="grid min-h-screen bg-slate-950 lg:grid-cols-[1.08fr_0.92fr]">
      <section className="relative hidden overflow-hidden lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_15%_15%,rgba(99,102,241,0.45),transparent_34%),radial-gradient(circle_at_90%_80%,rgba(16,185,129,0.18),transparent_32%)]" />
        <div className="absolute -right-24 top-20 h-96 w-96 rounded-full border border-indigo-400/20" />
        <div className="absolute -right-8 top-36 h-72 w-72 rounded-full border border-indigo-400/15" />
        <div className="relative flex items-center gap-3"><img src={laduxLogo} alt="LADUX" className="h-12 w-12 rounded-2xl ring-1 ring-white/20" /><div><p className="text-xl font-black tracking-[0.16em] text-white">LADUX</p><p className="text-xs font-bold uppercase tracking-[0.2em] text-indigo-300">Administration</p></div></div>
        <div className="relative max-w-xl">
          <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-xs font-bold text-indigo-200 backdrop-blur"><ShieldCheck className="h-4 w-4" />Secure operations workspace</span>
          <h1 className="text-5xl font-black leading-[1.08] tracking-tight text-white xl:text-6xl">Vận hành Ladux<br /><span className="text-indigo-400">từ một nơi.</span></h1>
          <p className="mt-6 max-w-lg text-base leading-7 text-slate-400">Theo dõi bán hàng, catalog, khách hàng và kho vận bằng dữ liệu trực tiếp từ hệ thống Ladux.</p>
        </div>
        <p className="relative text-xs text-slate-600">© 2026 LADUX · Internal system</p>
      </section>
      <section className="flex min-h-screen items-center justify-center bg-[#f7f8fb] px-5 py-10 sm:px-10">
        <div className="w-full max-w-md">
          <div className="mb-10 flex items-center gap-3 lg:hidden"><img src={laduxLogo} alt="LADUX" className="h-11 w-11 rounded-xl" /><p className="text-xl font-black tracking-[0.14em] text-slate-950">LADUX</p></div>
          <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-indigo-600">Admin Portal</p>
          <h2 className="mt-3 text-3xl font-black tracking-tight text-slate-950">Chào mừng trở lại</h2>
          <p className="mt-2 text-sm leading-6 text-slate-500">Đăng nhập bằng tài khoản có quyền ADMIN để tiếp tục.</p>

          <form onSubmit={onSubmit} className="mt-8 space-y-5" noValidate>
            {serverError && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{serverError}</div>}
            <div>
              <label htmlFor="admin-username" className="mb-2 block text-sm font-bold text-slate-700">Tên đăng nhập</label>
              <div className="relative"><UserRound className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" /><input id="admin-username" autoComplete="username" className={`${fieldClassName} pl-10`} placeholder="Nhập tên đăng nhập" {...form.register("username")} /></div>
              {form.formState.errors.username && <p className="mt-1.5 text-xs font-medium text-rose-600">{form.formState.errors.username.message}</p>}
            </div>
            <div>
              <div className="mb-2 flex items-center justify-between"><label htmlFor="admin-password" className="text-sm font-bold text-slate-700">Mật khẩu</label><span className="text-xs text-slate-400">HttpOnly Cookie</span></div>
              <div className="relative"><LockKeyhole className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" /><input id="admin-password" type={showPassword ? "text" : "password"} autoComplete="current-password" className={`${fieldClassName} px-10`} placeholder="Nhập mật khẩu" {...form.register("password")} /><button type="button" aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"} onClick={() => setShowPassword((value) => !value)} className="absolute right-2 top-1.5 flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700">{showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</button></div>
              {form.formState.errors.password && <p className="mt-1.5 text-xs font-medium text-rose-600">{form.formState.errors.password.message}</p>}
            </div>
            <AdminButton type="submit" className="w-full" disabled={isLoggingIn}>{isLoggingIn ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <ShieldCheck className="h-4 w-4" />}Đăng nhập quản trị<ArrowRight className="ml-auto h-4 w-4" /></AdminButton>
          </form>
          <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-4"><p className="text-xs font-bold text-slate-700">Bảo mật phiên</p><p className="mt-1 text-xs leading-5 text-slate-500">Ladux không lưu token trong trình duyệt. Phiên đăng nhập được bảo vệ bằng cookie HttpOnly.</p></div>
        </div>
      </section>
    </div>
  );
}
