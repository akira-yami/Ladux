import { describe, expect, it, vi } from "vitest";

vi.mock("./apiClient", () => ({ default: { post: vi.fn() } }));
import apiClient from "./apiClient";
import { smartFitService, type SmartFitRequest } from "./smartFitService";

describe("SmartFit API contract", () => {
  it("forwards cancellation and returns the direct backend DTO", async () => {
    const response = { recommendations: [], weights: { purpose: 40, budget: 25, memory: 20, mobility: 15 }, disclaimer: "Quy tắc thử nghiệm" };
    vi.mocked(apiClient.post).mockResolvedValueOnce(response);
    const request: SmartFitRequest = { budgetMax: 25_000_000, purpose: "GAMING", priority: "PERFORMANCE", minRamGb: 16, limit: 3 };
    const controller = new AbortController();
    expect(await smartFitService.recommend(request, controller.signal)).toBe(response);
    expect(apiClient.post).toHaveBeenCalledWith("/smartfit/recommend", request, { signal: controller.signal });
  });
});
