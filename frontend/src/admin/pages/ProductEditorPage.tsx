import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useFieldArray, useForm } from "react-hook-form";
import { ArrowLeft, LoaderCircle, Plus, Save, Trash2 } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { z } from "zod";
import { adminApi } from "../api/adminApi";
import { AdminButton, fieldClassName, LoadingScreen, PageHeader, Panel, textareaClassName } from "../components/AdminUI";
import { adminQueryKeys } from "../queryKeys";
import type { ProductRequest } from "../types";
import { getApiErrorMessage } from "../utils";

interface ProductFormValues {
  brandId: string;
  categoryId: string;
  name: string;
  description: string;
  cpu: string;
  gpu: string;
  display: string;
  battery: string;
  weight: string;
  numberOfFans: string;
  os: string;
  isActive: boolean;
  imageUrls: string;
  variants: Array<{
    id?: number;
    colorId: string;
    ram: string;
    rom: string;
    price: string;
    discountPrice: string;
    stockQuantity: string;
    isActive: boolean;
  }>;
}

const productSchema = z.object({
  brandId: z.coerce.number().positive("Vui lòng chọn thương hiệu"),
  categoryId: z.coerce.number().positive("Vui lòng chọn danh mục"),
  name: z.string().trim().min(1, "Tên sản phẩm là bắt buộc").max(255),
  numberOfFans: z.union([z.literal(""), z.coerce.number().int().nonnegative()]),
  variants: z.array(z.object({
    colorId: z.union([z.literal(""), z.coerce.number().positive()]),
    ram: z.string(), rom: z.string(),
    price: z.coerce.number().positive("Giá phải lớn hơn 0"),
    discountPrice: z.union([z.literal(""), z.coerce.number().nonnegative()]),
    stockQuantity: z.coerce.number().int().nonnegative("Tồn kho không được âm"),
    isActive: z.boolean(),
  }).passthrough()).min(1, "Sản phẩm phải có ít nhất một cấu hình"),
}).passthrough().superRefine((data, context) => {
  data.variants.forEach((variant, index) => {
    if (variant.discountPrice !== "" && Number(variant.discountPrice) > Number(variant.price)) {
      context.addIssue({ code: "custom", message: "Giá giảm không được lớn hơn giá gốc", path: ["variants", index, "discountPrice"] });
    }
  });
});

const emptyValues: ProductFormValues = {
  brandId: "", categoryId: "", name: "", description: "", cpu: "", gpu: "", display: "", battery: "", weight: "", numberOfFans: "", os: "", isActive: true, imageUrls: "",
  variants: [{ colorId: "", ram: "", rom: "", price: "", discountPrice: "", stockQuantity: "0", isActive: true }],
};

