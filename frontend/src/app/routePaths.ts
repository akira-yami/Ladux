export const ROUTES = {
    home: "/",
    products: "/products",
    smartfit: "/smartfit",
    cart: "/cart",
    checkout: "/checkout",
    orders: "/orders",
    wishlist: "/wishlist",
    account: "/account",
    addresses: "/addresses",
    login: "/login",
    oauth2Success: "/auth/oauth2/success",
    register: "/register",
    about: "/about",
    contact: "/contact",
    vnpayReturn: "/payment/vnpay/return",
} as const;

export function productPath(productId: number | string): string {
    return `${ROUTES.products}/${productId}`;
}
