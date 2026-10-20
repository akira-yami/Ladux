import type { ButtonHTMLAttributes, ReactNode } from "react";
import { AlertCircle, ChevronLeft, ChevronRight, Inbox, LoaderCircle, RefreshCw } from "lucide-react";
import { cn } from "../../app/components/ui/utils";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../../app/components/ui/dialog";

export function AdminButton({
  tone = "primary",
  size = "md",
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  tone?: "primary" | "secondary" | "danger" | "ghost" | "warning";
  size?: "sm" | "md" | "icon";
}) {
  return (
    <button
      className={cn(
        "inline-flex min-h-10 items-center justify-center gap-2 rounded-xl font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
        size === "sm" && "min-h-9 px-3 text-xs",
        size === "md" && "px-4 text-sm",
        size === "icon" && "h-10 w-10 p-0",
        tone === "primary" && "bg-indigo-600 text-white shadow-sm shadow-indigo-200 hover:bg-primary-hover",
        tone === "secondary" && "border border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50",
        tone === "danger" && "bg-rose-600 text-white hover:bg-rose-700",
        tone === "ghost" && "text-slate-600 hover:bg-slate-100 hover:text-slate-950",
        tone === "warning" && "border border-amber-300 bg-amber-50 text-amber-800 hover:bg-amber-100 hover:border-amber-400 shadow-sm",
        className,
      )}
      {...props}
    />
  );
}

export function PageHeader({ title, description, actions }: { title: string; description: string; actions?: ReactNode }) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-indigo-600">Ladux Operations</p>
        <h1 className="text-2xl font-extrabold tracking-tight text-slate-950 sm:text-3xl">{title}</h1>
        <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">{description}</p>
      </div>
      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Panel({ children, className }: { children: ReactNode; className?: string }) {
  return <section className={cn("overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_8px_30px_rgba(15,23,42,0.04)]", className)}>{children}</section>;
}

export function StatusBadge({ value }: { value: string | boolean | null | undefined }) {
  const normalized = String(value ?? "UNKNOWN").toUpperCase();
  const positive = ["ACTIVE", "SUCCESS", "DELIVERED", "RECEIVED", "READ", "TRUE", "CONFIRMED"].includes(normalized);
  const warning = ["PENDING", "SHIPPED", "PARTIALLY_RECEIVED", "RETURN_REQUESTED"].includes(normalized);
  const danger = ["FAILED", "CANCELLED", "REFUNDED", "RETURNED", "FALSE", "INACTIVE"].includes(normalized);
  return (
    <span className={cn(
      "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-bold tracking-wide",
      positive && "border-emerald-200 bg-emerald-50 text-emerald-700",
      warning && "border-amber-200 bg-amber-50 text-amber-700",
      danger && "border-rose-200 bg-rose-50 text-rose-700",
      !positive && !warning && !danger && "border-slate-200 bg-slate-50 text-slate-600",
    )}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {normalized}
    </span>
  );
}

export interface AdminColumn<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
}

