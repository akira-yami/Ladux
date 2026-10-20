import React from "react";
import { ArrowRight, Laptop, LoaderCircle, Search, Sparkles, X } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { productPath, ROUTES } from "../../app/routePaths";
import { env } from "../../config/env";
import { discoveryService } from "../../services/discoveryService";
import { discoveryPath, nextSuggestionIndex, SuggestionRequests, suggestionsHeading, type SuggestionState } from "./smartSearch";
import "../../styles/smart-search.css";

interface Props {
    searchQuery: string;
    setSearchQuery: (query: string) => void;
    mobileOpen: boolean;
    onCloseMobile: () => void;
    children?: React.ReactNode;
}

export default function SmartSearchDropdown({ searchQuery, setSearchQuery, mobileOpen, onCloseMobile, children }: Props) {
    const navigate = useNavigate();
    const location = useLocation();
    const id = React.useId();
    const rootRef = React.useRef<HTMLDivElement>(null);
    const inputRef = React.useRef<HTMLInputElement>(null);
    const [draft, setDraft] = React.useState(searchQuery);
    const [open, setOpen] = React.useState(false);
    const [active, setActive] = React.useState(-1);
    const [retry, setRetry] = React.useState(0);
    const [state, setState] = React.useState<SuggestionState>({ query: "", status: "loading", data: null });
    const requests = React.useMemo(() => new SuggestionRequests((query, signal) => discoveryService.suggest(query, signal)), []);

    const close = React.useCallback(() => {
        setOpen(false);
        setActive(-1);
        onCloseMobile();
    }, [onCloseMobile]);

    React.useEffect(() => setDraft(searchQuery), [searchQuery]);
    React.useEffect(() => {
        close();
    }, [location.pathname, location.search, close]);
    React.useEffect(() => {
        if (mobileOpen) inputRef.current?.focus();
        else setOpen(false);
    }, [mobileOpen]);
    React.useEffect(() => {
        const outside = (event: PointerEvent) => {
            if (!rootRef.current?.contains(event.target as Node)) close();
        };
        document.addEventListener("pointerdown", outside);
        return () => document.removeEventListener("pointerdown", outside);
    }, [close]);
    React.useEffect(() => {
        setActive(-1);
        if (!open) return;
        requests.search(draft, next => {
            setState(next);
            setActive(-1);
        });
        return () => requests.cancel();
    }, [draft, open, retry, requests]);
    React.useEffect(() => {
        if (open && active >= 0) document.getElementById(`${id}-option-${active}`)?.scrollIntoView({ block: "nearest" });
    }, [active, id, open]);

    const data = state.query === draft.trim() && state.status === "ready" ? state.data : null;
    const products = data?.products.slice(0, 5) ?? [];
    const queries = data?.suggestedQueries.slice(0, 4) ?? [];
    const count = products.length + queries.length + 1;
    const submit = (query = draft) => {
        setSearchQuery(query.trim());
        close();
        navigate(ROUTES.products);
    };
    const select = (index: number) => {
        if (index < products.length) {
            const product = products[index];
            close();
            navigate(`${productPath(product.productId)}${product.variantId ? `?variantId=${product.variantId}` : ""}`);
        } else if (index < products.length + queries.length) {
            const query = queries[index - products.length];
            const path = !draft.trim() ? discoveryPath(query) : null;
            if (path) { close(); navigate(path); } else submit(query);
        } else {
            close();
            navigate("/smartfit");
        }
    };

    return (
        <div ref={rootRef} className={`smart-search${mobileOpen ? " smart-search--mobile-open" : ""}`}
            onBlur={(event) => {
                if (!event.currentTarget.contains(event.relatedTarget as Node | null)) close();
            }}>
            <div className="smart-search__bar">
                <input ref={inputRef} type="search" role="combobox" aria-label="Tìm kiếm laptop"
                    placeholder="Tìm chiếc laptop của bạn..." value={draft} maxLength={120} autoComplete="off"
                    aria-autocomplete="list" aria-expanded={open} aria-controls={`${id}-list`}
                    aria-activedescendant={open && active >= 0 ? `${id}-option-${active}` : undefined}
                    onFocus={() => setOpen(true)} onChange={(event) => { setDraft(event.target.value); setOpen(true); setActive(-1); }}
                    onKeyDown={(event) => {
                        if (event.nativeEvent.isComposing) return;
                        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
                            event.preventDefault();
                            setOpen(true);
                            setActive(nextSuggestionIndex(active, count, event.key === "ArrowDown" ? 1 : -1));
                        } else if (event.key === "Escape") {
                            event.preventDefault();
                            close();
                        } else if (event.key === "Enter") {
                            event.preventDefault();
                            if (open && active >= 0 && active < count) select(active); else submit();
                        }
                    }} />
                {children}
                <button type="button" onClick={() => submit()} className="smart-search__submit" aria-label="Tìm kiếm">
                    <Search size={19} />
                </button>
                <button type="button" onClick={close} className="smart-search__close" aria-label="Đóng tìm kiếm"><X size={19} /></button>
            </div>
            {open && (
                <div className="smart-search__dropdown">
                    <div className="smart-search__heading">{data ? suggestionsHeading(data.mode) : "Tìm laptop của bạn"}</div>
                    <div role="status" aria-live="polite" className="smart-search__status">
                        {state.status === "loading" && <span><LoaderCircle className="animate-spin" size={16} /> Đang tìm gợi ý...</span>}
                        {state.status === "error" && <span>Chưa tải được gợi ý. <button type="button" onClick={() => setRetry(value => value + 1)}>Thử lại</button></span>}
                        {data && products.length === 0 && <span>Chưa tìm thấy laptop phù hợp. Hãy thử từ khóa khác hoặc SmartFit.</span>}
                    </div>
                    <div id={`${id}-list`} role="listbox" aria-label="Gợi ý laptop" aria-busy={state.status === "loading"}>
                        {products.map((product, index) => (
                            <div key={`${product.productId}-${product.variantId}`} id={`${id}-option-${index}`} role="option" aria-selected={active === index}
                                className="smart-search__product" onMouseEnter={() => setActive(index)} onMouseDown={event => event.preventDefault()} onClick={() => select(index)}>
                                <div className="smart-search__image">{product.imageUrl ? <img src={/^https?:\/\//i.test(product.imageUrl) ? product.imageUrl : `${env.backendOrigin}/${product.imageUrl.replace(/^\//, "")}`} alt="" loading="lazy" /> : <Laptop size={25} />}</div>
                                <div className="smart-search__product-copy"><span>{product.name}</span><div>
                                    <strong>{product.discountPrice != null || product.price != null ? `${new Intl.NumberFormat("vi-VN").format(product.discountPrice ?? product.price!)} ₫` : "Chưa có giá"}</strong>
                                    {!product.available && <small>Tạm hết hàng</small>}
                                </div></div><ArrowRight size={16} />
                            </div>
                        ))}
                        {queries.length > 0 && <div className="smart-search__queries" role="group" aria-label={draft.trim() ? "Từ khóa liên quan" : "Khám phá nhanh"}>
                            <p>{draft.trim() ? "Từ khóa liên quan" : "Khám phá nhanh"}</p>
                            {queries.map((query, index) => {
                                const optionIndex = products.length + index;
                                return <div key={query} id={`${id}-option-${optionIndex}`} role="option" aria-selected={active === optionIndex}
                                    onMouseEnter={() => setActive(optionIndex)} onMouseDown={event => event.preventDefault()} onClick={() => select(optionIndex)}>{query}</div>;
                            })}
                        </div>}
                        <div id={`${id}-option-${count - 1}`} role="option" aria-selected={active === count - 1}
                            className="smart-search__advisor" onMouseEnter={() => setActive(count - 1)} onMouseDown={event => event.preventDefault()} onClick={() => select(count - 1)}>
                            <Sparkles size={19} /><span><strong>SmartFit Advisor</strong><small>Chọn laptop theo nhu cầu và ngân sách</small></span><ArrowRight size={16} />
                        </div>
                    </div>
                    <p className="smart-search__hint">↑ ↓ để chọn · Enter để mở · Esc để đóng</p>
                </div>
            )}
        </div>
    );
}
