import type { DiscoverySuggestions, SuggestionLoader } from "../../services/discoveryService";

export const SEARCH_DEBOUNCE_MS = 275;

export type SuggestionState = {
    query: string;
    status: "loading" | "ready" | "error";
    data: DiscoverySuggestions | null;
};

/** Cancellation and a sequence guard both matter: a transport may complete after abort. */
export class SuggestionRequests {
    private sequence = 0;
    private abort: AbortController | null = null;
    private timer: ReturnType<typeof setTimeout> | null = null;

    constructor(private readonly load: SuggestionLoader) {}

    cancel() {
        this.sequence += 1;
        if (this.timer !== null) clearTimeout(this.timer);
        this.timer = null;
        this.abort?.abort();
        this.abort = null;
    }

    search(query: string, publish: (state: SuggestionState) => void) {
        this.cancel();
        const clean = query.trim();
        const sequence = this.sequence;
        const abort = new AbortController();
        this.abort = abort;
        publish({ query: clean, status: "loading", data: null });
        this.timer = setTimeout(async () => {
            this.timer = null;
            try {
                const data = await this.load(clean, abort.signal);
                if (!abort.signal.aborted && sequence === this.sequence) {
                    publish({ query: clean, status: "ready", data });
                }
            } catch {
                if (!abort.signal.aborted && sequence === this.sequence) {
                    publish({ query: clean, status: "error", data: null });
                }
            }
        }, clean ? SEARCH_DEBOUNCE_MS : 0);
    }
}

export function nextSuggestionIndex(current: number, count: number, direction: 1 | -1): number {
    if (!count) return -1;
    if (current < 0) return direction === 1 ? 0 : count - 1;
    return (current + direction + count) % count;
}

export function discoveryPath(label: string): string | null {
    const normalized = label.toLocaleLowerCase("vi");
    if (normalized.includes("gaming")) return "/smartfit?purpose=GAMING&budgetMax=25000000";
    if (normalized.includes("sinh viên") || normalized.includes("văn phòng")) return "/smartfit?purpose=OFFICE";
    if (normalized.includes("mỏng nhẹ")) return "/smartfit?priority=MOBILITY";
    return null;
}

export function suggestionsHeading(mode: DiscoverySuggestions["mode"]): string {
    if (mode === "BEST_SELLER") return "Bán chạy trong 30 ngày";
    if (mode === "FEATURED") return "Sản phẩm nổi bật";
    return "Laptop phù hợp";
}
