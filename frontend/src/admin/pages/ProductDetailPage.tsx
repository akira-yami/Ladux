import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Edit3, ImagePlus, LoaderCircle, Package, Plus, Trash2, Upload } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../../app/components/ui/dialog";
import { env } from "../../config/env";
import { adminApi } from "../api/adminApi";
import { AdminButton, AdminTable, ConfirmDialog, fieldClassName, LoadingScreen, PageHeader, Panel, StatusBadge, type AdminColumn } from "../components/AdminUI";
import { adminQueryKeys } from "../queryKeys";
import type { ProductImageResponse, ProductVariantRequest, ProductVariantResponse } from "../types";
import { formatCurrency, getApiErrorMessage, resolveImageUrl } from "../utils";

const emptyVariant: ProductVariantRequest = { colorId: null, ram: "", rom: "", price: 0, discountPrice: null, stockQuantity: 0, isActive: true };

export default function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>();
  const id = Number(productId);
  const queryClient = useQueryClient();
  const [variantDialogOpen, setVariantDialogOpen] = useState(false);
  const [variantForm, setVariantForm] = useState<ProductVariantRequest>(emptyVariant);
  const [editingVariantId, setEditingVariantId] = useState<number | null>(null);
  const [deletingVariant, setDeletingVariant] = useState<ProductVariantResponse | null>(null);
  const [deletingImage, setDeletingImage] = useState<ProductImageResponse | null>(null);
  const [imageUrl, setImageUrl] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const productQuery = useQuery({ queryKey: adminQueryKeys.detail("products", id), queryFn: () => adminApi.products.detail(id), enabled: id > 0 });
  const supplierLinksQuery = useQuery({ queryKey: adminQueryKeys.resource("product-suppliers", { productId: id }), queryFn: () => adminApi.productSuppliers.byProduct(id), enabled: id > 0 });
  const colorsQuery = useQuery({ queryKey: adminQueryKeys.resource("colors-lookup", {}), queryFn: () => adminApi.colors.list({ page: 0, size: 100, sort: "name,asc" }), staleTime: 5 * 60_000 });

  const invalidate = () => { queryClient.invalidateQueries({ queryKey: adminQueryKeys.detail("products", id) }); queryClient.invalidateQueries({ queryKey: ["admin", "products"] }); };
  const variantMutation = useMutation({ mutationFn: () => editingVariantId ? adminApi.products.updateVariant(editingVariantId, { ...variantForm, productId: id }) : adminApi.products.createVariant({ ...variantForm, productId: id }), onSuccess: () => { toast.success(editingVariantId ? "Đã cập nhật cấu hình" : "Đã thêm cấu hình"); setVariantDialogOpen(false); invalidate(); }, onError: (error) => setFormError(getApiErrorMessage(error)) });
  const deleteVariantMutation = useMutation({ mutationFn: (variantId: number) => adminApi.products.deleteVariant(variantId), onSuccess: () => { toast.success("Đã xóa cấu hình"); setDeletingVariant(null); invalidate(); }, onError: (error) => toast.error(getApiErrorMessage(error)) });
  const imageUrlMutation = useMutation({
    mutationFn: () => {
      const normalizedUrl = imageUrl.trim();
      const exists = productQuery.data?.images.some((image) => image.imageUrl === normalizedUrl);
      return exists ? Promise.resolve([] as ProductImageResponse[]) : adminApi.products.uploadImages(id, undefined, [normalizedUrl]);
    },
    onSuccess: (images) => { images.length ? toast.success("Đã thêm ảnh") : toast.info("Ảnh này đã có trong sản phẩm"); setImageUrl(""); invalidate(); },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });
  const uploadMutation = useMutation({ mutationFn: (files: File[]) => adminApi.products.uploadImages(id, files), onSuccess: () => { toast.success("Đã upload ảnh sản phẩm"); invalidate(); }, onError: (error) => toast.error(getApiErrorMessage(error)) });
  const deleteImageMutation = useMutation({ mutationFn: (imageId: number) => adminApi.products.deleteImage(id, imageId), onSuccess: () => { toast.success("Đã xóa ảnh"); setDeletingImage(null); invalidate(); }, onError: (error) => toast.error(getApiErrorMessage(error)) });

  if (productQuery.isLoading) return <LoadingScreen label="Đang tải chi tiết sản phẩm..." />;
  if (productQuery.isError || !productQuery.data) return <Panel className="p-8 text-center"><p className="font-bold text-rose-600">{getApiErrorMessage(productQuery.error)}</p><Link to="/admin/products" className="mt-4 inline-block text-sm font-bold text-indigo-600">Trở lại danh sách</Link></Panel>;
  const product = productQuery.data;
  const productImages = Array.from(new Map(product.images.map((image) => [image.imageUrl || image.id, image])).values());

  const openVariant = (variant?: ProductVariantResponse) => {
    setEditingVariantId(variant?.id ?? null);
    setVariantForm(variant ? { productId: id, colorId: variant.color?.id ?? null, ram: variant.ram, rom: variant.rom, price: variant.price, discountPrice: variant.discountPrice, stockQuantity: variant.stockQuantity, isActive: variant.isActive } : { ...emptyVariant, productId: id });
    setFormError(null);
    setVariantDialogOpen(true);
  };

  const submitVariant = (event: FormEvent) => {
    event.preventDefault(); setFormError(null);
    if ((variantForm.price ?? 0) < 0 || variantForm.stockQuantity < 0) return setFormError("Giá và tồn kho không được âm");
    if (variantForm.discountPrice != null && variantForm.price != null && variantForm.discountPrice > variantForm.price) return setFormError("Giá giảm không được lớn hơn giá gốc");
    variantMutation.mutate();
  };

  const variantColumns: AdminColumn<ProductVariantResponse>[] = [
    { key: "sku", header: "SKU", render: (variant) => <code className="font-bold text-slate-900">{variant.sku || `#${variant.id}`}</code> },
    { key: "config", header: "Cấu hình", render: (variant) => <div><p className="font-semibold">{[variant.ram, variant.rom].filter(Boolean).join(" · ") || "Mặc định"}</p><p className="mt-1 text-xs text-slate-400">{variant.color?.name ?? "Không màu"}</p></div> },
    { key: "price", header: "Giá", render: (variant) => <div><p className="font-bold">{formatCurrency(variant.discountPrice ?? variant.price)}</p>{variant.discountPrice != null && <p className="text-xs text-slate-400 line-through">{formatCurrency(variant.price)}</p>}</div> },
    { key: "stock", header: "Tồn kho", render: (variant) => <span className={variant.stockQuantity <= 5 ? "font-black text-amber-600" : "font-bold text-slate-900"}>{variant.stockQuantity}</span> },
    { key: "active", header: "Trạng thái", render: (variant) => <StatusBadge value={variant.isActive} /> },
    { key: "actions", header: "Thao tác", render: (variant) => <div className="flex justify-end gap-1"><AdminButton tone="ghost" size="icon" aria-label="Sửa cấu hình" onClick={() => openVariant(variant)}><Edit3 className="h-4 w-4" /></AdminButton><AdminButton tone="ghost" size="icon" aria-label="Xóa cấu hình" className="text-rose-600 hover:bg-rose-50" onClick={() => setDeletingVariant(variant)}><Trash2 className="h-4 w-4" /></AdminButton></div> },
  ];

  return <>
    <PageHeader title={product.name} description={`Sản phẩm #${product.id} · ${product.brand?.name ?? "Chưa có thương hiệu"} · ${product.category?.name ?? "Chưa có danh mục"}`} actions={<><Link to="/admin/products"><AdminButton tone="secondary"><ArrowLeft className="h-4 w-4" />Danh sách</AdminButton></Link><Link to={`/admin/products/${id}/edit`}><AdminButton><Edit3 className="h-4 w-4" />Chỉnh sửa</AdminButton></Link></>} />
    <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
      <Panel className="p-5 sm:p-6"><div className="flex items-start gap-4"><span className="rounded-2xl bg-indigo-50 p-3 text-indigo-600"><Package className="h-6 w-6" /></span><div><h2 className="text-lg font-extrabold text-slate-900">Thông tin catalog</h2><div className="mt-2"><StatusBadge value={product.isActive} /></div></div></div><dl className="mt-6 grid gap-x-6 gap-y-4 sm:grid-cols-2">{[["CPU", product.cpu], ["GPU", product.gpu], ["Màn hình", product.display], ["Pin", product.battery], ["Trọng lượng", product.weight], ["Hệ điều hành", product.os], ["Số quạt", product.numberOfFans]].map(([label, value]) => <div key={String(label)} className="rounded-xl bg-slate-50 p-3"><dt className="text-xs font-bold uppercase tracking-wider text-slate-400">{label}</dt><dd className="mt-1 text-sm font-bold text-slate-800">{value ?? "Chưa cập nhật"}</dd></div>)}</dl>{product.description && <p className="mt-5 whitespace-pre-line text-sm leading-7 text-slate-600">{product.description}</p>}</Panel>
      <Panel className="p-5 sm:p-6"><h2 className="text-lg font-extrabold text-slate-900">Đối tác cung ứng</h2><p className="mt-1 text-sm text-slate-500">Liên kết nhà cung cấp của sản phẩm.</p><div className="mt-5 space-y-3">{supplierLinksQuery.data?.map((link) => <div key={link.id} className="flex items-center justify-between rounded-xl border border-slate-200 p-3"><div><p className="font-bold text-slate-900">{link.supplierName || `NCC #${link.supplierId}`}</p><p className="mt-1 text-xs text-slate-400">Lead time: {link.leadTimeDays ?? "—"} ngày</p></div><strong className="text-sm">{formatCurrency(link.costPrice)}</strong></div>)}{!supplierLinksQuery.isLoading && !supplierLinksQuery.data?.length && <p className="rounded-xl bg-slate-50 py-8 text-center text-sm text-slate-400">Chưa có liên kết nhà cung cấp</p>}</div></Panel>
    </div>

    <Panel><div className="flex items-center justify-between border-b border-slate-200 p-5"><div><h2 className="text-lg font-extrabold text-slate-900">Cấu hình & tồn kho</h2><p className="mt-1 text-sm text-slate-500">{product.variants.length} variant đang được khai báo</p></div><AdminButton onClick={() => openVariant()}><Plus className="h-4 w-4" />Thêm cấu hình</AdminButton></div><AdminTable rows={product.variants} columns={variantColumns} /></Panel>

    <Panel className="overflow-hidden"><div className="border-b border-slate-200 p-5"><h2 className="text-lg font-extrabold text-slate-900">Hình ảnh sản phẩm</h2><p className="mt-1 text-sm text-slate-500">Upload file ảnh tối đa 5 MB/file hoặc thêm bằng URL.</p></div><div className="grid gap-3 p-5 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-6">{productImages.map((image) => <div key={image.id} className="group relative aspect-square overflow-hidden rounded-2xl border border-slate-200 bg-slate-50"><img src={resolveImageUrl(image.imageUrl, env.backendOrigin) ?? ""} alt={`Ảnh sản phẩm ${image.id}`} className="h-full w-full object-cover" /><button aria-label="Xóa ảnh" onClick={() => setDeletingImage(image)} className="absolute right-2 top-2 flex h-9 w-9 items-center justify-center rounded-xl bg-white/90 text-rose-600 opacity-0 shadow transition group-hover:opacity-100 focus:opacity-100"><Trash2 className="h-4 w-4" /></button></div>)}{!productImages.length && <div className="col-span-full rounded-2xl bg-slate-50 py-12 text-center text-sm text-slate-400">Sản phẩm chưa có ảnh</div>}</div><div className="grid gap-3 border-t border-slate-200 bg-slate-50/70 p-5 md:grid-cols-[1fr_auto_auto]"><input className={fieldClassName} value={imageUrl} onChange={(event) => setImageUrl(event.target.value)} placeholder="Nhập URL ảnh..." /><AdminButton tone="secondary" disabled={!imageUrl.trim() || imageUrlMutation.isPending} onClick={() => imageUrlMutation.mutate()}><ImagePlus className="h-4 w-4" />Thêm URL</AdminButton><label className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold text-white ${uploadMutation.isPending ? "cursor-not-allowed bg-slate-400" : "cursor-pointer bg-indigo-600 hover:bg-primary-hover"}`}><Upload className="h-4 w-4" />Upload file<input type="file" multiple accept="image/*" className="sr-only" disabled={uploadMutation.isPending} onChange={(event) => { if (uploadMutation.isPending) return; const files = Array.from(event.target.files ?? []); const invalid = files.some((file) => !file.type.startsWith("image/") || file.size > 5 * 1024 * 1024); const total = files.reduce((sum, file) => sum + file.size, 0); if (invalid || total > 25 * 1024 * 1024) return toast.error("File không hợp lệ hoặc vượt giới hạn dung lượng"); if (files.length) uploadMutation.mutate(files); event.target.value = ""; }} /></label></div></Panel>

    <Dialog open={variantDialogOpen} onOpenChange={setVariantDialogOpen}><DialogContent className="border-slate-200 bg-white text-slate-950 sm:max-w-2xl"><DialogHeader><DialogTitle>{editingVariantId ? "Chỉnh sửa" : "Thêm"} cấu hình</DialogTitle><DialogDescription className="text-slate-500">Chọn màu trực tiếp từ catalog backend.</DialogDescription></DialogHeader><form onSubmit={submitVariant} className="space-y-4">{formError && <div role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{formError}</div>}<div className="grid gap-4 sm:grid-cols-2"><div><label className="mb-1.5 block text-sm font-bold text-slate-700">Màu sắc</label><select className={fieldClassName} value={variantForm.colorId ?? ""} onChange={(event) => setVariantForm((value) => ({ ...value, colorId: event.target.value ? Number(event.target.value) : null }))}><option value="">Không chọn màu</option>{colorsQuery.data?.content.map((color) => <option key={color.id} value={color.id}>{color.name} ({color.hexCode})</option>)}</select></div><div><label className="mb-1.5 block text-sm font-bold text-slate-700">RAM</label><input className={fieldClassName} value={variantForm.ram ?? ""} onChange={(event) => setVariantForm((value) => ({ ...value, ram: event.target.value }))} /></div><div><label className="mb-1.5 block text-sm font-bold text-slate-700">ROM</label><input className={fieldClassName} value={variantForm.rom ?? ""} onChange={(event) => setVariantForm((value) => ({ ...value, rom: event.target.value }))} /></div><div><label className="mb-1.5 block text-sm font-bold text-slate-700">Giá gốc</label><input className={fieldClassName} type="number" min="0" value={variantForm.price ?? ""} onChange={(event) => setVariantForm((value) => ({ ...value, price: Number(event.target.value) }))} /></div><div><label className="mb-1.5 block text-sm font-bold text-slate-700">Giá giảm</label><input className={fieldClassName} type="number" min="0" value={variantForm.discountPrice ?? ""} onChange={(event) => setVariantForm((value) => ({ ...value, discountPrice: event.target.value ? Number(event.target.value) : null }))} /></div><div><label className="mb-1.5 block text-sm font-bold text-slate-700">Tồn kho</label><input className={fieldClassName} type="number" min="0" value={variantForm.stockQuantity} onChange={(event) => setVariantForm((value) => ({ ...value, stockQuantity: Number(event.target.value) }))} /></div><label className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm font-bold text-slate-700"><input type="checkbox" checked={variantForm.isActive} onChange={(event) => setVariantForm((value) => ({ ...value, isActive: event.target.checked }))} />Đang bán</label></div><DialogFooter><AdminButton type="button" tone="secondary" onClick={() => setVariantDialogOpen(false)}>Hủy</AdminButton><AdminButton type="submit" disabled={variantMutation.isPending}>{variantMutation.isPending && <LoaderCircle className="h-4 w-4 animate-spin" />}Lưu cấu hình</AdminButton></DialogFooter></form></DialogContent></Dialog>
    <ConfirmDialog open={Boolean(deletingVariant)} onOpenChange={(open) => !open && setDeletingVariant(null)} title="Xóa cấu hình?" description={deletingVariant ? `SKU ${deletingVariant.sku || `#${deletingVariant.id}`} sẽ bị xóa.` : ""} confirmLabel="Xóa cấu hình" isPending={deleteVariantMutation.isPending} onConfirm={() => deletingVariant && deleteVariantMutation.mutate(deletingVariant.id)} />
    <ConfirmDialog open={Boolean(deletingImage)} onOpenChange={(open) => !open && setDeletingImage(null)} title="Xóa ảnh?" description="Ảnh sẽ bị gỡ khỏi sản phẩm." confirmLabel="Xóa ảnh" isPending={deleteImageMutation.isPending} onConfirm={() => deletingImage && deleteImageMutation.mutate(deletingImage.id)} />
  </>;
}
