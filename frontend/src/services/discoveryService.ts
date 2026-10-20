import apiClient from "./apiClient";

export interface SuggestionProduct {
    productId: number;
    variantId: number | null;
    name: string;
    slug: string | null;
    price: number | null;
    discountPrice: number | null;
    imageUrl: string | null;
    available: boolean;
}

export interface DiscoverySuggestions {
    mode: "BEST_SELLER" | "FEATURED" | "SEARCH";
    suggestedQueries: string[];
    products: SuggestionProduct[];
}

export type SuggestionLoader = (query: string, signal: AbortSignal) => Promise<DiscoverySuggestions>;

/** Short lived, bounded cache: suggestions never become a second source of catalog data. */
export function createDiscoveryClient(
    load: SuggestionLoader,
    now = Date.now,
    ttlMs = 60_000,
    maxEntries = 30,
) {
    const cache = new Map<string, { expiresAt: number; data: DiscoverySuggestions }>();
    return {
        async suggest(query: string, signal: AbortSignal, refresh = false): Promise<DiscoverySuggestions> {
            const clean = query.trim().slice(0, 120);
            const key = clean.toLocaleLowerCase("vi");
            signal.throwIfAborted();
            const cached = cache.get(key);
            if (!refresh && cached && cached.expiresAt > now()) {
                cache.delete(key);
                cache.set(key, cached);
                return cached.data;
            }
            cache.delete(key);
            const data = await load(clean, signal);
            signal.throwIfAborted();
            for (const [entryKey, value] of cache) {
                if (value.expiresAt <= now()) cache.delete(entryKey);
            }
            if (cache.size >= maxEntries) cache.delete(cache.keys().next().value!);
            cache.set(key, { data, expiresAt: now() + ttlMs });
            return data;
        },
    };
}

export const discoveryService = createDiscoveryClient((query, signal) =>
    apiClient.get<DiscoverySuggestions>("/discovery/suggestions", {
        params: { q: query },
        signal,
    }) as unknown as Promise<DiscoverySuggestions>,
);
