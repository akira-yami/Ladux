import { ArrowRight, ArrowUpRight, Headphones, RotateCcw, ShieldCheck, Truck } from 'lucide-react';
import heroWorld from '../../assets/ladux-hero-world.png';

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
        <section aria-labelledby="store-hero-title" className="px-5 pt-6 sm:px-8 sm:pt-8">
            <div className="container mx-auto">
                <div className="store-hero">
                    <div className="store-hero__copy">
                        <p className="store-hero__eyebrow"><span aria-hidden="true" /> Một khởi đầu đầy cảm hứng</p>
                        <h1 id="store-hero-title" className="store-hero__title">
                            Mở máy lên.
                            <span>Mở ra điều hay.</span>
                        </h1>
                        <p className="store-hero__description">
                            Học tập, làm việc hay thỏa sức sáng tạo.
                            Tìm chiếc laptop hợp với bạn, cùng LADUX.
                        </p>
                        <div className="store-hero__actions">
                            <button type="button" onClick={onShopNowClick} className="store-hero__primary">
                                Khám phá laptop <ArrowUpRight className="h-4 w-4" />
                            </button>
                            <button type="button" onClick={onAiConsultClick} className="store-hero__secondary">
                                Nhờ tư vấn <ArrowRight className="h-4 w-4" />
                            </button>
                        </div>
                        <p className="store-hero__note"><ShieldCheck className="h-3.5 w-3.5" /> Chính hãng. Bảo hành đầy đủ. Chọn thật an tâm.</p>
                    </div>
                    <div className="store-hero__art">
                        <div className="store-hero__halo" aria-hidden="true" />
                        <div className="store-hero__orbit" aria-hidden="true" />
                        <img src={heroWorld} alt="Thế giới ý tưởng của LADUX: laptop trắng cùng dải lụa bay, mây và kính xanh sky" width={1448} height={1086} className="store-hero__image" decoding="async" />
                        <p className="store-hero__caption">Một chiếc laptop. <span>Vạn điều có thể.</span></p>
                    </div>
                </div>
                <div className="store-hero__benefits">
                    {benefits.map(({ icon: Icon, title, desc }) => (
                        <div key={title} className="flex items-center gap-3">
                            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-sky-50 text-sky-600"><Icon className="h-5 w-5" strokeWidth={1.6} /></span>
                            <div><h2 className="text-xs font-semibold text-slate-700 sm:text-sm">{title}</h2><p className="mt-1 text-[11px] text-slate-500 sm:text-xs">{desc}</p></div>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
