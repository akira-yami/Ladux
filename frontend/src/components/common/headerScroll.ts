export interface HeaderScrollState {
    isScrolled: boolean;
    isSubHeaderVisible: boolean;
}

// Accumulate deliberate movement in one direction; tiny touchpad reversals
// should not restart the navigation animation on every scroll event.
export function createHeaderScrollTracker(initialY = 0) {
    let lastY = Math.max(0, initialY);
    let travel = 0;
    let state: HeaderScrollState = {
        isScrolled: lastY > 40,
        isSubHeaderVisible: lastY <= 60,
    };

    return (scrollY: number): HeaderScrollState => {
        const y = Math.max(0, scrollY);
        const delta = y - lastY;
        lastY = y;
        if (delta !== 0) {
            travel = Math.sign(delta) === Math.sign(travel) ? travel + delta : delta;
        }

        const isScrolled = y > 40 ? true : y < 20 ? false : state.isScrolled;
        let isSubHeaderVisible = state.isSubHeaderVisible;
        if (y <= 30 || travel <= -12) isSubHeaderVisible = true;
        else if (y > 60 && travel >= 12) isSubHeaderVisible = false;

        if (isScrolled !== state.isScrolled || isSubHeaderVisible !== state.isSubHeaderVisible) {
            state = { isScrolled, isSubHeaderVisible };
        }
        return state;
    };
}
