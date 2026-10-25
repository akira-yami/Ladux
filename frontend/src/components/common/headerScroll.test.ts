import { describe, expect, it } from "vitest";
import { createHeaderScrollTracker } from "./headerScroll";

describe("header scroll direction", () => {
    it("hides on downward travel and reveals after a deliberate upward movement", () => {
        const track = createHeaderScrollTracker();
        expect(track(80)).toEqual({ isScrolled: true, isSubHeaderVisible: false });
        expect(track(75).isSubHeaderVisible).toBe(false);
        expect(track(68).isSubHeaderVisible).toBe(true);
        expect(track(72).isSubHeaderVisible).toBe(true);
        expect(track(80).isSubHeaderVisible).toBe(false);
    });

    it("ignores small reversals and reuses the same state while scrolling", () => {
        const track = createHeaderScrollTracker();
        const hidden = track(200);
        for (const y of [199, 201, 198, 200, 196, 202, 215, 215]) {
            expect(track(y)).toBe(hidden);
        }
    });

    it("avoids shadow flicker at the old 30px threshold", () => {
        const track = createHeaderScrollTracker();
        expect(track(41).isScrolled).toBe(true);
        for (const y of [31, 29, 32, 28, 20]) expect(track(y).isScrolled).toBe(true);
        expect(track(19).isScrolled).toBe(false);
        for (const y of [28, 32, 40]) expect(track(y).isScrolled).toBe(false);
    });

    it("handles restored positions, return to top, and negative overscroll", () => {
        const track = createHeaderScrollTracker(500);
        expect(track(500)).toEqual({ isScrolled: true, isSubHeaderVisible: false });
        expect(track(29).isSubHeaderVisible).toBe(true);
        expect(track(-20)).toEqual({ isScrolled: false, isSubHeaderVisible: true });
        expect(track(0)).toEqual({ isScrolled: false, isSubHeaderVisible: true });
    });
});
