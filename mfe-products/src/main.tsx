import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import StoreProducts from './expose/StoreProducts';
import StoreProductCount from './expose/StoreProductCount';
import type { StoreSession } from './types';
import './index.css';

// Standalone: yerel QueryClient + dev session ile tek başına çalışır.
const queryClient = new QueryClient();

const devSession: StoreSession = {
    authToken: import.meta.env.VITE_DEV_TOKEN ?? 'dev-token',
    storeId: import.meta.env.VITE_DEV_STORE_ID ?? 'dev-store-id',
    role: 'STORE',
};

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <div style={{ maxWidth: 1120, margin: '0 auto', padding: 24, background: '#0A0A0C', color: '#F2F2F5', minHeight: '100vh' }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(240px,1fr))', gap: 16, marginBottom: 32 }}>
                    <StoreProductCount session={devSession} />
                </div>
                <StoreProducts session={devSession} onNavigate={(p) => alert(`Navigate: ${p}`)} />
            </div>
        </QueryClientProvider>
    </React.StrictMode>,
);