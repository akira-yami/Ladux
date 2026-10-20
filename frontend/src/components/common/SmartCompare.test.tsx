import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import type { SmartFitRecommendation } from "../../services/smartFitService";
import SmartCompare from "./SmartCompare";

const first: SmartFitRecommendation = {
  productId: 5, variantId: 51, name: "Laptop A", slug: "laptop-a", imageUrl: null, price: 19_999_000, score: 81,
  specs: { cpu: "Intel Core i7", gpu: null, ramGb: 16, ssdGb: 512, weightKg: null, battery: "50 Wh" },
  reasons: ["Đáp ứng RAM tối thiểu 16 GB"], tradeoffs: ["Chưa đủ dữ liệu về trọng lượng"],
};
const second: SmartFitRecommendation = { ...first, variantId: 52, price: 24_500_000, score: 87, specs: { ...first.specs, ramGb: 32 }, reasons: ["RAM 32 GB phù hợp đa nhiệm"], tradeoffs: [] };

const render = (items: SmartFitRecommendation[]) => renderToStaticMarkup(<MemoryRouter><SmartCompare items={items} purpose="Lập trình" priority="Giá trị / giá tiền" /></MemoryRouter>);

describe("SmartCompare", () => {
  it("only shows comparison for two or three configurations", () => {
    expect(render([first])).toBe("");
    expect(render([first, second, first, second])).toBe("");
  });

  it("reuses exact SKU prices, scores, reasons and tradeoffs from SmartFit", () => {
    const html = render([first, second]);
    expect(html).toContain("/products/5?variantId=51");
    expect(html).toContain("/products/5?variantId=52");
    expect(html).toContain("81/100");
    expect(html).toContain("87/100");
    expect(html).toContain("19.999.000");
    expect(html).toContain("24.500.000");
    expect(html).toContain(first.reasons[0]);
    expect(html).toContain(second.reasons[0]);
    expect(html).toContain(first.tradeoffs[0]);
    expect(html).toContain("Chưa đủ dữ liệu");
    expect(html).toContain("Intel Core i7");
    expect(html).toContain("50 Wh");
    expect(html).toContain('scope="row"');
    expect(html).toContain('role="region"');
  });
});
