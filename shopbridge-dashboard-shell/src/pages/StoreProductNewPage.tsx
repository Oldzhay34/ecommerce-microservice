import { lazy, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { RemoteMount } from '../components/RemoteMount';
import { TopBar } from '../components/TopBar';
import { useSessionStore } from '../app/session/store';

const StoreProductCreate = lazy(() => import('mfe_product_create/StoreProductCreate'));

export function StoreProductNewPage() {
    const navigate = useNavigate();
    const { authToken, storeId, role, clear } = useSessionStore();

    // Remote'un beklediği StoreSession sözleşmesi (referans stabil kalsın).
    const session = useMemo(
        () =>
            authToken && storeId && role === 'STORE'
                ? ({ authToken, storeId, role: 'STORE' } as const)
                : null,
        [authToken, storeId, role],
    );

    if (!session) return null; // StoreGuard zaten '/' e yönlendirir

    const handleLogout = () => {
        clear();
        navigate('/');
    };

    return (
        <div className="min-h-screen bg-canvas">
            <TopBar identity={`Mağaza: ${storeId.slice(0, 8)}`} onLogout={handleLogout} />
            <div className="max-w-[1120px] mx-auto px-6 md:px-8 py-12">
                <button
                    onClick={() => navigate('/storedashboard')}
                    className="text-ink-muted text-sm font-medium hover:text-ink transition-colors mb-8"
                >
                    ← Mağaza Paneline Dön
                </button>
                <RemoteMount>
                    <StoreProductCreate session={session} onNavigate={navigate} />
                </RemoteMount>
            </div>
        </div>
    );
}