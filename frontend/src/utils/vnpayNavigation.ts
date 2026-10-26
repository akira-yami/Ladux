import { flushSync } from 'react-dom';
import type { NavigateFunction } from 'react-router-dom';
import { ROUTES } from '../app/routePaths';
import { requireVNPayUrl } from './paymentFlow';

export function redirectToVNPay(paymentUrl: string | null | undefined, navigate: NavigateFunction) {
    const gatewayUrl = requireVNPayUrl(paymentUrl);
    // Commit the orders screen before leaving so Back/BFCache never restores
    // a checkout whose cart has already been consumed by the order transaction.
    flushSync(() => navigate(ROUTES.orders, { replace: true }));
    window.location.assign(gatewayUrl);
}
