import {
    createBrowserRouter,
    Navigate,
    type RouteObject,
} from 'react-router-dom';
import { type PropsWithChildren } from 'react';
import { useSessionStore } from '../app/session/store';
import { StoreDashboardPage } from '../pages/StoreDashboardPage';
import { StoreProductNewPage } from '../pages/StoreProductNewPage';
import { DashboardPage } from '../pages/DashboardPage';
import { ProductDetailPage } from '../pages/ProductDetailPage';
import { TokenCatcher } from '../components/TokenCatcher';

/** STORE rolü guard'ı. Rol uygun değilse köke yönlendirir. */
function StoreGuard({ children }: PropsWithChildren) {
    const { authToken, role } = useSessionStore();
    if (!authToken || role !== 'STORE') {
        return <Navigate to="/" replace />;
    }
    return <>{children}</>;
}

const routes: RouteObject[] = [
    // Login projesi buraya (?token=xxx ile) yönlendirir; TokenCatcher token'ı
    // yakalar, session'ı doldurur ve role'e göre /storedashboard veya /dashboard'a geçer.
    { path: '/', element: <TokenCatcher /> },
    { path: '/dashboard', element: <DashboardPage /> },
    // Ürün detayı public'tir (backend @PreAuthorize kullanmaz) — CUSTOMER guard yok.
    { path: '/product/:productId', element: <ProductDetailPage /> },
    // Sepet, DashboardPage içindeki CartWidget'ta gösterilir; onNavigate('/cart') sözleşmesi için ayrı bir gerçek route.
    { path: '/cart', element: <DashboardPage /> },
    {
        path: '/storedashboard',
        element: (
            <StoreGuard>
                <StoreDashboardPage />
            </StoreGuard>
        ),
    },
    {
        path: '/store/products/new',
        element: (
            <StoreGuard>
                <StoreProductNewPage />
            </StoreGuard>
        ),
    },
    { path: '*', element: <Navigate to="/" replace /> },
];

export const router = createBrowserRouter(routes);
