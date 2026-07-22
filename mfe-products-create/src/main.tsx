import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import StoreProductCreate from './expose/StoreProductCreate';
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
            <div className="bg-canvas min-h-screen px-6 py-8">
                <StoreProductCreate
                    session={devSession}
                    onNavigate={(p) => alert(`Navigate: ${p}`)}
                />
            </div>
        </QueryClientProvider>
    </React.StrictMode>,
);