import { Link } from 'react-router-dom';
import { Mail, MapPin, Phone, ShieldCheck } from 'lucide-react';
import BrandMark from './BrandMark';
import { ROUTES } from '../../app/routePaths';
import { STOREFRONT_CONTACT } from '../../config/storefront';

export default function Footer() {
    return (
        <footer className="mt-12 border-t border-sky-100 bg-white text-sm text-slate-500">
            <div className="container mx-auto grid gap-8 px-5 py-12 sm:grid-cols-2 sm:px-8 lg:grid-cols-4">
                <div><Link to={ROUTES.home} className="inline-flex"><BrandMark /></Link><p className="mt-4 max-w-xs text-xs leading-6">Một chiếc laptop phù hợp, một ngày dễ chịu hơn. Đồng hành cùng bạn trong học tập, công việc và những ý tưởng mới.</p><span className="mt-4 inline-flex items-center gap-1.5 text-xs text-sky-700"><ShieldCheck className="h-4 w-4" /> Chính hãng, an tâm mỗi ngày</span></div>
                <div><h2 className="mb-4 text-sm font-semibold text-slate-700">Khám phá LADUX</h2><ul className="space-y-3 text-xs"><li><Link to={ROUTES.products} className="hover:text-sky-600">Tất cả laptop</Link></li><li><Link to={ROUTES.about} className="hover:text-sky-600">Câu chuyện của chúng mình</Link></li><li><Link to={ROUTES.contact} className="hover:text-sky-600">Liên hệ & hỗ trợ</Link></li></ul></div>
                <div><h2 className="mb-4 text-sm font-semibold text-slate-700">Góc của bạn</h2><ul className="space-y-3 text-xs"><li><Link to={ROUTES.account} className="hover:text-sky-600">Tài khoản của bạn</Link></li><li><Link to={ROUTES.orders} className="hover:text-sky-600">Theo dõi đơn hàng</Link></li><li><Link to={ROUTES.wishlist} className="hover:text-sky-600">Laptop yêu thích</Link></li><li><Link to={ROUTES.cart} className="hover:text-sky-600">Giỏ hàng</Link></li></ul></div>
                <div><h2 className="mb-4 text-sm font-semibold text-slate-700">Luôn ở đây để giúp bạn</h2><div className="space-y-3 text-xs">{STOREFRONT_CONTACT.phone && <a href={`tel:${STOREFRONT_CONTACT.phone}`} className="flex items-center gap-2 hover:text-sky-600"><Phone className="h-4 w-4 shrink-0 text-sky-500" />{STOREFRONT_CONTACT.phone}</a>}<a href={`mailto:${STOREFRONT_CONTACT.email}`} className="flex items-center gap-2 break-all hover:text-sky-600"><Mail className="h-4 w-4 shrink-0 text-sky-500" />{STOREFRONT_CONTACT.email}</a><p className="flex items-start gap-2 leading-5"><MapPin className="mt-0.5 h-4 w-4 shrink-0 text-sky-500" />{STOREFRONT_CONTACT.address}</p></div></div>
            </div>
            <div className="border-t border-slate-100"><div className="container mx-auto flex flex-wrap items-center justify-between gap-3 px-5 py-5 text-[11px] text-slate-400 sm:px-8"><p>© {new Date().getFullYear()} LADUX. Cùng bạn mở ra những điều hay.</p><span>Thanh toán VNPay · Giao hàng toàn quốc</span></div></div>
        </footer>
    );
}
