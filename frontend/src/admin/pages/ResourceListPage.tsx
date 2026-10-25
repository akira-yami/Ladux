import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Edit3, LoaderCircle, Plus, Search, Trash2 } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../../app/components/ui/dialog";
import { adminApi } from "../api/adminApi";
import { AdminButton, AdminTable, ConfirmDialog, fieldClassName, PageHeader, PaginationBar, Panel, StatusBadge, type AdminColumn } from "../components/AdminUI";
import { adminQueryKeys } from "../queryKeys";
import { env } from "../../config/env";
import type { AdminRow, PageResponse, RoleResponse } from "../types";
import { formatBackendDateTime, formatCurrency, getApiErrorMessage, resolveImageUrl } from "../utils";

export type ResourceName = "brands" | "categories" | "coupons" | "customers" | "users" | "user-addresses" | "reviews" | "suppliers" | "order-histories" | "order-items" | "payments";

type FormField = { key: string; label: string; type?: "text" | "number" | "email" | "select" | "checkbox" | "date" | "datetime-local" | "roleIds"; options?: Array<{ value: string; label: string }>; placeholder?: string };

interface ResourceDefinition {
  title: string;
  description: string;
  searchPlaceholder?: string;
  statusOptions?: string[];
  readOnly?: boolean;
  columns: AdminColumn<AdminRow>[];
  fetcher: (params: { page: number; size: number; search: string; status: string }) => Promise<PageResponse<AdminRow>>;
  form?: {
    title: string;
    fields: FormField[];
    schema: z.ZodType<AdminRow>;
    defaults: AdminRow;
    normalize: (values: AdminRow) => AdminRow;
    submit: (id: number | null, values: AdminRow) => Promise<unknown>;
    remove: (id: number) => Promise<unknown>;
    name: (row: AdminRow) => string;
    canCreate?: boolean;
    canDelete?: boolean;
    uploadImage?: (file: File) => Promise<string>;
  };
}

function asAdminPage<T extends object>(page: PageResponse<T>): PageResponse<AdminRow> {
  return { ...page, content: page.content.map((item) => ({ ...item }) as AdminRow) };
}

function oneItemPage<T extends object>(item: T | null, page: number, size: number): PageResponse<AdminRow> {
  const content: AdminRow[] = item ? [({ ...item }) as AdminRow] : [];
  return { content, totalElements: content.length, totalPages: content.length ? 1 : 0, size, number: page, numberOfElements: content.length, first: true, last: true, empty: content.length === 0 };
}

const text = (row: AdminRow, key: string) => String(row[key] ?? "—");
const number = (row: AdminRow, key: string) => typeof row[key] === "number" ? row[key] as number : null;

function toDateInputValue(value: unknown): string {
  if (typeof value !== "string" || !value) return "";
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
  const backendMatch = value.match(/^(\d{2})-(\d{2})-(\d{4})/);
  if (backendMatch) return `${backendMatch[3]}-${backendMatch[2]}-${backendMatch[1]}`;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function dateInputToEndOfDayIso(value: unknown): string {
  const dateValue = typeof value === "string" ? value : "";
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateValue)) return dateValue;
  const [year, month, day] = dateValue.split("-").map(Number);
  return new Date(year, month - 1, day, 23, 59, 59, 999).toISOString();
}

function initialDialogValues(definition: NonNullable<ResourceDefinition["form"]>, row: AdminRow | null): AdminRow {
  const values = row ? { ...definition.defaults, ...row } : { ...definition.defaults };
  definition.fields.forEach((field) => {
    if (field.type === "date") {
      values[field.key] = toDateInputValue(values[field.key]);
    }
  });
  return values;
}

const activeColumn: AdminColumn<AdminRow> = { key: "active", header: "Trạng thái", render: (row) => <StatusBadge value={Boolean(row.isActive)} /> };
const createdColumn: AdminColumn<AdminRow> = { key: "created", header: "Ngày tạo", render: (row) => formatBackendDateTime(typeof row.createdAt === "string" ? row.createdAt : null) };

const fallbackRoleOptions: RoleResponse[] = [
  { id: 1, name: "ADMIN" },
  { id: 2, name: "CUSTOMER" },
];