export function AdminTable<T>({
  rows,
  columns,
  isLoading,
  error,
  onRetry,
  emptyTitle = "Chưa có dữ liệu",
  emptyDescription = "Dữ liệu sẽ xuất hiện tại đây khi backend có bản ghi.",
}: {
  rows: T[];
  columns: AdminColumn<T>[];
  isLoading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  emptyTitle?: string;
  emptyDescription?: string;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px] border-collapse text-left">
        <thead>
          <tr className="border-b border-slate-200 bg-slate-50/80">
            {columns.map((column) => (
              <th key={column.key} className={cn("px-5 py-3.5 text-[11px] font-extrabold uppercase tracking-[0.12em] text-slate-500", column.className)}>{column.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {isLoading && Array.from({ length: 6 }).map((_, index) => (
            <tr key={index} className="border-b border-slate-100">
              {columns.map((column) => <td key={column.key} className="px-5 py-4"><div className="h-4 animate-pulse rounded bg-slate-100" /></td>)}
            </tr>
          ))}
          {!isLoading && !error && rows.map((row, rowIndex) => (
            <tr key={rowIndex} className="border-b border-slate-100 transition hover:bg-indigo-50/30 last:border-0">
              {columns.map((column) => <td key={column.key} className={cn("px-5 py-4 text-sm text-slate-700", column.className)}>{column.render(row)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
      {!isLoading && error && (
        <div className="flex min-h-64 flex-col items-center justify-center px-6 text-center" role="alert">
          <span className="rounded-2xl bg-rose-50 p-3 text-rose-600"><AlertCircle className="h-6 w-6" /></span>
          <h3 className="mt-4 font-bold text-slate-900">Không thể tải dữ liệu</h3>
          <p className="mt-1 max-w-md text-sm text-slate-500">{error}</p>
          {onRetry && <AdminButton tone="secondary" className="mt-4" onClick={onRetry}><RefreshCw className="h-4 w-4" />Thử lại</AdminButton>}
        </div>
      )}
      {!isLoading && !error && rows.length === 0 && (
        <div className="flex min-h-64 flex-col items-center justify-center px-6 text-center">
          <span className="rounded-2xl bg-slate-100 p-3 text-slate-500"><Inbox className="h-6 w-6" /></span>
          <h3 className="mt-4 font-bold text-slate-900">{emptyTitle}</h3>
          <p className="mt-1 text-sm text-slate-500">{emptyDescription}</p>
        </div>
      )}
    </div>
  );
}

export function PaginationBar({ page, totalPages, totalElements, size, onPageChange, onSizeChange }: {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}) {
  return (
    <div className="flex flex-col gap-3 border-t border-slate-200 px-5 py-4 text-sm sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-3 text-slate-500">
        <span>{totalElements.toLocaleString("vi-VN")} bản ghi</span>
        <select aria-label="Số dòng mỗi trang" className="h-9 rounded-lg border border-slate-200 bg-white px-2 text-slate-700" value={size} onChange={(event) => onSizeChange(Number(event.target.value))}>
          {[10, 20, 50, 100].map((value) => <option key={value} value={value}>{value}/trang</option>)}
        </select>
      </div>
      <div className="flex items-center gap-2">
        <AdminButton tone="secondary" size="icon" aria-label="Trang trước" disabled={page <= 0} onClick={() => onPageChange(page - 1)}><ChevronLeft className="h-4 w-4" /></AdminButton>
        <span className="min-w-28 text-center font-semibold text-slate-700">Trang {Math.min(page + 1, Math.max(totalPages, 1))} / {Math.max(totalPages, 1)}</span>
        <AdminButton tone="secondary" size="icon" aria-label="Trang sau" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}><ChevronRight className="h-4 w-4" /></AdminButton>
      </div>
    </div>
  );
}

export function LoadingScreen({ label = "Đang kiểm tra phiên quản trị..." }: { label?: string }) {
  return <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 text-slate-600"><LoaderCircle className="mb-4 h-8 w-8 animate-spin text-indigo-600" /><p className="text-sm font-semibold">{label}</p></div>;
}

export function ConfirmDialog({ open, onOpenChange, title, description, confirmLabel = "Xác nhận", isPending, onConfirm, danger = true, confirmDisabled = false, children }: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel?: string;
  isPending?: boolean;
  onConfirm: () => void;
  danger?: boolean;
  confirmDisabled?: boolean;
  children?: ReactNode;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="border-slate-200 bg-white text-slate-950 sm:max-w-md">
        <DialogHeader><DialogTitle>{title}</DialogTitle><DialogDescription className="text-slate-500">{description}</DialogDescription></DialogHeader>
        {children}
        <DialogFooter>
          <AdminButton tone="secondary" onClick={() => onOpenChange(false)}>Đóng</AdminButton>
          <AdminButton tone={danger ? "danger" : "primary"} disabled={isPending || confirmDisabled} onClick={onConfirm}>{isPending && <LoaderCircle className="h-4 w-4 animate-spin" />}{confirmLabel}</AdminButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export const fieldClassName = "h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100";
export const textareaClassName = "min-h-28 w-full resize-y rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100";
