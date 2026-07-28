import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import StoreReviews from './expose/StoreReviews';
import type { StoreSession } from './types';
import './index.css';

const queryClient = new QueryClient();

const devSession: StoreSession = {
    authToken: import.meta.env.VITE_DEV_TOKEN ?? 'dev-token',
    storeId: import.meta.env.VITE_DEV_STORE_ID ?? 'dev-store-id',
    role: 'STORE',
};

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <div style={{ maxWidth: 1120, margin: '0 auto', padding: 24, background: '#0A0A0C', minHeight: '100vh' }}>
                <StoreReviews session={devSession} />
            </div>
        </QueryClientProvider>
    </React.StrictMode>,
);