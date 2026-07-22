import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ProductDetail from './expose/ProductDetail';
import type { CustomerSession } from './types';
import './index.css';

/**
 * Standalone dev entry — YALNIZCA `npm run dev` içindir.
 * Shell tüketiminde bu dosya çalışmaz; QueryClientProvider shell'e aittir.
 */
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
        },
    },
});

/** Sahte oturum — token boş bırakılabilir, ürün sorgusu public'tir. */
const devSession: CustomerSession = {
    authToken: '',
    userId: '00000000-0000-0000-0000-000000000000',
    role: 'CUSTOMER',
};

/** Sabit productId — kendi ortamınızdaki bir UUID ile değiştirin. */
const DEV_PRODUCT_ID = 'eee829bb-0000-0000-0000-000000000000';

function DevApp() {
    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#F4F5F7', padding: 24 }}>
            <div style={{ maxWidth: 1120, margin: '0 auto' }}>
                <div style={{ fontSize: 20, fontWeight: 700, marginBottom: 20 }}>
                    <span style={{ color: '#000000' }}>Shop</span>
                    <span style={{ color: '#1D4ED8' }}>Bridge</span>
                </div>
                <ProductDetail
                    session={devSession}
                    productId={DEV_PRODUCT_ID}
                    onNavigate={(path) => {
                        console.log('[dev] onNavigate →', path);
                    }}
                />
            </div>
        </div>
    );
}

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <DevApp />
        </QueryClientProvider>
    </React.StrictMode>,
);