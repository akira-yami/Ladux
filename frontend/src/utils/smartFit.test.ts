import { describe, expect, it } from "vitest";
import type { SmartFitRecommendation } from "../services/smartFitService";
import { buildSmartFitRequest, defaultSmartFitValues, readSmartFitParams, recommendationPath, resolveDetailVariantId, selectedRecommendations, specValue, toggleComparedVariant } from "./smartFit";

describe("SmartFit form and quick links", () => {
  it("uses valid quick-link criteria and ignores unsupported or invalid values", () => {
    expect(readSmartFitParams(new URLSearchParams("purpose=GAMING&budgetMax=25000000&priority=PERFORMANCE"))).toMatchObject({ purpose: "GAMING", budgetMax: "25000000", priority: "PERFORMANCE" });
    for (const value of ["-1", "Infinity", "1000000001", "1.5", ""]) {
      expect(readSmartFitParams(new URLSearchParams(`purpose=toString&priority=unknown&budgetMax=${value}`))).toEqual(defaultSmartFitValues);
    }
  });

  it("sends numeric limits and omits optional hard constraints until chosen", () => {
    expect(buildSmartFitRequest(defaultSmartFitValues)).toEqual({ budgetMax: 30_000_000, purpose: "PROGRAMMING", priority: "VALUE", limit: 3 });
    expect(buildSmartFitRequest({ ...defaultSmartFitValues, minRamGb: "16", minSsdGb: "1024", limit: "5" })).toMatchObject({ minRamGb: 16, minSsdGb: 1024, limit: 5 });
  });

  it("rejects missing, fractional, nonfinite and over-limit budgets and unsupported memory values", () => {
    for (const budgetMax of ["", "0", "-10", "3.1", "NaN", "Infinity", "1000000001"]) {
      expect(buildSmartFitRequest({ ...defaultSmartFitValues, budgetMax })).toBeNull();
    }
    expect(buildSmartFitRequest({ ...defaultSmartFitValues, minRamGb: "0" })).toBeNull();
    expect(buildSmartFitRequest({ ...defaultSmartFitValues, minSsdGb: "500" })).toBeNull();
  });
});

describe("exact-SKU comparison and detail navigation", () => {
  const items = [{ productId: 10, variantId: 101, price: 20_000_000 }, { productId: 10, variantId: 102, price: 25_000_000 }, { productId: 20, variantId: 201, price: 24_000_000 }] as SmartFitRecommendation[];

  it("preserves distinct configurations of the same product without recalculating prices", () => {
    expect(selectedRecommendations(items, [102, 101, 999])).toEqual([items[1], items[0]]);
    expect(recommendationPath(items[1])).toBe("/products/10?variantId=102");
  });

  it("caps comparison at three variants and allows deselection at capacity", () => {
    expect(toggleComparedVariant([101, 102, 201], 301)).toEqual([101, 102, 201]);
    expect(toggleComparedVariant([101, 102, 201], 102)).toEqual([101, 201]);
    expect(toggleComparedVariant([101], 102)).toEqual([101, 102]);
  });

  it("only honors detail links for active SKUs belonging to the viewed product", () => {
    const variants = [{ id: 101, isActive: true }, { id: 102, isActive: false }];
    expect(resolveDetailVariantId(variants, "101")).toBe(101);
    for (const query of ["102", "201", "101.0", "-1", "Infinity", null]) expect(resolveDetailVariantId(variants, query)).toBeUndefined();
  });

  it("keeps raw CPU/GPU/battery text and marks absent measurements explicitly", () => {
    expect(specValue("Intel Core Ultra 7 155H")).toBe("Intel Core Ultra 7 155H");
    expect(specValue("3-cell 50Wh")).toBe("3-cell 50Wh");
    expect(specValue(null, " GB")).toBe("Chưa đủ dữ liệu");
    expect(specValue("  ")).toBe("Chưa đủ dữ liệu");
    expect(specValue(NaN)).toBe("Chưa đủ dữ liệu");
    expect(specValue(1.4, " kg")).toBe("1.4 kg");
  });
});
