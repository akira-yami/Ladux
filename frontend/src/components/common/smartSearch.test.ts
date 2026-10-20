import { afterEach, describe, expect, it, vi } from "vitest";
import type { DiscoverySuggestions } from "../../services/discoveryService";
import { discoveryPath, nextSuggestionIndex, SEARCH_DEBOUNCE_MS, SuggestionRequests, suggestionsHeading } from "./smartSearch";

const response: DiscoverySuggestions = { mode: "SEARCH", suggestedQueries: [], products: [] };

afterEach(() => vi.useRealTimers());

describe("suggestion requests", () => {
    it("debounces typing and cancels an older scheduled query", async () => {
        vi.useFakeTimers();
        const load = vi.fn().mockResolvedValue(response);
        const publish = vi.fn();
        const requests = new SuggestionRequests(load);
        requests.search("thi", publish);
        await vi.advanceTimersByTimeAsync(200);
        requests.search("thinkpad", publish);
        await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS - 1);
        expect(load).not.toHaveBeenCalled();
        await vi.advanceTimersByTimeAsync(1);
        expect(load).toHaveBeenCalledOnce();
        expect(load.mock.calls[0][0]).toBe("thinkpad");
        expect(publish).toHaveBeenLastCalledWith({ query: "thinkpad", status: "ready", data: response });
    });

    it("aborts in-flight requests and rejects stale results even when transport ignores abort", async () => {
        vi.useFakeTimers();
        let resolveFirst!: (value: DiscoverySuggestions) => void;
        const load = vi.fn()
            .mockImplementationOnce(() => new Promise<DiscoverySuggestions>(resolve => { resolveFirst = resolve; }))
            .mockResolvedValueOnce(response);
        const publish = vi.fn();
        const requests = new SuggestionRequests(load);
        requests.search("old", publish);
        await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
        const signal = load.mock.calls[0][1] as AbortSignal;
        requests.search("new", publish);
        expect(signal.aborted).toBe(true);
        await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
        resolveFirst({ ...response, suggestedQueries: ["stale"] });
        await Promise.resolve();
        expect(publish).toHaveBeenLastCalledWith({ query: "new", status: "ready", data: response });
        expect(publish.mock.calls.some(([state]) => state.data?.suggestedQueries.includes("stale"))).toBe(false);
    });

    it("loads empty-query discovery immediately and does not emit errors after closing", async () => {
        vi.useFakeTimers();
        let reject!: (reason: Error) => void;
        const load = vi.fn(() => new Promise<DiscoverySuggestions>((_, fail) => { reject = fail; }));
        const publish = vi.fn();
        const requests = new SuggestionRequests(load);
        requests.search("", publish);
        await vi.advanceTimersByTimeAsync(0);
        expect(load).toHaveBeenCalledOnce();
        requests.cancel();
        reject(new Error("cancelled"));
        await Promise.resolve();
        expect(publish).toHaveBeenCalledOnce();
        expect(publish).toHaveBeenLastCalledWith({ query: "", status: "loading", data: null });
    });

    it("exposes an error state and permits an explicit retry", async () => {
        vi.useFakeTimers();
        const load = vi.fn().mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce(response);
        const publish = vi.fn();
        const requests = new SuggestionRequests(load);
        requests.search("asus", publish);
        await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
        expect(publish).toHaveBeenLastCalledWith({ query: "asus", status: "error", data: null });
        requests.search("asus", publish);
        await vi.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
        expect(publish).toHaveBeenLastCalledWith({ query: "asus", status: "ready", data: response });
    });
});

describe("search interaction contracts", () => {
    it("wraps keyboard selection and handles empty options", () => {
        expect(nextSuggestionIndex(-1, 4, 1)).toBe(0);
        expect(nextSuggestionIndex(-1, 4, -1)).toBe(3);
        expect(nextSuggestionIndex(3, 4, 1)).toBe(0);
        expect(nextSuggestionIndex(0, 4, -1)).toBe(3);
        expect(nextSuggestionIndex(-1, 0, 1)).toBe(-1);
    });

    it("labels fallback catalog data honestly", () => {
        expect(suggestionsHeading("FEATURED")).toBe("Sản phẩm nổi bật");
        expect(suggestionsHeading("BEST_SELLER")).toBe("Bán chạy trong 30 ngày");
    });

    it("routes discovery chips to functional SmartFit inputs instead of literal catalog searches", () => {
        expect(discoveryPath("Gaming dưới 25 triệu")).toBe("/smartfit?purpose=GAMING&budgetMax=25000000");
        expect(discoveryPath("Laptop cho sinh viên")).toBe("/smartfit?purpose=OFFICE");
        expect(discoveryPath("Văn phòng")).toBe("/smartfit?purpose=OFFICE");
        expect(discoveryPath("Mỏng nhẹ")).toBe("/smartfit?priority=MOBILITY");
        expect(discoveryPath("ThinkPad")).toBeNull();
    });
});
