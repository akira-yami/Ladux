import { useNavigate } from 'react-router-dom';
import { ArrowRight, Briefcase, Feather, Gamepad2, GraduationCap, Laptop, Palette, UserCheck } from 'lucide-react';
import ProductHero from '../components/product/ProductHero';
import ProductCard from '../components/product/ProductCard';
import { LaptopProduct } from '../types';
import { useProductStore, useWishlistStore } from '../stores';
import { productPath, ROUTES } from '../app/routePaths';

const API_ORIGIN = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/api\/v1\/?$/, '');
function resolvePublicAssetUrl(path?: string | null): string | null {
    if (!path) return null;
    if (/^(https?:|data:|blob:)/i.test(path)) return path;
    return `${API_ORIGIN}${path.startsWith('/') ? '' : '/'}${path}`;
}

export interface ProductStoreViewProps {
    filteredProducts?: LaptopProduct[];
    toggleWishlist?: (laptopId: number) => Promise<void>;
    setSelectedProduct: (product: LaptopProduct) => void;
    addToCartCustom?: (product: LaptopProduct, variantId: number, quantity: number) => Promise<boolean>;
    showToast: (msg: string) => void;
}

const needs = [
    { key: 'gaming', title: 'Chơi game', icon: Gamepad2, desc: 'Mượt mà từng trận đấu', keyword: 'gaming' },
    { key: 'van-phong', title: 'Làm việc', icon: Briefcase, desc: 'Gọn gàng, hiệu quả hơn', keyword: 'văn phòng' },
    { key: 'ultrabook', title: 'Mỏng nhẹ', icon: Feather, desc: 'Mang theo mọi ý tưởng', keyword: 'ultrabook' },
    { key: 'do-hoa', title: 'Sáng tạo', icon: Palette, desc: 'Cho cảm hứng bay xa', keyword: 'đồ họa' },
    { key: 'doanh-nhan', title: 'Doanh nhân', icon: UserCheck, desc: 'Tinh tế trong từng việc', keyword: 'doanh nhân' },
    { key: 'sinh-vien', title: 'Học tập', icon: GraduationCap, desc: 'Cùng bạn mỗi ngày', keyword: 'sinh viên' },
];

