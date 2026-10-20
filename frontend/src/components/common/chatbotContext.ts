/** Events are local UI hints; the backend reloads and authorizes the public catalog rows. */
export function contextVariantIds(detail: unknown): number[] {
    if (!detail || typeof detail !== "object" || !("variantIds" in detail) || !Array.isArray(detail.variantIds)) return [];
    return [...new Set(detail.variantIds.filter((value): value is number =>
        typeof value === "number" && Number.isSafeInteger(value) && value > 0 && value <= 2_147_483_647,
    ))].slice(0, 5);
}
