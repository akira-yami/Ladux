import { describe, expect, it, vi } from "vitest";

vi.mock("./apiClient", () => ({ default: { get: vi.fn() } }));

import { createDiscoveryClient, type DiscoverySuggestions } from "./discoveryService";

const response: DiscoverySuggestions = { mode: "FEATURED", suggestedQueries: [], products: [] };

describe("discovery cache", () => {
    it("reuses normalized queries only within the TTL", async () => {
        let now = 0;
        const load = vi.fn().mockResolvedValue(response);
        const service = createDiscoveryClient(load, () => now, 60_000);
        const signal = new AbortController().signal;
        await service.suggest(" ASUS ", signal);
        await service.suggest("asus", signal);
        expect(load).toHaveBeenCalledOnce();
        now = 60_001;
        await service.suggest("asus", signal);
        expect(load).toHaveBeenCalledTimes(2);
    });

    it("bounds the cache and evicts the least recently used query", async () => {
        const load = vi.fn().mockResolvedValue(response);
        const service = createDiscoveryClient(load, () => 0, 60_000, 2);
        const signal = new AbortController().signal;
        await service.suggest("first", signal);
        await service.suggest("second", signal);
        await service.suggest("first", signal);
        await service.suggest("third", signal);
        await service.suggest("second", signal);
        expect(load.mock.calls.map(([query]) => query)).toEqual(["first", "second", "third", "second"]);
    });

    it("does not cache results from a cancelled request", async () => {
        const controller = new AbortController();
        const load = vi.fn().mockImplementationOnce(async () => {
            controller.abort();
            return response;
        }).mockResolvedValueOnce(response);
        const service = createDiscoveryClient(load);
        await expect(service.suggest("asus", controller.signal)).rejects.toThrow();
        await service.suggest("asus", new AbortController().signal);
        expect(load).toHaveBeenCalledTimes(2);
    });
});