export default function ProductStoreView({ filteredProducts, toggleWishlist, setSelectedProduct, addToCartCustom, showToast }: ProductStoreViewProps) {
    const navigate = useNavigate();
    const { brands, categories, setBrandFilter, setCategoryFilter, isLoading } = useProductStore();
    const { wishlistProductIds } = useWishlistStore();
    const products = filteredProducts || [];
    const openBrand = (id: number) => { setBrandFilter(id); navigate(ROUTES.products); };
    const renderProduct = (laptop: LaptopProduct) => (
        <ProductCard key={laptop.id} laptop={laptop} isWishlisted={wishlistProductIds.includes(laptop.id)}
            onToggleWishlist={id => { void toggleWishlist?.(id); }}
            onSelectProduct={product => { setSelectedProduct(product); navigate(productPath(product.id)); }}
            onAddToCart={product => {
                const variant = product.variants.find(item => item.id === product.defaultVariantId && item.stockQuantity > 0) ?? product.variants.find(item => item.stockQuantity > 0);
                if (variant) void addToCartCustom?.(product, variant.id, 1);
            }} />
    );

    return (
        <main className="store-landing">
            <ProductHero onShopNowClick={() => document.getElementById('catalog-section')?.scrollIntoView({ behavior: 'smooth' })} onAiConsultClick={() => showToast('Bạn có thể mở trợ lý ở góc màn hình để được tư vấn laptop nhé!')} />
            <div className="container mx-auto space-y-12 px-5 py-10 sm:px-8 sm:py-12">
                <section>
                    <div className="mb-6"><p className="mb-2 text-xs font-semibold text-sky-600">Bắt đầu từ điều bạn cần</p><h2 className="text-2xl font-bold tracking-tight text-slate-800 sm:text-3xl">Chiếc laptop dành cho bạn</h2><p className="mt-2 text-sm text-slate-500">Học, làm, chơi hay sáng tạo — chọn một điều bạn yêu thích.</p></div>
                    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
                        {needs.map(({ key, title, icon: Icon, desc, keyword }) => {
                            const category = categories.find(item => item.name.toLowerCase().includes(keyword) || item.slug?.toLowerCase().includes(key));
                            return <button key={key} type="button" onClick={() => { if (category) setCategoryFilter(category.id); navigate(ROUTES.products); }} className="store-card store-card-hover flex flex-col items-start rounded-2xl border border-sky-100/80 bg-white p-4 text-left sm:p-5"><span className="store-category-icon mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-sky-50 text-sky-600"><Icon className="h-5 w-5" strokeWidth={1.6} /></span><span className="text-sm font-semibold text-slate-700">{title}</span><span className="mt-1 text-[11px] leading-5 text-slate-500">{desc}</span></button>;
                        })}
                    </div>
                </section>
                {brands.length > 0 && <section className="rounded-3xl border border-sky-100 bg-white px-5 py-6 sm:px-8"><div className="mb-5 flex flex-wrap items-center justify-between gap-2"><h2 className="text-sm font-semibold text-slate-700">Những thương hiệu bạn tin yêu</h2><span className="text-xs text-slate-400">Chính hãng. Chọn thật an tâm.</span></div><div className="grid grid-cols-3 gap-4 sm:grid-cols-6">{brands.map(brand => { const logo = resolvePublicAssetUrl(brand.logoUrl); return <button key={brand.id} type="button" onClick={() => openBrand(brand.id)} className="store-brand-card flex h-14 items-center justify-center rounded-xl border border-slate-100 bg-white p-3">{logo ? <img src={logo} alt={brand.name} loading="lazy" className="max-h-7 max-w-full object-contain" /> : <span className="text-sm font-bold text-slate-600">{brand.name}</span>}</button>; })}</div></section>}
                <section id="catalog-section" className="scroll-mt-36">
                    <div className="mb-6 flex flex-wrap items-end justify-between gap-4"><div><p className="mb-2 text-xs font-semibold text-sky-600">Một chút mới mẻ, mỗi ngày</p><h2 className="text-2xl font-bold tracking-tight text-slate-800 sm:text-3xl">Laptop mới lên kệ</h2></div><button type="button" onClick={() => navigate(ROUTES.products)} className="inline-flex items-center gap-2 text-sm font-semibold text-sky-700 hover:underline">Xem tất cả <ArrowRight className="h-4 w-4" /></button></div>
                    {isLoading ? <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4" role="status" aria-label="Đang tải laptop">{Array.from({ length: 4 }, (_, i) => <div key={i} className="h-[390px] animate-pulse rounded-3xl border border-sky-100 bg-white p-4"><div className="aspect-[4/3] rounded-2xl bg-sky-50" /><div className="mt-5 h-4 w-3/4 rounded-full bg-slate-100" /><div className="mt-3 h-3 w-1/2 rounded-full bg-slate-100" /></div>)}</div> : products.length > 0 ? <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">{products.slice(0, 8).map(renderProduct)}</div> : <div className="rounded-3xl border border-sky-100 bg-white px-6 py-14 text-center"><Laptop className="mx-auto mb-4 h-9 w-9 text-sky-600" /><p className="text-sm font-medium text-slate-600">Laptop đang được cập nhật</p><p className="mt-2 text-xs text-slate-500">Ghé lại một chút nữa để khám phá sản phẩm nhé.</p></div>}
                </section>
                {brands.map(brand => {
                    const items = products.filter(product => product.brandId === brand.id || product.brand.toLowerCase() === brand.name.toLowerCase());
                    if (!items.length) return null;
                    return <section key={brand.id}><div className="mb-5 flex items-center justify-between gap-4"><h2 className="text-xl font-bold tracking-tight text-slate-800">Khám phá {brand.name}</h2><button type="button" onClick={() => openBrand(brand.id)} className="inline-flex items-center gap-2 text-xs font-semibold text-sky-700 hover:underline">Xem tất cả <ArrowRight className="h-3.5 w-3.5" /></button></div><div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">{items.slice(0, 4).map(renderProduct)}</div></section>;
                })}
            </div>
        </main>
    );
}
