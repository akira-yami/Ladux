import { lazy, Suspense, useEffect, type ReactElement } from "react";
import { Check } from "lucide-react";
import { Link, Navigate, Outlet, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import Footer from "../components/common/Footer";
import Header from "../components/common/Header";
import AIChatbot from "../components/common/AIChatbot";
import BrandMark from "../components/common/BrandMark";
import ThemeToggle from "../components/common/ThemeToggle";
import heroArtwork from "../assets/ladux/hero.webp";
import "../styles/storefront-v2.css";
import { authService } from "../services";
import { productPath, ROUTES } from "./routePaths";
import { useStorefront } from "./StorefrontProvider";

const ProductStoreView = lazy(() => import("../pages/ProductStoreView"));
const AllProductsView = lazy(() => import("../pages/AllProductsView"));
const ProductDetailView = lazy(() => import("../pages/ProductDetailView"));
const CartView = lazy(() => import("../pages/CartView"));
const CheckoutView = lazy(() => import("../pages/CheckoutView"));
const OrdersView = lazy(() => import("../pages/OrdersView"));
const WishlistView = lazy(() => import("../pages/WishlistView"));
const AccountView = lazy(() => import("../pages/AccountView"));
const LoginView = lazy(() => import("../pages/LoginView"));
const RegisterView = lazy(() => import("../pages/RegisterView"));
const AboutView = lazy(() => import("../pages/AboutView"));
const ContactView = lazy(() => import("../pages/ContactView"));
const VNPayReturnView = lazy(() => import("../pages/VNPayReturnView"));

function RouteLoading() {
    return (
        <main className="container mx-auto flex min-h-[50vh] items-center justify-center px-6 text-sm text-slate-500">
            Đang tải giao diện...
        </main>
    );
}

function NotificationToast() {
    const { notificationMsg } = useStorefront();
    if (!notificationMsg) return null;

    return (
        <div role="status" className="fixed bottom-6 left-5 right-5 z-[60] mx-auto flex w-fit max-w-[calc(100vw-40px)] items-center gap-3 rounded-2xl border border-sky-100 bg-white px-5 py-3 text-sm font-medium text-slate-700 shadow-xl shadow-sky-100/50 animate-in fade-in slide-in-from-bottom-5">
            <Check className="h-4 w-4 text-emerald-600" />
            <span>{notificationMsg}</span>
        </div>
    );
}

function StoreLayout() {
    const {
        searchQuery,
        setSearchQuery,
        selectedCategory,
        setSelectedCategory,
        wishlistCount,
        cartCount,
        isLoggedIn,
        displayAvatar,
        userName,
    } = useStorefront();

    return (
        <div className="storefront-ui min-h-screen overflow-x-hidden selection:bg-sky-100 selection:text-sky-900">
            <NotificationToast />
            <Header
                searchQuery={searchQuery}
                setSearchQuery={setSearchQuery}
                selectedCategory={selectedCategory}
                setSelectedCategory={setSelectedCategory}
                wishlistCount={wishlistCount}
                cartCount={cartCount}
                isLoggedIn={isLoggedIn}
                userAvatar={displayAvatar}
                userName={userName}
            />
            <div className="store-content pt-[122px]">
                <Suspense fallback={<RouteLoading />}>
                    <Outlet />
                </Suspense>
            </div>
            <Footer />
            <AIChatbot />
        </div>
    );
}

function AuthLayout() {
    return (
        <div className="storefront-ui min-h-screen overflow-x-hidden selection:bg-sky-100 selection:text-sky-900">
            <NotificationToast />
            <header className="border-b border-sky-100 bg-white backdrop-blur-xl">
                <div className="container mx-auto flex h-20 items-center justify-between px-6">
                    <Link to={ROUTES.home} className="flex items-center gap-3">
                        <BrandMark />
                    </Link>
                    <ThemeToggle />
                </div>
            </header>
            <div className="auth-experience">
                <aside className="auth-story"><div><p className="section-eyebrow">YOUR NEXT CHAPTER</p><h2>Những điều hay,<br /><span>bắt đầu cùng bạn.</span></h2><p>Một tài khoản, cả thế giới công nghệ.<br />Lưu điều bạn thích, theo dõi mọi hành trình.</p></div><img src={heroArtwork} alt="Laptop Ladux với dải kính hổ phách" width={1536} height={1024} /></aside>
                <div className="auth-panel"><Suspense fallback={<RouteLoading />}><Outlet /></Suspense></div>
            </div>
        </div>
    );
}

function HomeRoute() {
    const storefront = useStorefront();
    return (
        <ProductStoreView
            filteredProducts={storefront.allDisplayProducts}
            setSelectedProduct={storefront.setSelectedProduct}
            toggleWishlist={storefront.toggleWishlist}
            addToCartCustom={storefront.addToCartCustom}
            showToast={storefront.showToast}
        />
    );
}

function ProductsRoute() {
    const storefront = useStorefront();
    return (
        <AllProductsView
            allProducts={storefront.allDisplayProducts}
            selectedCategory={storefront.selectedCategory}
            setSelectedCategory={storefront.setSelectedCategory}
            searchQuery={storefront.searchQuery}
            setSearchQuery={storefront.setSearchQuery}
            toggleWishlist={storefront.toggleWishlist}
            addToCartCustom={storefront.addToCartCustom}
            setSelectedProduct={storefront.setSelectedProduct}
        />
    );
}

function ProductDetailRoute() {
    const { id } = useParams<{ id: string }>();
    const {
        allDisplayProducts,
        isCatalogReady,
        catalogError,
        selectedProduct,
        setSelectedProduct,
        toggleWishlist,
        addToCartCustom,
        handleAddReview,
        newRating,
        setNewRating,
        newComment,
        setNewComment,
    } = useStorefront();

    const routeProduct = allDisplayProducts.find((product) => String(product.id) === id);

    useEffect(() => {
        if (routeProduct && selectedProduct?.id !== routeProduct.id) {
            setSelectedProduct(routeProduct);
        }
    }, [routeProduct, selectedProduct?.id, setSelectedProduct]);

    if (!isCatalogReady) return <RouteLoading />;

    if (!routeProduct) {
        if (catalogError) {
            return (
                <main className="container mx-auto flex min-h-[50vh] flex-col items-center justify-center gap-4 px-6 text-center">
                    <p className="text-sm text-slate-500">Không thể tải sản phẩm này.</p>
                    <Link to={ROUTES.products} className="text-sm font-bold text-sky-700 hover:underline">
                        Trở lại danh sách sản phẩm
                    </Link>
                </main>
            );
        }
        return <Navigate to={ROUTES.products} replace />;
    }

    const activeProduct = selectedProduct?.id === routeProduct.id ? selectedProduct : routeProduct;

    return (
        <ProductDetailView
            selectedProduct={activeProduct}
            toggleWishlist={toggleWishlist}
            addToCartCustom={addToCartCustom}
            handleAddReview={handleAddReview}
            newRating={newRating}
            setNewRating={setNewRating}
            newComment={newComment}
            setNewComment={setNewComment}
        />
    );
}

function CartRoute() {
    const { setSelectedProduct } = useStorefront();
    return <CartView setSelectedProduct={setSelectedProduct} />;
}

function CheckoutRoute() {
    const { setSelectedOrderId, showToast } = useStorefront();
    return <CheckoutView setSelectedAddressOrderId={setSelectedOrderId} showToast={showToast} />;
}

function OrdersRoute() {
    const { selectedOrderId, setSelectedOrderId, showToast } = useStorefront();
    return (
        <OrdersView
            selectedOrderId={selectedOrderId}
            setSelectedAddressOrderId={setSelectedOrderId}
            showToast={showToast}
        />
    );
}

function WishlistRoute() {
    const { setSelectedProduct } = useStorefront();
    return <WishlistView setSelectedProduct={setSelectedProduct} />;
}

function AccountRoute({ view }: { view: "account" | "addresses" }) {
    const { displayAvatar, setUserAvatar, userName, wishlistCount, handleLogout, showToast } = useStorefront();
    return (
        <AccountView
            currentView={view}
            userAvatar={displayAvatar}
            setUserAvatar={setUserAvatar}
            userFullName={userName || "Thành viên LADUX"}
            wishlistCount={wishlistCount}
            handleLogout={handleLogout}
            showToast={showToast}
        />
    );
}

function ContactRoute() {
    const { showToast } = useStorefront();
    return <ContactView showToast={showToast} />;
}

function RequireAuth({ children }: { children: ReactElement }) {
    const location = useLocation();
    const { isAuthReady, isLoggedIn } = useStorefront();

    if (!isAuthReady) return <RouteLoading />;

    if (!isLoggedIn) {
        return (
            <Navigate
                to={ROUTES.login}
                replace
                state={{ from: `${location.pathname}${location.search}` }}
            />
        );
    }

    return children;
}

function LoginRoute() {
    const navigate = useNavigate();
    const location = useLocation();
    const { showToast } = useStorefront();
    const from = (location.state as { from?: string } | null)?.from || ROUTES.account;

    return (
        <LoginView
            onLoginSuccess={() => {
                navigate(from, { replace: true });
                showToast("Đăng nhập thành công! Phiên làm việc đã được khôi phục.");
            }}
            onGoRegister={() => navigate(ROUTES.register)}
            onBack={() => navigate(ROUTES.home)}
            oauthReturnTo={from}
        />
    );
}

function OAuth2SuccessRoute() {
    const navigate = useNavigate();
    const { isAuthReady, isLoggedIn, showToast } = useStorefront();

    useEffect(() => {
        if (!isAuthReady) return;

        if (!isLoggedIn) {
            authService.clearOAuth2ReturnTo();
            navigate(`${ROUTES.login}?oauth2Error=true&reason=session_not_established`, { replace: true });
            return;
        }

        const returnTo = authService.consumeOAuth2ReturnTo();
        navigate(returnTo, { replace: true });
        showToast("Đăng nhập bằng Google thành công!");
    }, [isAuthReady, isLoggedIn, navigate, showToast]);

    return (
        <main className="flex min-h-[70vh] items-center justify-center px-6">
            <div className="text-center">
                <div className="mx-auto mb-4 h-9 w-9 animate-spin rounded-full border-2 border-sky-100 border-t-sky-600" />
                <p className="text-sm font-semibold text-slate-600">Đang hoàn tất đăng nhập Google...</p>
            </div>
        </main>
    );
}

function RegisterRoute() {
    const navigate = useNavigate();
    const { handleRegister } = useStorefront();
    return (
        <RegisterView
            onRegister={handleRegister}
            onGoLogin={() => navigate(ROUTES.login)}
            onBack={() => navigate(ROUTES.home)}
        />
    );
}

export default function AppRouter() {
    return (
        <Routes>
            <Route element={<AuthLayout />}>
                <Route path={ROUTES.login} element={<LoginRoute />} />
                <Route path={ROUTES.oauth2Success} element={<OAuth2SuccessRoute />} />
                <Route path={ROUTES.register} element={<RegisterRoute />} />
            </Route>

            <Route element={<StoreLayout />}>
                <Route index element={<HomeRoute />} />
                <Route path={ROUTES.products} element={<ProductsRoute />} />
                <Route path={productPath(":id")} element={<ProductDetailRoute />} />
                <Route path={ROUTES.cart} element={<RequireAuth><CartRoute /></RequireAuth>} />
                <Route path={ROUTES.checkout} element={<RequireAuth><CheckoutRoute /></RequireAuth>} />
                <Route path={ROUTES.orders} element={<RequireAuth><OrdersRoute /></RequireAuth>} />
                <Route path={ROUTES.wishlist} element={<RequireAuth><WishlistRoute /></RequireAuth>} />
                <Route path={ROUTES.account} element={<RequireAuth><AccountRoute view="account" /></RequireAuth>} />
                <Route path={ROUTES.addresses} element={<RequireAuth><AccountRoute view="addresses" /></RequireAuth>} />
                <Route path={ROUTES.about} element={<AboutView />} />
                <Route path={ROUTES.contact} element={<ContactRoute />} />
                <Route path={ROUTES.vnpayReturn} element={<VNPayReturnView />} />
                <Route path="*" element={<Navigate to={ROUTES.home} replace />} />
            </Route>
        </Routes>
    );
}
