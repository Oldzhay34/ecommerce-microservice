import { To, useNavigate} from 'react-router-dom';
import {useSessionStore} from '../app/session/store';
import {TopBar} from '../components/TopBar';
import {SectionHeader} from '../components/SectionHeader';
import {RemoteMount} from '../components/RemoteMount';
import {lazy} from 'react';

// Görev 5'te aktif kompozisyon. Görev 1'de iskelet + slotlar hazır.
const StoreProductCount = lazy(() => import('mfe_products/StoreProductCount'));
const StoreOrderMetrics = lazy(() => import('mfe_orders/StoreOrderMetrics'));
const StoreProducts = lazy(() => import('mfe_products/StoreProducts'));
const StoreOrders = lazy(() => import('mfe_orders/StoreOrders'));
const StoreReviews = lazy(() => import('mfe_reviews/StoreReviews'));

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

    return (
        <div style={{minHeight: '100vh', background: '#F4F5F7'}}>
            <TopBar onLogout={handleLogout}/>

            <main
                style={{
                    maxWidth: 1120,
                    margin: '0 auto',
                    padding: '0 24px',
                    paddingTop: 32,
                    paddingBottom: 64,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 32,
                }}
            >
                {/* 1 — MAĞAZA ÖZETİ */}
                <section>
                    <SectionHeader>Mağaza Özeti</SectionHeader>
                    <div
                        style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                            gap: 16,
                        }}
                    >
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