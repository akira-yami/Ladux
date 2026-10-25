import { Cpu, HardDrive, Heart, ImageOff, Monitor, ShoppingCart, Star } from 'lucide-react';
import { LaptopProduct, formatVND } from '../../types';

export interface ProductCardProps {
    laptop: LaptopProduct;
    isWishlisted: boolean;
    onToggleWishlist: (laptopId: number) => void;
    onSelectProduct: (laptop: LaptopProduct) => void;
    onAddToCart: (laptop: LaptopProduct) => void;
}

export default function ProductCard({ laptop, isWishlisted, onToggleWishlist, onSelectProduct, onAddToCart }: ProductCardProps) {
    const inStock = laptop.variants ? laptop.variants.some(variant => variant.stockQuantity > 0) : laptop.stockQuantity > 0;
    const discounted = laptop.discountPrice != null && laptop.discountPrice < laptop.price;
    const finalPrice = discounted ? laptop.discountPrice! : laptop.price;
    const specs = [{ icon: Cpu, value: laptop.cpu }, { icon: Cpu, value: laptop.ram }, { icon: HardDrive, value: laptop.rom }, { icon: Monitor, value: laptop.display }];

    return (
        <article className="product-card store-card store-card-hover flex h-full flex-col rounded-3xl border border-sky-100/80 bg-white p-3.5 sm:p-4">
            <div className="product-card__media relative overflow-hidden rounded-2xl bg-slate-50">
                <button type="button" onClick={() => onSelectProduct(laptop)} aria-label={`Xem ${laptop.name}`} className="product-card__image-button flex aspect-[4/3] w-full cursor-pointer items-center justify-center p-4">
                    {laptop.images?.[0] ? <img src={laptop.images[0]} alt={laptop.name} loading="lazy" className="store-card__image h-full w-full object-contain" /> : <span className="product-card__placeholder flex flex-col items-center gap-2"><ImageOff className="h-8 w-8" /><span className="text-xs">Chưa có ảnh</span></span>}
                </button>
                {discounted && <span className="absolute left-2.5 top-2.5 rounded-full border border-sky-100 bg-white/95 px-2.5 py-1 text-[10px] font-semibold text-sky-700">Giá tốt</span>}
                <button type="button" onClick={() => onToggleWishlist(laptop.id)} aria-label={isWishlisted ? 'Xóa khỏi yêu thích' : 'Thêm vào yêu thích'} aria-pressed={isWishlisted} className={`absolute right-2.5 top-2.5 flex h-8 w-8 items-center justify-center rounded-full border bg-white/95 transition ${isWishlisted ? 'border-sky-200 text-sky-600' : 'border-slate-100 text-slate-400 hover:text-sky-600'}`}>
                    <Heart className={`h-4 w-4 ${isWishlisted ? 'fill-sky-100' : ''}`} strokeWidth={1.7} />
                </button>
            </div>
            <div className="flex flex-1 flex-col px-1 pt-4">
                <div className="mb-2 flex items-center justify-between gap-2 text-[10px] font-medium">
                    <span className="uppercase tracking-wider text-slate-400">{laptop.brand}</span>
                    <span className="flex items-center gap-1 text-slate-500"><Star className="h-3 w-3 fill-sky-200 text-sky-500" />{laptop.rating ? laptop.rating.toFixed(1) : '—'} <span className="text-slate-400">({laptop.reviewCount || 0})</span></span>
                </div>
                <button type="button" onClick={() => onSelectProduct(laptop)} className="mb-4 min-h-10 cursor-pointer text-left text-sm font-semibold leading-5 text-slate-800 transition hover:text-sky-700"><span className="line-clamp-2">{laptop.name}</span></button>
                <div className="grid grid-cols-2 gap-x-2 gap-y-2.5 border-b border-slate-100 pb-4">
                    {specs.map(({ icon: Icon, value }, i) => <span key={i} className="flex min-w-0 items-center gap-1.5 text-[11px] text-slate-500"><Icon className="h-3.5 w-3.5 shrink-0 text-slate-400" strokeWidth={1.6} /><span className="truncate" title={value}>{value || 'N/A'}</span></span>)}
                </div>
                {laptop.gpu && <p className="mt-3 truncate text-[11px] text-slate-500" title={laptop.gpu}>{laptop.gpu}</p>}
                <div className="mt-auto pt-4">
                    <div className="mb-3 flex flex-wrap items-baseline gap-x-2 gap-y-1"><span className="product-card__price text-lg font-bold tracking-tight text-sky-700">{formatVND(finalPrice)}</span>{discounted && <span className="text-[10px] text-slate-400 line-through">{formatVND(laptop.price)}</span>}</div>
                    <button type="button" onClick={() => onAddToCart(laptop)} disabled={!inStock} className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-sky-50 py-2.5 text-xs font-semibold text-sky-700 transition hover:bg-sky-600 hover:text-white disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400"><ShoppingCart className="h-3.5 w-3.5" />{inStock ? 'Thêm vào giỏ' : 'Tạm hết hàng'}</button>
                </div>
            </div>
        </article>
    );
}
