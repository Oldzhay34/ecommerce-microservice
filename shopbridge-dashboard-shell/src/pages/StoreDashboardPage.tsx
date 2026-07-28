import { To, useNavigate} from 'react-router-dom';
import {useSessionStore} from '../app/session/store';
import {TopBar} from '../components/TopBar';
import {SectionHeader} from '../components/SectionHeader';
import {RemoteMount} from '../components/RemoteMount';
import {lazy} from 'react';

// Store* bileşenleri customer Widget'ını sunan mfe_products/mfe_orders/mfe_reviews'da değil,
// ayrı "_store" remote'larında (bkz. vite.config.ts).
const StoreProductCount = lazy(() => import('mfe_products_store/StoreProductCount'));
const StoreOrderMetrics = lazy(() => import('mfe_orders_store/StoreOrderMetrics'));
const StoreProducts = lazy(() => import('mfe_products_store/StoreProducts'));
const StoreOrders = lazy(() => import('mfe_orders_store/StoreOrders'));
const StoreReviews = lazy(() => import('mfe_reviews_store/StoreReviews'));

export function StoreDashboardPage() {
    const navigate = useNavigate();
    const {authToken, storeId, clear} = useSessionStore();

    const handleLogout = () => {
        clear();
        navigate('/');
    };

    // Guard StoreDashboardPage'e router'da uygulanır; burada tip güvencesi.
    if (!authToken || !storeId) {
        return null;
    }

    const session: StoreSession = {authToken, storeId, role: 'STORE'};
    const shortId = storeId.slice(0, 8);

    return (
        <div className="min-h-screen bg-canvas">
            <TopBar identity={`Mağaza: ${shortId}`} onLogout={handleLogout} />

            <main className="max-w-6xl mx-auto px-6 md:px-8 pt-12 pb-24 flex flex-col gap-14">
                {/* 1 — MAĞAZA ÖZETİ */}
                <section>
                    <SectionHeader>Mağaza Özeti</SectionHeader>
                    <div className="grid gap-5" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
                        <RemoteMount>
                            <StoreProductCount session={session}/>
                        </RemoteMount>
                        <RemoteMount>
                            <StoreOrderMetrics session={session}/>
                        </RemoteMount>
                    </div>
                </section>

                {/* 2 — ÜRÜNLERİM */}
                <section>
                    <SectionHeader>Ürünlerim</SectionHeader>
                    <RemoteMount>
                        <StoreProducts
                            session={session}
                            onNavigate={(path: To) => navigate(path)}
                        />
                    </RemoteMount>
                </section>

                {/* 3 — SİPARİŞLER */}
                <section>
                    <SectionHeader>Siparişler</SectionHeader>
                    <RemoteMount>
                        <StoreOrders session={session} />
                    </RemoteMount>
                </section>

                {/* 4 — DEĞERLENDİRMELER */}
                <section>
                    <SectionHeader>Değerlendirmeler</SectionHeader>
                    <RemoteMount>
                        <StoreReviews session={session} />
                    </RemoteMount>
                </section>
            </main>
        </div>
    );
}