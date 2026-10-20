import { describe, expect, it } from "vitest";
import { contextVariantIds } from "./chatbotContext";

describe("chatbot context events", () => {
    it("keeps only distinct positive catalog IDs within the API bound", () => {
        expect(contextVariantIds({ variantIds: [1, 1, 2, 3, 4, 5, 6] })).toEqual([1, 2, 3, 4, 5]);
    });

    it("rejects malformed, fractional, string, negative and out-of-range IDs", () => {
        expect(contextVariantIds({ variantIds: [null, "1", {}, -1, 0, 1.5, NaN, Infinity, 2_147_483_648, 7] })).toEqual([7]);
        expect(contextVariantIds(null)).toEqual([]);
        expect(contextVariantIds({ productIds: [1] })).toEqual([]);
    });
});
