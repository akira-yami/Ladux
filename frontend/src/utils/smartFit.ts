import { productPath } from "../app/routePaths";
import type { SmartFitPriority, SmartFitPurpose, SmartFitRecommendation, SmartFitRequest } from "../services/smartFitService";

export const purposeLabels: Record<SmartFitPurpose, string> = {
  PROGRAMMING: "Lập trình",
  GAMING: "Gaming",
  DESIGN: "Đồ họa",
  OFFICE: "Văn phòng / học tập",
};

export const priorityLabels: Record<SmartFitPriority, string> = {
  PERFORMANCE: "Hiệu năng",
  VALUE: "Giá trị / giá tiền",
  MOBILITY: "Pin / di động",
};

export interface SmartFitFormValues {
  budgetMax: string;
  purpose: SmartFitPurpose;
  priority: SmartFitPriority;
  minRamGb: string;
  minSsdGb: string;
  limit: "3" | "4" | "5";
}

export const defaultSmartFitValues: SmartFitFormValues = {
  budgetMax: "30000000", purpose: "PROGRAMMING", priority: "VALUE", minRamGb: "", minSsdGb: "", limit: "3",
};

export function readSmartFitParams(params: URLSearchParams): SmartFitFormValues {
  const purpose = params.get("purpose");
  const priority = params.get("priority");
  const budget = Number(params.get("budgetMax"));
  return {
    ...defaultSmartFitValues,
    ...(purpose && Object.prototype.hasOwnProperty.call(purposeLabels, purpose) ? { purpose: purpose as SmartFitPurpose } : {}),
    ...(priority && Object.prototype.hasOwnProperty.call(priorityLabels, priority) ? { priority: priority as SmartFitPriority } : {}),
    ...(Number.isSafeInteger(budget) && budget > 0 && budget <= 1_000_000_000 ? { budgetMax: String(budget) } : {}),
  };
}

export function buildSmartFitRequest(values: SmartFitFormValues): SmartFitRequest | null {
  const budgetMax = Number(values.budgetMax);
  if (!Number.isSafeInteger(budgetMax) || budgetMax <= 0 || budgetMax > 1_000_000_000) return null;
  const minRamGb = values.minRamGb ? Number(values.minRamGb) : undefined;
  const minSsdGb = values.minSsdGb ? Number(values.minSsdGb) : undefined;
  if (minRamGb !== undefined && ![8, 16, 32, 64].includes(minRamGb)) return null;
  if (minSsdGb !== undefined && ![256, 512, 1024, 2048].includes(minSsdGb)) return null;
  if (!Object.prototype.hasOwnProperty.call(purposeLabels, values.purpose) || !Object.prototype.hasOwnProperty.call(priorityLabels, values.priority)) return null;
  const limit = Number(values.limit);
  if (limit !== 3 && limit !== 4 && limit !== 5) return null;
  return { budgetMax, purpose: values.purpose, priority: values.priority, limit, ...(minRamGb === undefined ? {} : { minRamGb }), ...(minSsdGb === undefined ? {} : { minSsdGb }) };
}

export function toggleComparedVariant(ids: number[], variantId: number): number[] {
  if (ids.includes(variantId)) return ids.filter((id) => id !== variantId);
  return ids.length < 3 ? [...ids, variantId] : ids;
}

export function selectedRecommendations(recommendations: SmartFitRecommendation[], ids: number[]): SmartFitRecommendation[] {
  return ids.flatMap((id) => recommendations.find((item) => item.variantId === id) ?? []);
}

export function recommendationPath(item: Pick<SmartFitRecommendation, "productId" | "variantId">): string {
  return `${productPath(item.productId)}?variantId=${item.variantId}`;
}

/** Honor a deep-linked SKU only when it belongs to this product and is active. */
export function resolveDetailVariantId(variants: { id: number; isActive: boolean }[], query: string | null): number | undefined {
  if (!query || !/^\d+$/.test(query)) return undefined;
  const id = Number(query);
  return Number.isSafeInteger(id) && id > 0 && variants.some((variant) => variant.id === id && variant.isActive) ? id : undefined;
}

export function specValue(value: string | number | null | undefined, unit = ""): string {
  if (value == null || (typeof value === "string" && !value.trim()) || (typeof value === "number" && !Number.isFinite(value))) return "Chưa đủ dữ liệu";
  return `${value}${unit}`;
}
