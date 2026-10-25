import { useEffect, useState } from "react";
import { createHeaderScrollTracker } from "./headerScroll";

export function useHeaderScroll() {
    const [state, setState] = useState({ isScrolled: false, isSubHeaderVisible: true });

    useEffect(() => {
        const track = createHeaderScrollTracker(window.scrollY);
        let previous = track(window.scrollY);
        let rafId = 0;
        setState(previous);

        const onScroll = () => {
            if (rafId) return;
            rafId = requestAnimationFrame(() => {
                rafId = 0;
                const next = track(window.scrollY);
                if (next !== previous) {
                    previous = next;
                    setState(next);
                }
            });
        };

        window.addEventListener("scroll", onScroll, { passive: true });
        return () => {
            window.removeEventListener("scroll", onScroll);
            if (rafId) cancelAnimationFrame(rafId);
        };
    }, []);

    return state;
}
