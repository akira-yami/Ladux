import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Edit3, Eye, Plus, Search, Trash2 } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { env } from "../../config/env";
import { adminApi } from "../api/adminApi";
import { AdminButton, AdminTable, ConfirmDialog, fieldClassName, PageHeader, PaginationBar, Panel, StatusBadge, type AdminColumn } from "../components/AdminUI";
import { adminQueryKeys } from "../queryKeys";
import type { ProductResponse } from "../types";
import { formatCurrency, getApiErrorMessage, resolveImageUrl } from "../utils";

export default function ProductsPage() {
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const [searchDraft, setSearchDraft] = useState(params.get("search") ?? "");
  const [deleting, setDeleting] = useState<ProductResponse | null>(null);
  const page = Math.max(0, Number(params.get("page") ?? 0));
  const size = [10, 20, 50, 100].includes(Number(params.get("size"))) ? Number(params.get("size")) : 20;
  const search = params.get("search") ?? "";
  const brandId = Number(params.get("brand") ?? 0);
  const categoryId = Number(params.get("category") ?? 0);
  const queryParams = useMemo(() => ({ page, size, search, brandId, categoryId }), [page, size, search, brandId, categoryId]);

  const productsQuery = useQuery({
    queryKey: adminQueryKeys.resource("products", queryParams),
    queryFn: () => brandId ? adminApi.products.byBrand(brandId, { page, size }) : categoryId ? adminApi.products.byCategory(categoryId, { page, size }) : adminApi.products.list({ page, size, search: search || undefined, sort: "createdAt,desc" }),
    placeholderData: (previous) => previous,
  });
  const brandsQuery = useQuery({ queryKey: adminQueryKeys.resource("brands-lookup", {}), queryFn: () => adminApi.brands.list({ page: 0, size: 100 }), staleTime: 5 * 60_000 });
  const categoriesQuery = useQuery({ queryKey: adminQueryKeys.resource("categories-lookup", {}), queryFn: () => adminApi.categories.list({ page: 0, size: 100 }), staleTime: 5 * 60_000 });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminApi.products.delete(id),
    onSuccess: () => { toast.success("Đã xóa sản phẩm"); setDeleting(null); queryClient.invalidateQueries({ queryKey: adminQueryKeys.products }); queryClient.invalidateQueries({ queryKey: ["admin", "products"] }); },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  const updateParam = (key: string, value: string) => { const next = new URLSearchParams(params); value ? next.set(key, value) : next.delete(key); if (key !== "page") next.set("page", "0"); if (key === "brand" && value) next.delete("category"); if (key === "category" && value) next.delete("brand"); setParams(next); };

  const columns: AdminColumn<ProductResponse>[] = [
    { key: "product", header: "Sản phẩm", render: (product) => { const image = resolveImageUrl(product.images?.[0]?.imageUrl, env.backendOrigin); return <div className="flex min-w-64 items-center gap-3">{image ? <img src={image} alt="" className="h-12 w-12 rounded-xl border border-slate-200 object-cover" /> : <span className="h-12 w-12 rounded-xl bg-slate-100" />}<div><Link to={`/admin/products/${product.id}`} className="line-clamp-1 font-extrabold text-slate-900 hover:text-indigo-600">{product.name}</Link><p className="mt-1 text-xs text-slate-400">#{product.id} · {product.brand?.name ?? "Chưa có brand"}</p></div></div>; } },
    { key: "category", header: "Danh mục", render: (product) => product.category?.name ?? "—" },
    { key: "variants", header: "Cấu hình", render: (product) => <span className="font-bold">{product.variants?.length ?? 0}</span> },
    { key: "stock", header: "Tồn kho", render: (product) => { const stocks = (product.variants ?? []).map((variant) => variant.stockQuantity); const total = stocks.reduce((sum, value) => sum + value, 0); return <div><p className="font-bold text-slate-900">{total.toLocaleString("vi-VN")}</p><p className="text-xs text-slate-400">Thấp nhất: {stocks.length ? Math.min(...stocks) : "—"}</p></div>; } },
    { key: "price", header: "Giá từ", render: (product) => { const prices = (product.variants ?? []).map((variant) => variant.discountPrice ?? variant.price); return <strong>{prices.length ? formatCurrency(Math.min(...prices)) : "—"}</strong>; } },
    { key: "rating", header: "Đánh giá", render: (product) => <span className="font-semibold text-amber-700">★ {product.averageRating?.toFixed(1) ?? "0.0"} <small className="text-slate-400">({product.reviewCount ?? 0})</small></span> },
    { key: "active", header: "Trạng thái", render: (product) => <StatusBadge value={product.isActive} /> },
    { key: "actions", header: "Thao tác", className: "sticky right-0 bg-white", render: (product) => <div className="flex justify-end gap-1"><AdminButton tone="ghost" size="icon" aria-label="Xem sản phẩm" onClick={() => undefined}><Link to={`/admin/products/${product.id}`}><Eye className="h-4 w-4" /></Link></AdminButton><AdminButton tone="ghost" size="icon" aria-label="Sửa sản phẩm"><Link to={`/admin/products/${product.id}/edit`}><Edit3 className="h-4 w-4" /></Link></AdminButton><AdminButton tone="ghost" size="icon" aria-label="Xóa sản phẩm" className="text-rose-600 hover:bg-rose-50" onClick={() => setDeleting(product)}><Trash2 className="h-4 w-4" /></AdminButton></div> },
  ];

  return <>
    <PageHeader title="Sản phẩm" description="Quản lý toàn bộ catalog, thông số kỹ thuật, cấu hình, hình ảnh và tồn kho theo variant." actions={<Link to="/admin/products/new"><AdminButton><Plus className="h-4 w-4" />Thêm sản phẩm</AdminButton></Link>} />
    <Panel>
      <div className="grid gap-3 border-b border-slate-200 p-4 lg:grid-cols-[1fr_240px_240px]">
        <form className="relative" onSubmit={(event) => { event.preventDefault(); updateParam("search", searchDraft.trim()); }}><Search className="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" /><input className={`${fieldClassName} pl-10`} placeholder="Tìm sản phẩm..." value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /></form>
        <select aria-label="Lọc thương hiệu" className={fieldClassName} value={brandId || ""} onChange={(event) => updateParam("brand", event.target.value)}><option value="">Tất cả thương hiệu</option>{brandsQuery.data?.content.map((brand) => <option key={brand.id} value={brand.id}>{brand.name}</option>)}</select>
        <select aria-label="Lọc danh mục" className={fieldClassName} value={categoryId || ""} onChange={(event) => updateParam("category", event.target.value)}><option value="">Tất cả danh mục</option>{categoriesQuery.data?.content.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select>
      </div>
      <AdminTable rows={productsQuery.data?.content ?? []} columns={columns} isLoading={productsQuery.isLoading} error={productsQuery.isError ? getApiErrorMessage(productsQuery.error) : null} onRetry={() => productsQuery.refetch()} />
      <PaginationBar page={page} totalPages={productsQuery.data?.totalPages ?? 0} totalElements={productsQuery.data?.totalElements ?? 0} size={size} onPageChange={(value) => updateParam("page", String(value))} onSizeChange={(value) => updateParam("size", String(value))} />
    </Panel>
    <ConfirmDialog open={Boolean(deleting)} onOpenChange={(open) => !open && setDeleting(null)} title="Xóa sản phẩm?" description={deleting ? `Bạn sắp xóa “${deleting.name}” và có thể ảnh hưởng dữ liệu liên quan.` : ""} confirmLabel="Xóa sản phẩm" isPending={deleteMutation.isPending} onConfirm={() => deleting && deleteMutation.mutate(deleting.id)} />
  </>;
}
