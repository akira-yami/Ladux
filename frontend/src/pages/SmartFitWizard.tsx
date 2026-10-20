import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ArrowRight, Check, CircleAlert, ImageOff, LoaderCircle, Scale, SlidersHorizontal, Sparkles } from "lucide-react";
import { isCancel } from "axios";
import { env } from "../config/env";
import { ROUTES } from "../app/routePaths";
import SmartCompare from "../components/common/SmartCompare";
import { smartFitService, type SmartFitRecommendation, type SmartFitRequest, type SmartFitResponse } from "../services/smartFitService";
import { formatVND } from "../types";
import { openAssistant } from "../utils/openAssistant";
import { buildSmartFitRequest, priorityLabels, purposeLabels, readSmartFitParams, recommendationPath, selectedRecommendations, specValue, toggleComparedVariant, type SmartFitFormValues } from "../utils/smartFit";
import "../styles/smartfit.css";

function RecommendationImage({ item }: { item: SmartFitRecommendation }) {
  const [failed, setFailed] = useState(false);
  const src = item.imageUrl && (/^https?:\/\//i.test(item.imageUrl) ? item.imageUrl : `${env.backendOrigin}/${item.imageUrl.replace(/^\//, "")}`);
  return <div className="smartfit-product-image">{src && !failed ? <img src={src} alt={item.name} loading="lazy" onError={() => setFailed(true)} /> : <span><ImageOff aria-hidden="true" size={26} />Chưa có ảnh</span>}</div>;
}

export default function SmartFitWizard() {
  const [searchParams] = useSearchParams();
  const query = searchParams.toString();
  const [values, setValues] = useState(() => readSmartFitParams(searchParams));
  const [result, setResult] = useState<SmartFitResponse | null>(null);
  const [submitted, setSubmitted] = useState<SmartFitRequest | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState("");
  const [comparedIds, setComparedIds] = useState<number[]>([]);
  const request = useRef<AbortController | null>(null);
  const generation = useRef(0);
  const resultHeading = useRef<HTMLHeadingElement>(null);

  function cancelPending() {
    request.current?.abort();
    generation.current += 1;
    setLoading(false);
  }

  useEffect(() => {
    request.current?.abort();
    generation.current += 1;
    setValues(readSmartFitParams(new URLSearchParams(query)));
    setResult(null);
    setComparedIds([]);
    setError(null);
    setLoading(false);
    setNotice("");
  }, [query]);

  useEffect(() => () => { request.current?.abort(); generation.current += 1; }, []);

  useEffect(() => {
    const items = comparedIds.length ? selectedRecommendations(result?.recommendations ?? [], comparedIds) : result?.recommendations ?? [];
    window.dispatchEvent(new CustomEvent("ladux:recommendation-context", { detail: { variantIds: items.map((item) => item.variantId), productIds: items.map((item) => item.productId) } }));
  }, [result, comparedIds]);

  function update<K extends keyof SmartFitFormValues>(key: K, value: SmartFitFormValues[K]) {
    cancelPending();
    setValues((current) => ({ ...current, [key]: value }));
    setResult(null);
    setComparedIds([]);
    setError(null);
    setNotice("");
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const payload = buildSmartFitRequest(values);
    if (!payload) { setError("Kiểm tra ngân sách (từ 1 đến 1 tỷ đồng) và các nhu cầu đã chọn."); return; }
    cancelPending();
    const controller = new AbortController();
    request.current = controller;
    const currentGeneration = generation.current;
    setLoading(true);
    setError(null);
    setNotice("");
    setResult(null);
    setComparedIds([]);
    setSubmitted(payload);
    try {
      const response = await smartFitService.recommend(payload, controller.signal);
      if (controller.signal.aborted || generation.current !== currentGeneration) return;
      setResult(response);
      setLoading(false);
      // Focus the result summary after React renders the response.
      requestAnimationFrame(() => { if (generation.current === currentGeneration) resultHeading.current?.focus(); });
    } catch (failure) {
      if (controller.signal.aborted || generation.current !== currentGeneration || isCancel(failure)) return;
      setError("Chưa thể tải tư vấn lúc này. Vui lòng thử lại sau ít phút.");
      setLoading(false);
    }
  }

  const compared = selectedRecommendations(result?.recommendations ?? [], comparedIds);

  function discussRecommendations() {
    const items = compared.length ? compared : result?.recommendations ?? [];
    window.dispatchEvent(new CustomEvent("ladux:recommendation-context", { detail: { variantIds: items.map((item) => item.variantId), productIds: items.map((item) => item.productId) } }));
    openAssistant();
  }

  return (
    <main className="smartfit-page container mx-auto px-4 sm:px-6">
      <nav aria-label="Đường dẫn" className="smartfit-breadcrumb"><Link to={ROUTES.home}>Trang chủ</Link><span aria-hidden="true">/</span><span>SmartFit Advisor</span></nav>
      <section className="smartfit-hero" aria-labelledby="smartfit-title">
        <div><p className="smartfit-eyebrow"><Sparkles size={15} aria-hidden="true" /> SMARTFIT ADVISOR</p><h1 id="smartfit-title">Laptop phù hợp,<br /><span>từ nhu cầu của bạn.</span></h1><p>Chọn ngân sách và việc bạn cần làm. SmartFit đối chiếu cấu hình đang bán để gợi ý cùng lý do rõ ràng.</p></div>
        <div className="smartfit-hero-note"><SlidersHorizontal size={30} aria-hidden="true" /><strong>Bạn chọn nhu cầu.<br />Dữ liệu dẫn đường.</strong><span>Giá và cấu hình từ catalog<br />Điểm phù hợp có thể giải thích</span></div>
      </section>
      <div className="smartfit-layout">
        <form onSubmit={submit} className="smartfit-form smartfit-panel" aria-labelledby="smartfit-form-heading">
          <div><p className="smartfit-eyebrow">01 / NHU CẦU CỦA BẠN</p><h2 id="smartfit-form-heading">Bạn cần laptop như thế nào?</h2></div>
          <div className="smartfit-field"><label htmlFor="smartfit-budget">Ngân sách tối đa (VNĐ)</label><input id="smartfit-budget" type="number" inputMode="numeric" min={1} max={1_000_000_000} step={1} required value={values.budgetMax} onChange={(event) => update("budgetMax", event.target.value)} aria-describedby="smartfit-budget-hint" /><span id="smartfit-budget-hint">{values.budgetMax && Number(values.budgetMax) > 0 ? formatVND(Number(values.budgetMax)) : "Nhập ngân sách của bạn"} · Lọc theo giới hạn này</span></div>
          <div className="smartfit-field"><label htmlFor="smartfit-purpose">Mục đích chính</label><select id="smartfit-purpose" value={values.purpose} onChange={(event) => update("purpose", event.target.value as SmartFitFormValues["purpose"])}>{Object.entries(purposeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
          <div className="smartfit-field"><label htmlFor="smartfit-priority">Điều bạn ưu tiên</label><select id="smartfit-priority" value={values.priority} onChange={(event) => update("priority", event.target.value as SmartFitFormValues["priority"])}>{Object.entries(priorityLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
          <fieldset><legend>Cấu hình tối thiểu <span>(tùy chọn)</span></legend><div className="smartfit-field-pair"><div className="smartfit-field"><label htmlFor="smartfit-ram">RAM</label><select id="smartfit-ram" value={values.minRamGb} onChange={(event) => update("minRamGb", event.target.value)}><option value="">Không yêu cầu</option>{[8, 16, 32, 64].map((size) => <option key={size} value={size}>{size} GB</option>)}</select></div><div className="smartfit-field"><label htmlFor="smartfit-ssd">SSD</label><select id="smartfit-ssd" value={values.minSsdGb} onChange={(event) => update("minSsdGb", event.target.value)}><option value="">Không yêu cầu</option>{[256, 512, 1024, 2048].map((size) => <option key={size} value={size}>{size} GB</option>)}</select></div></div></fieldset>
          <div className="smartfit-field"><label htmlFor="smartfit-limit">Số gợi ý tối đa</label><select id="smartfit-limit" value={values.limit} onChange={(event) => update("limit", event.target.value as SmartFitFormValues["limit"])}><option value="3">3 lựa chọn</option><option value="4">4 lựa chọn</option><option value="5">5 lựa chọn</option></select></div>
          <button className="smartfit-primary" type="submit" disabled={loading}>{loading ? <LoaderCircle size={18} className="smartfit-spinner" aria-hidden="true" /> : <Sparkles size={18} aria-hidden="true" />}{loading ? "Đang đối chiếu cấu hình..." : "Tìm laptop phù hợp"}</button>
          {loading && <button type="button" className="smartfit-text-button" onClick={() => { cancelPending(); setNotice("Đã dừng tư vấn. Bạn có thể đổi nhu cầu và tìm lại."); }}>Dừng tư vấn</button>}
          {error && <p role="alert" className="smartfit-error"><CircleAlert size={17} aria-hidden="true" />{error}</p>}
          {notice && <p role="status" className="smartfit-muted">{notice}</p>}
        </form>
        <section className="smartfit-results" aria-labelledby="smartfit-results-heading" aria-busy={loading}>
          <div className="smartfit-section-heading"><div><p className="smartfit-eyebrow">02 / LỰA CHỌN PHÙ HỢP</p><h2 id="smartfit-results-heading" ref={resultHeading} tabIndex={-1}>{result ? `${result.recommendations.length} cấu hình cho bạn` : "Cùng tìm lựa chọn tiếp theo"}</h2></div>{result && <span>{submitted && purposeLabels[submitted.purpose]}</span>}</div>
          <div role="status" className="smartfit-sr-only">{loading ? "Đang tìm laptop theo nhu cầu của bạn." : result ? `Đã tìm thấy ${result.recommendations.length} cấu hình.` : ""}</div>
          {!result && <div className="smartfit-empty smartfit-panel">{loading ? <LoaderCircle size={36} className="smartfit-spinner" aria-hidden="true" /> : <Sparkles size={36} aria-hidden="true" />}<h3>{loading ? "Đang kiểm tra giá và cấu hình" : "Một vài lựa chọn, đúng điều bạn cần"}</h3><p>{loading ? "SmartFit đang lọc và xếp hạng theo nhu cầu đã chọn." : "Hoàn thành nhu cầu bên trái để xem lý do phù hợp và điểm cần cân nhắc của từng cấu hình."}</p><span>Ngân sách · Mục đích · Bộ nhớ · Di động</span></div>}
          {result && !result.recommendations.length && <div className="smartfit-empty smartfit-panel"><SlidersHorizontal size={36} aria-hidden="true" /><h3>Chưa có cấu hình đáp ứng đủ nhu cầu</h3><p>Thử tăng ngân sách hoặc giảm RAM/SSD tối thiểu. SmartFit giữ nguyên các yêu cầu bắt buộc của bạn.</p><Link to={ROUTES.products}>Khám phá tất cả laptop <ArrowRight size={16} aria-hidden="true" /></Link></div>}
          {result?.recommendations.map((item, index) => {
            const checked = comparedIds.includes(item.variantId);
            return <article className="smartfit-recommendation smartfit-panel" key={item.variantId}>
              <div className="smartfit-product-top"><Link to={recommendationPath(item)} tabIndex={-1} aria-hidden="true"><RecommendationImage item={item} /></Link><div className="smartfit-product-copy"><span className="smartfit-rank">LỰA CHỌN {index + 1} <span>SKU ID {item.variantId}</span></span><h3><Link to={recommendationPath(item)}>{item.name}</Link></h3><p className="smartfit-config">RAM {specValue(item.specs.ramGb, " GB")} · SSD {specValue(item.specs.ssdGb, " GB")}</p><strong className="smartfit-price">{formatVND(item.price)}</strong></div><div className="smartfit-score"><strong>{item.score}</strong><span>/100 phù hợp</span></div></div>
              <div className="smartfit-analysis"><div><h4><Check size={15} aria-hidden="true" /> Vì sao phù hợp</h4><ul>{item.reasons.map((reason, reasonIndex) => <li key={reasonIndex}>{reason}</li>)}</ul></div><div><h4><Scale size={15} aria-hidden="true" /> Cần cân nhắc</h4>{item.tradeoffs.length ? <ul>{item.tradeoffs.map((tradeoff, tradeoffIndex) => <li key={tradeoffIndex}>{tradeoff}</li>)}</ul> : <p>Chưa có điểm đánh đổi được xác định từ dữ liệu hiện có.</p>}</div></div>
              <div className="smartfit-card-actions"><label><input type="checkbox" checked={checked} disabled={!checked && comparedIds.length >= 3} onChange={() => setComparedIds((ids) => toggleComparedVariant(ids, item.variantId))} /><span>So sánh cấu hình <span className="smartfit-sr-only">{item.name}, SKU {item.variantId}</span></span></label><Link to={recommendationPath(item)}>Xem đúng cấu hình <ArrowRight size={16} aria-hidden="true" /></Link></div>
            </article>;
          })}
          {result && <div className="smartfit-method"><h3>Điểm phù hợp được tính thế nào?</h3><p>Mục đích {result.weights.purpose}% · Ngân sách {result.weights.budget}% · RAM / lưu trữ {result.weights.memory}% · Di động {result.weights.mobility}%</p><p>{result.disclaimer}</p><p>Giá được ghi nhận khi tư vấn; kiểm tra lại cấu hình và tình trạng bán tại trang chi tiết trước khi mua.</p></div>}
        </section>
      </div>
      {!!result?.recommendations.length && <div className="smartfit-comparison-summary"><Scale size={18} aria-hidden="true" /><span role="status">Đã chọn {comparedIds.length}/3 cấu hình. {comparedIds.length < 2 ? "Chọn ít nhất 2 để mở bảng so sánh." : "Bảng so sánh hiển thị bên dưới."}</span><button type="button" onClick={discussRecommendations}>Hỏi trợ lý về {compared.length || result.recommendations.length} cấu hình</button>{!!comparedIds.length && <button type="button" onClick={() => setComparedIds([])}>Bỏ chọn tất cả</button>}</div>}
      {submitted && <SmartCompare items={compared} purpose={purposeLabels[submitted.purpose]} priority={priorityLabels[submitted.priority]} />}
    </main>
  );
}