export default function ProductEditorPage() {
  const { productId } = useParams<{ productId: string }>();
  const id = Number(productId);
  const editing = Number.isFinite(id) && id > 0;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [serverError, setServerError] = useState<string | null>(null);
  const form = useForm<ProductFormValues>({ defaultValues: emptyValues });
  const variants = useFieldArray({ control: form.control, name: "variants" });

  const brandsQuery = useQuery({ queryKey: adminQueryKeys.resource("brands-lookup", {}), queryFn: () => adminApi.brands.list({ page: 0, size: 100 }), staleTime: 5 * 60_000 });
  const categoriesQuery = useQuery({ queryKey: adminQueryKeys.resource("categories-lookup", {}), queryFn: () => adminApi.categories.list({ page: 0, size: 100 }), staleTime: 5 * 60_000 });
  const colorsQuery = useQuery({ queryKey: adminQueryKeys.resource("colors-lookup", {}), queryFn: () => adminApi.colors.list({ page: 0, size: 100, sort: "name,asc" }), staleTime: 5 * 60_000 });
  const productQuery = useQuery({ queryKey: adminQueryKeys.detail("products", id), queryFn: () => adminApi.products.detail(id), enabled: editing });

  useEffect(() => {
    const product = productQuery.data;
    if (!product) return;
    form.reset({
      brandId: String(product.brand?.id ?? ""), categoryId: String(product.category?.id ?? ""), name: product.name,
      description: product.description ?? "", cpu: product.cpu ?? "", gpu: product.gpu ?? "", display: product.display ?? "", battery: product.battery ?? "", weight: product.weight ?? "", numberOfFans: product.numberOfFans == null ? "" : String(product.numberOfFans), os: product.os ?? "", isActive: product.isActive,
      imageUrls: Array.from(new Set((product.images ?? []).map((image) => image.imageUrl).filter(Boolean))).join("\n"),
      variants: (product.variants ?? []).length ? product.variants.map((variant) => ({ id: variant.id, colorId: variant.color?.id ? String(variant.color.id) : "", ram: variant.ram ?? "", rom: variant.rom ?? "", price: String(variant.price ?? ""), discountPrice: variant.discountPrice == null ? "" : String(variant.discountPrice), stockQuantity: String(variant.stockQuantity), isActive: variant.isActive })) : emptyValues.variants,
    });
  }, [form, productQuery.data]);

  const mutation = useMutation({
    mutationFn: ({ payload }: { payload: ProductRequest }) => editing ? adminApi.products.update(id, payload) : adminApi.products.create(payload),
    onSuccess: (product) => {
      toast.success(editing ? "Đã cập nhật sản phẩm" : "Đã tạo sản phẩm");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      navigate(`/admin/products/${product.id}`, { replace: true });
    },
    onError: (error) => setServerError(getApiErrorMessage(error)),
  });

  const submit = form.handleSubmit((values) => {
    setServerError(null);
    const parsed = productSchema.safeParse(values);
    if (!parsed.success) {
      setServerError(parsed.error.issues[0]?.message ?? "Dữ liệu sản phẩm chưa hợp lệ");
      return;
    }
    const nullable = (value: string) => value.trim() || null;
    mutation.mutate({ payload: {
      brandId: Number(values.brandId), categoryId: Number(values.categoryId), name: values.name.trim(), description: nullable(values.description), cpu: nullable(values.cpu), gpu: nullable(values.gpu), display: nullable(values.display), battery: nullable(values.battery), weight: nullable(values.weight), numberOfFans: values.numberOfFans === "" ? null : Number(values.numberOfFans), os: nullable(values.os), isActive: values.isActive,
      imageUrls: Array.from(new Set(values.imageUrls.split(/\r?\n/).map((url) => url.trim()).filter(Boolean))),
      variants: values.variants.map((variant) => ({ id: variant.id ?? null, productId: editing ? id : null, colorId: variant.colorId ? Number(variant.colorId) : null, ram: nullable(variant.ram), rom: nullable(variant.rom), price: Number(variant.price), discountPrice: variant.discountPrice === "" ? null : Number(variant.discountPrice), stockQuantity: Number(variant.stockQuantity), isActive: variant.isActive })),
    } });
  });

  if (editing && productQuery.isLoading) return <LoadingScreen label="Đang tải thông tin sản phẩm..." />;

  return <>
    <PageHeader title={editing ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm"} description="Thông tin được lưu trực tiếp vào catalog backend. Cấu hình sản phẩm là bắt buộc." actions={<Link to={editing ? `/admin/products/${id}` : "/admin/products"}><AdminButton tone="secondary"><ArrowLeft className="h-4 w-4" />Quay lại</AdminButton></Link>} />
    <form onSubmit={submit} className="space-y-6" noValidate>
      {serverError && <div role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700">{serverError}</div>}
      <Panel className="p-5 sm:p-6">
        <div className="mb-5"><h2 className="text-lg font-extrabold text-slate-900">Thông tin cơ bản</h2><p className="mt-1 text-sm text-slate-500">Tên, phân loại và mô tả bán hàng.</p></div>
        <div className="grid gap-4 md:grid-cols-2">
          <div><label className="mb-1.5 block text-sm font-bold text-slate-700" htmlFor="brandId">Thương hiệu *</label><select id="brandId" className={fieldClassName} {...form.register("brandId")}><option value="">Chọn thương hiệu</option>{brandsQuery.data?.content.map((brand) => <option key={brand.id} value={brand.id}>{brand.name}</option>)}</select></div>
          <div><label className="mb-1.5 block text-sm font-bold text-slate-700" htmlFor="categoryId">Danh mục *</label><select id="categoryId" className={fieldClassName} {...form.register("categoryId")}><option value="">Chọn danh mục</option>{categoriesQuery.data?.content.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div>
          <div className="md:col-span-2"><label className="mb-1.5 block text-sm font-bold text-slate-700" htmlFor="name">Tên sản phẩm *</label><input id="name" className={fieldClassName} maxLength={255} {...form.register("name")} /></div>
          <div className="md:col-span-2"><label className="mb-1.5 block text-sm font-bold text-slate-700" htmlFor="description">Mô tả</label><textarea id="description" className={textareaClassName} {...form.register("description")} /></div>
          <label className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm font-bold text-slate-700"><input type="checkbox" {...form.register("isActive")} />Đang hiển thị bán</label>
        </div>
      </Panel>

      <Panel className="p-5 sm:p-6">
        <div className="mb-5"><h2 className="text-lg font-extrabold text-slate-900">Thông số kỹ thuật</h2><p className="mt-1 text-sm text-slate-500">Dữ liệu này hiển thị tại chi tiết sản phẩm storefront.</p></div>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{[
          ["cpu", "Bộ xử lý (CPU)"], ["gpu", "Đồ họa (GPU)"], ["display", "Màn hình"], ["battery", "Pin"], ["weight", "Trọng lượng"], ["os", "Hệ điều hành"], ["numberOfFans", "Số quạt"],
        ].map(([key, label]) => <div key={key}><label htmlFor={key} className="mb-1.5 block text-sm font-bold text-slate-700">{label}</label><input id={key} className={fieldClassName} type={key === "numberOfFans" ? "number" : "text"} min={key === "numberOfFans" ? 0 : undefined} {...form.register(key as keyof ProductFormValues)} /></div>)}</div>
      </Panel>

      <Panel className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-200 p-5 sm:p-6"><div><h2 className="text-lg font-extrabold text-slate-900">Cấu hình sản phẩm</h2><p className="mt-1 text-sm text-slate-500">RAM, ROM, màu, giá và tồn kho theo variant.</p></div><AdminButton type="button" tone="secondary" onClick={() => variants.append({ colorId: "", ram: "", rom: "", price: "", discountPrice: "", stockQuantity: "0", isActive: true })}><Plus className="h-4 w-4" />Thêm cấu hình</AdminButton></div>
        <div className="space-y-4 p-5 sm:p-6">{variants.fields.map((field, index) => <div key={field.id} className="rounded-2xl border border-slate-200 bg-slate-50/50 p-4"><div className="mb-4 flex items-center justify-between"><p className="text-sm font-extrabold text-slate-900">Cấu hình {index + 1}</p><AdminButton type="button" tone="ghost" size="icon" aria-label={`Xóa cấu hình ${index + 1}`} className="text-rose-600 hover:bg-rose-50" disabled={variants.fields.length <= 1} onClick={() => variants.remove(index)}><Trash2 className="h-4 w-4" /></AdminButton></div><div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <div><label className="mb-1 block text-xs font-bold text-slate-600">Màu sắc</label><select className={fieldClassName} {...form.register(`variants.${index}.colorId`)}><option value="">Không chọn màu</option>{colorsQuery.data?.content.map((color) => <option key={color.id} value={color.id}>{color.name} ({color.hexCode})</option>)}</select></div>
          <div><label className="mb-1 block text-xs font-bold text-slate-600">RAM</label><input className={fieldClassName} placeholder="16GB" {...form.register(`variants.${index}.ram`)} /></div>
          <div><label className="mb-1 block text-xs font-bold text-slate-600">ROM</label><input className={fieldClassName} placeholder="512GB SSD" {...form.register(`variants.${index}.rom`)} /></div>
          <div><label className="mb-1 block text-xs font-bold text-slate-600">Giá gốc *</label><input className={fieldClassName} type="number" min="0" step="1" {...form.register(`variants.${index}.price`)} /></div>
          <div><label className="mb-1 block text-xs font-bold text-slate-600">Giá giảm</label><input className={fieldClassName} type="number" min="0" step="1" {...form.register(`variants.${index}.discountPrice`)} /></div>
          <div><label className="mb-1 block text-xs font-bold text-slate-600">Tồn kho *</label><input className={fieldClassName} readOnly={Boolean(form.getValues(`variants.${index}.id`))} type="number" min="0" step="1" {...form.register(`variants.${index}.stockQuantity`)} />{form.getValues(`variants.${index}.id`) && <p className="mt-1 text-xs text-slate-500">Thay đổi số lượng trong mục điều chỉnh kho.</p>}</div>
          <label className="flex items-center gap-3 self-end rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-bold text-slate-700"><input type="checkbox" {...form.register(`variants.${index}.isActive`)} />Đang bán</label>
        </div></div>)}</div>
      </Panel>

      <Panel className="p-5 sm:p-6"><label htmlFor="imageUrls" className="mb-1.5 block text-sm font-bold text-slate-700">URL hình ảnh</label><textarea id="imageUrls" className={textareaClassName} placeholder="Mỗi URL một dòng" {...form.register("imageUrls")} /><p className="mt-2 text-xs text-slate-400">Có thể upload file sau khi tạo sản phẩm tại trang chi tiết.</p></Panel>
      <div className="flex justify-end gap-3"><Link to={editing ? `/admin/products/${id}` : "/admin/products"}><AdminButton type="button" tone="secondary">Hủy</AdminButton></Link><AdminButton type="submit" disabled={mutation.isPending}>{mutation.isPending ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}{editing ? "Lưu thay đổi" : "Tạo sản phẩm"}</AdminButton></div>
    </form>
  </>;
}
