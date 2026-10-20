import apiClient from "./apiClient";

export type SmartFitPurpose = "PROGRAMMING" | "GAMING" | "DESIGN" | "OFFICE";
export type SmartFitPriority = "PERFORMANCE" | "VALUE" | "MOBILITY";

export interface SmartFitRequest {
  budgetMax: number;
  purpose: SmartFitPurpose;
  priority: SmartFitPriority;
  minRamGb?: number;
  minSsdGb?: number;
  limit: 3 | 4 | 5;
}

export interface SmartFitRecommendation {
  productId: number;
  variantId: number;
  name: string;
  slug: string | null;
  imageUrl: string | null;
  price: number;
  score: number;
  reasons: string[];
  tradeoffs: string[];
  specs: {
    cpu: string | null;
    gpu: string | null;
    ramGb: number | null;
    ssdGb: number | null;
    weightKg: number | null;
    battery: string | null;
  };
}

export interface SmartFitResponse {
  recommendations: SmartFitRecommendation[];
  weights: { purpose: number; budget: number; memory: number; mobility: number };
  disclaimer: string;
}

export const smartFitService = {
  recommend: (request: SmartFitRequest, signal?: AbortSignal): Promise<SmartFitResponse> =>
    apiClient.post<SmartFitResponse, SmartFitResponse>("/smartfit/recommend", request, { signal }),
};