function roleIdsFromNames(roles: unknown, options: RoleResponse[] = fallbackRoleOptions): number[] {
  if (!Array.isArray(roles)) return [2];
  const normalized = roles.map((role) => String(role).replace(/^ROLE_/, "").toUpperCase());
  return options.filter((option) => normalized.includes(option.name)).map((option) => option.id);
}

const definitions: Record<ResourceName, ResourceDefinition> = {
  brands: {
    title: "Thương hiệu", description: "Quản lý thương hiệu sử dụng public GET và admin mutation đúng contract backend.",
    columns: [
      { key: "id", header: "ID", render: (row) => <span className="font-bold text-slate-400">#{text(row, "id")}</span> },
      { key: "name", header: "Thương hiệu", render: (row) => <div className="flex items-center gap-3">{row.logoUrl ? <img src={resolveImageUrl(String(row.logoUrl), env.backendOrigin) ?? String(row.logoUrl)} alt="" className="h-9 w-9 rounded-lg border border-slate-200 object-contain" /> : <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-50 font-black text-indigo-600">{text(row, "name").slice(0, 1)}</span>}<span className="font-bold text-slate-900">{text(row, "name")}</span></div> },
      { key: "slug", header: "Slug", render: (row) => <code className="rounded bg-slate-100 px-2 py-1 text-xs text-slate-600">{text(row, "slug")}</code> },
    ],
    fetcher: (params) => adminApi.brands.list(params).then(asAdminPage),
    form: {
      title: "thương hiệu", fields: [{ key: "name", label: "Tên thương hiệu", placeholder: "Ví dụ: ASUS" }, { key: "logoUrl", label: "Logo URL", placeholder: "https://..." }],
      schema: z.object({ name: z.string().trim().min(1, "Tên là bắt buộc").max(100), logoUrl: z.string().trim().url("URL không hợp lệ").nullable().or(z.literal("")) }).passthrough(),
      defaults: { name: "", logoUrl: "" }, normalize: (values) => ({ name: String(values.name).trim(), logoUrl: String(values.logoUrl || "").trim() || null }),
      submit: (id, values) => id ? adminApi.brands.update(id, values as unknown as { name: string; logoUrl?: string | null }) : adminApi.brands.create(values as unknown as { name: string; logoUrl?: string | null }),
      remove: adminApi.brands.delete, name: (row) => text(row, "name"),
    },
  },
  categories: {
    title: "Danh mục", description: "Danh mục sản phẩm của catalog Ladux.",
    columns: [
      { key: "id", header: "ID", render: (row) => `#${text(row, "id")}` },
      { key: "name", header: "Danh mục", render: (row) => <div className="flex items-center gap-3">{row.imageUrl ? <img src={resolveImageUrl(String(row.imageUrl), env.backendOrigin) ?? String(row.imageUrl)} alt="" className="h-9 w-9 rounded-lg object-cover" /> : <span className="h-9 w-9 rounded-lg bg-slate-100" />}<div><p className="font-bold text-slate-900">{text(row, "name")}</p><p className="text-xs text-slate-400">{text(row, "slug")}</p></div></div> },
    ],
    fetcher: (params) => adminApi.categories.list(params).then(asAdminPage),
    form: {
      title: "danh mục", fields: [{ key: "name", label: "Tên danh mục" }, { key: "imageUrl", label: "URL ảnh" }],
      schema: z.object({ name: z.string().trim().min(1).max(100), imageUrl: z.string().trim().max(500).optional().nullable() }).passthrough(),
      defaults: { name: "", imageUrl: "" }, normalize: (values) => ({ name: String(values.name).trim(), imageUrl: String(values.imageUrl || "").trim() || null }),
      submit: (id, values) => id ? adminApi.categories.update(id, values as unknown as { name: string; imageUrl?: string | null }) : adminApi.categories.create(values as unknown as { name: string; imageUrl?: string | null }),
      remove: adminApi.categories.delete, name: (row) => text(row, "name"),
      uploadImage: (file) => adminApi.categories.uploadImage(file).then((response) => response.url),
    },
  },
  coupons: {
    title: "Mã giảm giá", description: "Theo dõi hiệu lực, hạn dùng và mức sử dụng coupon.", searchPlaceholder: "Nhập chính xác mã coupon...",
    columns: [
      { key: "code", header: "Mã", render: (row) => <code className="rounded-lg bg-indigo-50 px-2.5 py-1.5 font-bold text-indigo-700">{text(row, "code")}</code> },
      { key: "discount", header: "Ưu đãi", render: (row) => row.discountType === "PERCENT" ? `${text(row, "discountValue")}%` : formatCurrency(number(row, "discountValue")) },
      { key: "minimum", header: "Đơn tối thiểu", render: (row) => formatCurrency(number(row, "minOrderValue")) },
      { key: "usage", header: "Đã dùng", render: (row) => `${text(row, "usedCount")} / ${row.usageLimit ?? "∞"}` },
      { key: "expires", header: "Hết hạn", render: (row) => formatBackendDateTime(typeof row.expiresAt === "string" ? row.expiresAt : null) },
    ],
    fetcher: async (params) => params.search ? adminApi.coupons.byCode(params.search).then((item) => oneItemPage(item, params.page, params.size)).catch(() => oneItemPage(null, params.page, params.size)) : adminApi.coupons.list(params).then(asAdminPage),
    form: {
      title: "mã giảm giá", fields: [
        { key: "code", label: "Mã coupon" },
        { key: "discountType", label: "Loại giảm", type: "select", options: [{ value: "PERCENT", label: "Phần trăm" }, { value: "FIXED_AMOUNT", label: "Số tiền cố định" }] },
        { key: "discountValue", label: "Giá trị giảm", type: "number" }, { key: "minOrderValue", label: "Đơn tối thiểu", type: "number" },
        { key: "usageLimit", label: "Giới hạn lượt dùng", type: "number" }, { key: "expiresAt", label: "Ngày hết hạn", type: "date" },
      ],
      schema: z.object({ code: z.string().trim().min(1).max(50), discountType: z.enum(["PERCENT", "FIXED_AMOUNT"]), discountValue: z.coerce.number().positive(), minOrderValue: z.union([z.coerce.number().nonnegative(), z.literal("")]), usageLimit: z.union([z.coerce.number().positive(), z.literal("")]), expiresAt: z.string().min(1) }).passthrough(),
      defaults: { code: "", discountType: "PERCENT", discountValue: "", minOrderValue: "", usageLimit: "", expiresAt: "" },
      normalize: (values) => ({ code: String(values.code).trim().toUpperCase(), discountType: values.discountType, discountValue: Number(values.discountValue), minOrderValue: values.minOrderValue === "" ? null : Number(values.minOrderValue), usageLimit: values.usageLimit === "" ? null : Number(values.usageLimit), expiresAt: dateInputToEndOfDayIso(values.expiresAt) }),
      submit: (id, values) => id ? adminApi.coupons.update(id, values as never) : adminApi.coupons.create(values as never), remove: adminApi.coupons.delete, name: (row) => text(row, "code"),
    },
  },
  customers: {
    title: "Khách hàng", description: "Dữ liệu CRM, hạng thành viên và giá trị tích lũy.", searchPlaceholder: "Tìm theo tên hoặc số điện thoại...", statusOptions: ["BROWSER", "SILVER", "GOLD", "RUBY"],
    columns: [
      { key: "customer", header: "Khách hàng", render: (row) => <div><Link to={`/admin/customers/${text(row, "id")}`} className="font-bold text-slate-900 hover:text-indigo-600">{text(row, "fullName")}</Link><p className="mt-1 text-xs text-slate-400">@{text(row, "username")} · {text(row, "email")}</p></div> },
      { key: "phone", header: "Điện thoại", render: (row) => text(row, "phone") },
      { key: "level", header: "Hạng", render: (row) => <StatusBadge value={text(row, "level")} /> },
      { key: "points", header: "Điểm", render: (row) => number(row, "loyaltyPoints")?.toLocaleString("vi-VN") ?? "—" },
      { key: "spent", header: "Tổng chi tiêu", render: (row) => <strong>{formatCurrency(number(row, "totalSpent"))}</strong> },
    ],
    fetcher: (params) => params.status ? adminApi.customers.byLevel(params.status as never, params).then(asAdminPage) : params.search ? adminApi.customers.search(params.search, params.search, params).then(asAdminPage) : adminApi.customers.list(params).then(asAdminPage),
  },
  users: {
    title: "Người dùng", description: "Quản lý trạng thái tài khoản. Backend khóa user bằng isActive và tự revoke phiên đăng nhập.", searchPlaceholder: "Tìm theo tên hoặc số điện thoại...", statusOptions: ["ACTIVE", "LOCKED"],
    columns: [
      { key: "user", header: "Người dùng", render: (row) => <div><Link to={`/admin/users/${text(row, "id")}`} className="font-bold text-slate-900 hover:text-indigo-600">{text(row, "fullName")}</Link><p className="text-xs text-slate-400">@{text(row, "username")} · {text(row, "email")}</p></div> },
      { key: "phone", header: "Điện thoại", render: (row) => text(row, "phone") },
      { key: "roles", header: "Vai trò", render: (row) => <div className="flex flex-wrap gap-1">{Array.isArray(row.roles) ? row.roles.map((role) => <StatusBadge key={String(role)} value={String(role)} />) : "—"}</div> }, activeColumn,
    ],
    fetcher: async (params) => {
      if (params.search) {
        const response = await adminApi.users.search(params.search, params.search, params).then(asAdminPage);
        const content = params.status === "ACTIVE"
          ? response.content.filter((user) => user.isActive)
          : params.status === "LOCKED"
            ? response.content.filter((user) => user.isActive === false)
            : response.content;
        return { ...response, content, totalElements: content.length, numberOfElements: content.length, empty: content.length === 0 };
      }
      if (params.status === "ACTIVE") return adminApi.users.active(params).then(asAdminPage);
      if (params.status === "LOCKED") {
        const response = await adminApi.users.list(params).then(asAdminPage);
        const content = response.content.filter((user) => user.isActive === false);
        return { ...response, content, totalElements: content.length, numberOfElements: content.length, empty: content.length === 0 };
      }
      return adminApi.users.list(params).then(asAdminPage);
    },
    form: {
      title: "người dùng",
      fields: [
        { key: "fullName", label: "Họ tên" },
        { key: "avatar", label: "Avatar URL" },
        { key: "isActive", label: "Tài khoản đang hoạt động", type: "checkbox" },
        { key: "roleIds", label: "Vai trò", type: "roleIds" },
      ],
      schema: z.object({
        fullName: z.union([z.string().max(150), z.literal(""), z.null()]).optional(),
        avatar: z.union([z.string().max(255), z.literal(""), z.null()]).optional(),
        isActive: z.boolean(),
        roleIds: z.array(z.number().positive()).min(1, "User phải có ít nhất một vai trò"),
      }).passthrough(),
      defaults: { fullName: "", avatar: "", isActive: true, roleIds: [2] },
      normalize: (values) => ({
        fullName: String(values.fullName || "").trim() || null,
        avatar: String(values.avatar || "").trim() || null,
        isActive: Boolean(values.isActive),
        roleIds: Array.isArray(values.roleIds) ? values.roleIds.map(Number) : [],
      }),
      submit: (id, values) => id ? adminApi.users.update(id, values as never) : Promise.reject(new Error("Backend chưa có API tạo user")),
      remove: adminApi.users.delete,
      name: (row) => text(row, "username"),
      canCreate: false,
      canDelete: false,
    },
  },
  "user-addresses": {
    title: "Địa chỉ người dùng", description: "Dữ liệu chỉ đọc vì backend chưa hỗ trợ admin chỉnh sửa hoặc xóa địa chỉ.", readOnly: true,
    columns: [
      { key: "receiver", header: "Người nhận", render: (row) => <div><p className="font-bold text-slate-900">{text(row, "receiverName")}</p><p className="text-xs text-slate-400">User #{text(row, "userId")} · {text(row, "phone")}</p></div> },
      { key: "address", header: "Địa chỉ", render: (row) => <span className="max-w-xl leading-6">{[row.street, row.ward, row.district, row.city].filter(Boolean).join(", ")}</span> },
      { key: "default", header: "Mặc định", render: (row) => row.isDefault ? <StatusBadge value="TRUE" /> : "—" },
    ], fetcher: (params) => adminApi.userAddresses.list(params).then(asAdminPage),
  },
  reviews: {
    title: "Đánh giá", description: "Audit phản hồi khách hàng. Backend chưa có endpoint duyệt, ẩn hoặc xóa.", searchPlaceholder: "Tìm theo tên người dùng...", readOnly: true,
    columns: [
      { key: "reviewer", header: "Người đánh giá", render: (row) => <span className="font-bold text-slate-900">{text(row, "reviewerName")}</span> },
      { key: "rating", header: "Điểm", render: (row) => <span className="font-bold text-amber-500">{"★".repeat(Math.max(0, Math.min(5, number(row, "rating") ?? 0)))}</span> },
      { key: "comment", header: "Nội dung", render: (row) => <p className="max-w-xl whitespace-normal leading-6">{text(row, "comment")}</p> }, createdColumn,
    ], fetcher: (params) => params.search
      ? /^\d+$/.test(params.search) ? adminApi.reviews.byUser(Number(params.search), params).then(asAdminPage) : adminApi.reviews.search(params.search, params).then(asAdminPage)
      : adminApi.reviews.list(params).then(asAdminPage),
  },
  suppliers: {
    title: "Nhà cung cấp", description: "Danh bạ và trạng thái đối tác cung ứng của Ladux.", searchPlaceholder: "Tìm theo tên hoặc số điện thoại...", statusOptions: ["ACTIVE"],
    columns: [
      { key: "supplier", header: "Nhà cung cấp", render: (row) => <div><Link to={`/admin/suppliers/${text(row, "id")}`} className="font-bold text-slate-900 hover:text-indigo-600">{text(row, "name")}</Link><p className="text-xs text-slate-400">{text(row, "email")}</p></div> },
      { key: "phone", header: "Điện thoại", render: (row) => text(row, "phone") },
      { key: "address", header: "Địa chỉ", render: (row) => <span className="max-w-sm whitespace-normal">{text(row, "address")}</span> }, activeColumn, createdColumn,
    ], fetcher: (params) => params.status === "ACTIVE" ? adminApi.suppliers.active(params).then(asAdminPage) : params.search ? adminApi.suppliers.search(params.search, params.search, params).then(asAdminPage) : adminApi.suppliers.list(params).then(asAdminPage),
    form: {
      title: "nhà cung cấp", fields: [{ key: "name", label: "Tên nhà cung cấp" }, { key: "address", label: "Địa chỉ" }, { key: "phone", label: "Điện thoại" }, { key: "email", label: "Email", type: "email" }, { key: "isActive", label: "Đang hoạt động", type: "checkbox" }],
      schema: z.object({ name: z.string().trim().min(1).max(150), address: z.string().max(255).optional(), phone: z.string().max(20).optional(), email: z.union([z.string().email(), z.literal("")]), isActive: z.boolean() }).passthrough(),
      defaults: { name: "", address: "", phone: "", email: "", isActive: true }, normalize: (values) => ({ name: String(values.name).trim(), address: String(values.address || "").trim() || null, phone: String(values.phone || "").trim() || null, email: String(values.email || "").trim() || null, isActive: Boolean(values.isActive) }),
      submit: (id, values) => id ? adminApi.suppliers.update(id, values as never) : adminApi.suppliers.create(values as never), remove: adminApi.suppliers.delete, name: (row) => text(row, "name"),
    },
  },
  "order-histories": {
    title: "Lịch sử đơn hàng", description: "Audit log chỉ đọc của toàn bộ thay đổi trạng thái đơn.", searchPlaceholder: "Lọc theo Order ID...", readOnly: true,
    columns: [{ key: "id", header: "ID", render: (row) => `#${text(row, "id")}` }, { key: "order", header: "Đơn hàng", render: (row) => <strong>#{text(row, "orderId")}</strong> }, { key: "status", header: "Trạng thái", render: (row) => <StatusBadge value={text(row, "status")} /> }, { key: "description", header: "Mô tả", render: (row) => text(row, "description") }, createdColumn],
    fetcher: (params) => params.search && Number(params.search) > 0 ? adminApi.orderHistories.byOrder(Number(params.search), params).then(asAdminPage) : adminApi.orderHistories.list(params).then(asAdminPage),
  },
  "order-items": {
    title: "Dòng sản phẩm trong đơn", description: "Dữ liệu chỉ đọc, giữ nguyên giá tại thời điểm mua.", searchPlaceholder: "Lọc theo Order ID...", readOnly: true,
    columns: [
      { key: "id", header: "ID", render: (row) => `#${text(row, "id")}` }, { key: "order", header: "Đơn", render: (row) => <strong>#{text(row, "orderId")}</strong> },
      { key: "product", header: "Sản phẩm", render: (row) => { const product = row.product as { name?: string } | null; return product?.name ?? "—"; } },
      { key: "variant", header: "Variant ID", render: (row) => `#${text(row, "productVariantId")}` }, { key: "quantity", header: "SL", render: (row) => text(row, "quantity") },
      { key: "price", header: "Đơn giá", render: (row) => formatCurrency(number(row, "priceAtPurchase")) },
      { key: "total", header: "Thành tiền", render: (row) => <strong>{formatCurrency((number(row, "priceAtPurchase") ?? 0) * (number(row, "quantity") ?? 0))}</strong> },
    ], fetcher: (params) => params.search && Number(params.search) > 0 ? adminApi.orderItems.byOrder(Number(params.search), params).then(asAdminPage) : adminApi.orderItems.list(params).then(asAdminPage),
  },
  payments: {
    title: "Thanh toán", description: "Theo dõi giao dịch và chỉ cập nhật các payment đang PENDING.", statusOptions: ["PENDING", "SUCCESS", "FAILED", "REFUNDED"], readOnly: true,
    columns: [
      { key: "id", header: "Giao dịch", render: (row) => <strong>#{text(row, "id")}</strong> }, { key: "order", header: "Đơn hàng", render: (row) => `#${text(row, "orderId")}` },
      { key: "provider", header: "Cổng", render: (row) => text(row, "provider") }, { key: "transaction", header: "Mã giao dịch", render: (row) => text(row, "transactionNo") },
      { key: "amount", header: "Số tiền", render: (row) => <strong>{formatCurrency(number(row, "amount"))}</strong> }, { key: "status", header: "Trạng thái", render: (row) => <StatusBadge value={text(row, "status")} /> }, createdColumn,
    ], fetcher: (params) => params.status ? adminApi.payments.byStatus(params.status as never, params).then(asAdminPage) : adminApi.payments.list({ page: params.page, size: params.size }).then(asAdminPage),
  },
};

function EntityDialog({ definition, row, open, onOpenChange, onSaved }: { definition: NonNullable<ResourceDefinition["form"]>; row: AdminRow | null; open: boolean; onOpenChange: (open: boolean) => void; onSaved: () => void }) {
  const hasRoleField = definition.fields.some((field) => field.type === "roleIds");
  const rolesQuery = useQuery({ queryKey: adminQueryKeys.resource("roles-lookup", {}), queryFn: () => adminApi.roles.list(), enabled: hasRoleField, staleTime: 5 * 60_000 });
  const [values, setValues] = useState<AdminRow>(() => {
    const initial = initialDialogValues(definition, row);
    if (definition.title === "người dùng") initial.roleIds = roleIdsFromNames(row?.roles);
    return initial;
  });
  const [error, setError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const mutation = useMutation({
    mutationFn: () => {
      const normalized = definition.normalize(values);
      const parsed = definition.schema.safeParse(normalized);
      if (!parsed.success) throw new Error(parsed.error.issues[0]?.message ?? "Dữ liệu chưa hợp lệ");
      return definition.submit(typeof row?.id === "number" ? row.id : null, parsed.data);
    },
    onSuccess: () => { toast.success(row ? "Cập nhật thành công" : "Tạo mới thành công"); onSaved(); onOpenChange(false); },
    onError: (mutationError) => setError(getApiErrorMessage(mutationError)),
  });

  const submit = (event: FormEvent) => { event.preventDefault(); setError(null); mutation.mutate(); };
  const availableRoleOptions = rolesQuery.data ?? fallbackRoleOptions;

  useEffect(() => {
    if (!hasRoleField || !row || !rolesQuery.data) return;
    setValues((current) => ({ ...current, roleIds: roleIdsFromNames(row.roles, rolesQuery.data) }));
  }, [hasRoleField, row, rolesQuery.data]);

  return <Dialog open={open} onOpenChange={onOpenChange}><DialogContent className="border-slate-200 bg-white text-slate-950 sm:max-w-xl"><DialogHeader><DialogTitle>{row ? "Cập nhật" : "Tạo"} {definition.title}</DialogTitle><DialogDescription className="text-slate-500">Dữ liệu sẽ được gửi trực tiếp tới backend Ladux.</DialogDescription></DialogHeader><form onSubmit={submit} className="space-y-4">{error && <div role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}{definition.fields.map((field) => <div key={field.key}>{field.type === "checkbox" ? <label className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm font-semibold text-slate-700"><input type="checkbox" checked={Boolean(values[field.key])} onChange={(event) => setValues((current) => ({ ...current, [field.key]: event.target.checked }))} />{field.label}</label> : field.type === "roleIds" ? <div><p className="mb-1.5 text-sm font-bold text-slate-700">{field.label}</p><div className="grid gap-2 sm:grid-cols-2">{availableRoleOptions.map((role) => { const selected = Array.isArray(values.roleIds) && values.roleIds.map(Number).includes(role.id); return <label key={role.id} className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm font-semibold text-slate-700"><input type="checkbox" checked={selected} onChange={(event) => setValues((current) => { const currentIds = Array.isArray(current.roleIds) ? current.roleIds.map(Number) : []; return { ...current, roleIds: event.target.checked ? Array.from(new Set([...currentIds, role.id])) : currentIds.filter((id) => id !== role.id) }; })} />{role.name}</label>; })}</div></div> : <><label htmlFor={`field-${field.key}`} className="mb-1.5 block text-sm font-bold text-slate-700">{field.label}</label>{field.type === "select" ? <select id={`field-${field.key}`} className={fieldClassName} value={String(values[field.key] ?? "")} onChange={(event) => setValues((current) => ({ ...current, [field.key]: event.target.value }))}>{field.options?.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select> : <input id={`field-${field.key}`} className={fieldClassName} type={field.type ?? "text"} placeholder={field.placeholder} value={String(values[field.key] ?? "")} onChange={(event) => setValues((current) => ({ ...current, [field.key]: event.target.value }))} />}{["logoUrl", "imageUrl", "avatar"].includes(field.key) && values[field.key] && <img src={resolveImageUrl(String(values[field.key]), env.backendOrigin) ?? String(values[field.key])} alt="Xem trước" className="mt-3 h-20 w-20 rounded-xl border border-slate-200 object-cover" />}</>}</div>)}{definition.uploadImage && <div><label className="mb-1.5 block text-sm font-bold text-slate-700">Upload ảnh danh mục</label><input className={fieldClassName} type="file" accept="image/*" disabled={isUploading} onChange={async (event) => { const file = event.target.files?.[0]; if (!file) return; if (!file.type.startsWith("image/") || file.size > 5 * 1024 * 1024) { setError("Ảnh phải đúng MIME image và không quá 5 MB"); return; } setIsUploading(true); setError(null); try { const url = await definition.uploadImage!(file); setValues((current) => ({ ...current, imageUrl: url })); toast.success("Đã upload ảnh danh mục"); } catch (uploadError) { setError(getApiErrorMessage(uploadError)); } finally { setIsUploading(false); } }} /></div>}<DialogFooter><AdminButton type="button" tone="secondary" onClick={() => onOpenChange(false)}>Hủy</AdminButton><AdminButton type="submit" disabled={mutation.isPending || isUploading}>{(mutation.isPending || isUploading) && <LoaderCircle className="h-4 w-4 animate-spin" />}Lưu thay đổi</AdminButton></DialogFooter></form></DialogContent></Dialog>;
}

export default function ResourceListPage({ resource }: { resource: ResourceName }) {
  const definition = definitions[resource];
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const [searchDraft, setSearchDraft] = useState(params.get("search") ?? "");
  const [editingRow, setEditingRow] = useState<AdminRow | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deletingRow, setDeletingRow] = useState<AdminRow | null>(null);
  const page = Math.max(0, Number(params.get("page") ?? 0));
  const size = [10, 20, 50, 100].includes(Number(params.get("size"))) ? Number(params.get("size")) : 20;
  const search = params.get("search") ?? "";
  const status = params.get("status") ?? "";
  const queryParams = useMemo(() => ({ page, size, search, status }), [page, size, search, status]);
  const query = useQuery({ queryKey: adminQueryKeys.resource(resource, queryParams), queryFn: () => definition.fetcher(queryParams), placeholderData: (previous) => previous });

  const deleteMutation = useMutation({
    mutationFn: () => definition.form!.remove(Number(deletingRow?.id)),
    onSuccess: () => { toast.success("Đã xóa dữ liệu"); setDeletingRow(null); queryClient.invalidateQueries({ queryKey: ["admin"] }); },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  const columns = useMemo(() => definition.form ? [...definition.columns, { key: "actions", header: "Thao tác", className: "sticky right-0 bg-white", render: (row: AdminRow): ReactNode => <div className="flex justify-end gap-1"><AdminButton tone="ghost" size="icon" aria-label={`Sửa ${definition.form!.name(row)}`} onClick={() => { setEditingRow(row); setDialogOpen(true); }}><Edit3 className="h-4 w-4" /></AdminButton>{definition.form!.canDelete !== false && <AdminButton tone="ghost" size="icon" aria-label={`Xóa ${definition.form!.name(row)}`} className="text-rose-600 hover:bg-rose-50 hover:text-rose-700" onClick={() => setDeletingRow(row)}><Trash2 className="h-4 w-4" /></AdminButton>}</div> }] : definition.columns, [definition]);

  const updateParam = (key: string, value: string) => { const next = new URLSearchParams(params); value ? next.set(key, value) : next.delete(key); if (key !== "page") next.set("page", "0"); setParams(next); };

  return <>
    <PageHeader title={definition.title} description={definition.description} actions={definition.form && definition.form.canCreate !== false && <AdminButton onClick={() => { setEditingRow(null); setDialogOpen(true); }}><Plus className="h-4 w-4" />Tạo mới</AdminButton>} />
    <Panel>
      {(definition.searchPlaceholder || definition.statusOptions) && <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center"><form className="relative flex-1" onSubmit={(event) => { event.preventDefault(); updateParam("search", searchDraft.trim()); }}><Search className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-slate-400" /><input className={`${fieldClassName} pl-10`} value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} placeholder={definition.searchPlaceholder ?? "Tìm kiếm..."} /><button className="sr-only">Tìm</button></form>{definition.statusOptions && <select aria-label="Lọc trạng thái" className={`${fieldClassName} sm:w-52`} value={status} onChange={(event) => updateParam("status", event.target.value)}><option value="">Tất cả trạng thái</option>{definition.statusOptions.map((option) => <option key={option} value={option}>{option}</option>)}</select>}</div>}
      <AdminTable rows={query.data?.content ?? []} columns={columns} isLoading={query.isLoading} error={query.isError ? getApiErrorMessage(query.error) : null} onRetry={() => query.refetch()} />
      <PaginationBar page={page} totalPages={query.data?.totalPages ?? 0} totalElements={query.data?.totalElements ?? 0} size={size} onPageChange={(value) => updateParam("page", String(value))} onSizeChange={(value) => updateParam("size", String(value))} />
    </Panel>
    {definition.form && dialogOpen && <EntityDialog key={`${editingRow?.id ?? "new"}-${dialogOpen}`} definition={definition.form} row={editingRow} open={dialogOpen} onOpenChange={setDialogOpen} onSaved={() => queryClient.invalidateQueries({ queryKey: ["admin"] })} />}
    {definition.form && definition.form.canDelete !== false && <ConfirmDialog open={Boolean(deletingRow)} onOpenChange={(open) => !open && setDeletingRow(null)} title={`Xóa ${definition.form.title}?`} description={deletingRow ? `Bạn sắp xóa “${definition.form.name(deletingRow)}”. Thao tác này không thể hoàn tác.` : ""} confirmLabel="Xóa" isPending={deleteMutation.isPending} onConfirm={() => deleteMutation.mutate()} />}
  </>;
}
