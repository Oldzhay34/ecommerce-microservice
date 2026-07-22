import {
    createBrowserRouter,
    Navigate,
    type RouteObject,
} from 'react-router-dom';
import { type PropsWithChildren } from 'react';
import { useSessionStore } from '../app/session/store';
import { StoreDashboardPage } from '../pages/StoreDashboardPage';
import { StoreProductNewPage } from '../pages/StoreProductNewPage';
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
    // yakalar, session'ı doldurur ve /storedashboard'a geçer.
    { path: '/', element: <TokenCatcher /> },
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