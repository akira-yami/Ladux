import { Link } from "react-router-dom";
import type { SmartFitRecommendation } from "../../services/smartFitService";
import { formatVND } from "../../types";
import { recommendationPath, specValue } from "../../utils/smartFit";

export default function SmartCompare({ items, purpose, priority }: { items: SmartFitRecommendation[]; purpose: string; priority: string }) {
  if (items.length < 2 || items.length > 3) return null;
  const rows: { label: string; value: (item: SmartFitRecommendation) => string }[] = [
    { label: "Giá cấu hình", value: (item) => formatVND(item.price) },
    { label: "Điểm phù hợp", value: (item) => `${item.score}/100` },
    { label: "CPU", value: (item) => specValue(item.specs.cpu) },
    { label: "GPU", value: (item) => specValue(item.specs.gpu) },
    { label: "RAM", value: (item) => specValue(item.specs.ramGb, " GB") },
    { label: "SSD", value: (item) => specValue(item.specs.ssdGb, " GB") },
    { label: "Trọng lượng", value: (item) => specValue(item.specs.weightKg, " kg") },
    { label: "Pin", value: (item) => specValue(item.specs.battery) },
  ];
  return (
    <section className="smartfit-compare smartfit-panel" aria-labelledby="smart-compare-heading">
      <div className="smartfit-section-heading"><div><p className="smartfit-eyebrow">SMART COMPARE</p><h2 id="smart-compare-heading">Cân nhắc cùng một nhu cầu</h2></div><span>{items.length} cấu hình</span></div>
      <p className="smartfit-muted">{purpose} · Ưu tiên {priority.toLocaleLowerCase("vi-VN")}. Điểm, lý do và đánh đổi giữ nguyên từ kết quả SmartFit; thông số CPU/GPU không đại diện cho benchmark.</p>
      <div className="smartfit-table-scroll" role="region" aria-label="Bảng so sánh laptop, cuộn ngang để xem đủ" tabIndex={0}>
        <table>
          <caption className="smartfit-sr-only">So sánh chính xác các SKU đã chọn theo nhu cầu {purpose}</caption>
          <thead><tr><th scope="col">Tiêu chí</th>{items.map((item) => <th key={item.variantId} scope="col"><Link to={recommendationPath(item)}>{item.name}</Link><small>SKU ID {item.variantId}</small></th>)}</tr></thead>
          <tbody>
            {rows.map((row) => <tr key={row.label}><th scope="row">{row.label}</th>{items.map((item) => <td key={item.variantId}>{row.value(item)}</td>)}</tr>)}
            <tr className="smartfit-reasons-row"><th scope="row">Lý do phù hợp</th>{items.map((item) => <td key={item.variantId}><ul>{item.reasons.map((reason, index) => <li key={index}>{reason}</li>)}</ul></td>)}</tr>
            <tr className="smartfit-tradeoffs-row"><th scope="row">Điểm đánh đổi</th>{items.map((item) => <td key={item.variantId}>{item.tradeoffs.length ? <ul>{item.tradeoffs.map((reason, index) => <li key={index}>{reason}</li>)}</ul> : "Chưa có điểm đánh đổi được xác định từ dữ liệu hiện có."}</td>)}</tr>
          </tbody>
        </table>
      </div>
      <p className="smartfit-muted">Giá và tình trạng bán có thể thay đổi. Mở chi tiết để kiểm tra đúng cấu hình trước khi mua.</p>
    </section>
  );
}
