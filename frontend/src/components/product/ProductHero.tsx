import { ArrowRight, ArrowUpRight, Headphones, RotateCcw, ShieldCheck, Sparkles, Truck } from 'lucide-react';
import heroWorld from '../../assets/ladux/hero.webp';

export interface ProductHeroProps {
    onShopNowClick?: () => void;
    onAiConsultClick?: () => void;
}

const benefits = [
    { icon: ShieldCheck, title: 'Chính hãng 100%', desc: 'An tâm từ lần mở hộp' },
    { icon: Truck, title: 'Giao hàng hỏa tốc', desc: 'Nội thành nhận trong 2H' },
    { icon: RotateCcw, title: 'Đổi trả 7 ngày', desc: 'Mua sắm thật thoải mái' },
    { icon: Headphones, title: 'Luôn sẵn sàng hỗ trợ', desc: 'Tư vấn phù hợp với bạn' },
];

export default function ProductHero({ onShopNowClick, onAiConsultClick }: ProductHeroProps) {
    return (
        <section aria-labelledby="store-hero-title" className="hero-section px-5 pt-6 sm:px-8 sm:pt-8">
            <div className="container mx-auto">
                <div className="store-hero">
                    <div className="store-hero__copy">
                        <p className="store-hero__eyebrow"><span aria-hidden="true" /> TECHNOLOGY, WITH A HUMAN TOUCH</p>
                        <h1 id="store-hero-title" className="store-hero__title">
                            Công nghệ mới.
                            <span>Cảm hứng mới.</span>
                        </h1>
                        <p className="store-hero__description">
                            Một chiếc laptop phù hợp có thể mở ra rất nhiều điều.
                            Tìm người bạn đồng hành cho công việc và đam mê của bạn.
                        </p>
                        <div className="store-hero__actions">
                            <button type="button" onClick={onShopNowClick} className="store-hero__primary">
                                Khám phá laptop <ArrowUpRight className="h-4 w-4" />
                            </button>
                            <button type="button" onClick={onAiConsultClick} className="store-hero__secondary">
                                <Sparkles className="h-4 w-4" /> Tìm cùng trợ lý AI
                            </button>
                        </div>
                        <p className="store-hero__note"><ShieldCheck className="h-3.5 w-3.5" /> Chính hãng, chỉn chu trong từng trải nghiệm.</p>
                    </div>
                    <div className="store-hero__art">
                        <img src={heroWorld} alt="Laptop bạc trên bục trưng bày cùng dải kính màu hổ phách — minh họa riêng cho Ladux" width={1536} height={1024} className="store-hero__image" decoding="async" fetchPriority="high" />
                        <div className="store-hero__tag"><span className="hero-tag-icon"><Sparkles size={17} /></span><div><strong>Được chọn cho bạn</strong><span>Thiết kế tinh gọn. Hiệu năng vượt trội.</span></div><ArrowUpRight size={16} /></div>
                    </div>
                </div>
                <div className="store-hero__benefits">
                    {benefits.map(({ icon: Icon, title, desc }) => (
                        <div key={title} className="hero-benefit flex items-center gap-3">
                            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-sky-50 text-sky-700"><Icon className="h-5 w-5" strokeWidth={1.6} /></span>
                            <div><h2 className="text-xs font-semibold text-slate-700 sm:text-sm">{title}</h2><p className="mt-1 text-[11px] text-slate-500 sm:text-xs">{desc}</p></div>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
