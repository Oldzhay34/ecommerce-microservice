import { lazy, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { RemoteMount } from '../components/RemoteMount';
import { useSessionStore } from '../app/session/store';

const StoreProductCreate = lazy(() => import('mfe_product_create/StoreProductCreate'));

export function StoreProductNewPage() {
    const navigate = useNavigate();
    const { authToken, storeId, role } = useSessionStore();

    // Remote'un beklediği StoreSession sözleşmesi (referans stabil kalsın).
    const session = useMemo(
        () =>
            authToken && storeId && role === 'STORE'
                ? ({ authToken, storeId, role: 'STORE' } as const)
                : null,
        [authToken, storeId, role],
    );

    if (!session) return null; // StoreGuard zaten '/' e yönlendirir

    return (
        <div className="min-h-screen bg-canvas">
            <div className="max-w-[1120px] mx-auto px-6 py-8">
                <RemoteMount>
                    <StoreProductCreate session={session} onNavigate={navigate} />
                </RemoteMount>
            </div>
        </div>
    );
}