import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ProductGallery from './expose/ProductGallery';
import './index.css';

// Sadece dev ortamı (main.tsx) için QueryClient
const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <div className="max-w-[420px] mx-auto mt-10 p-4 border rounded-sb-lg border-dashed border-border">
                <h2 className="text-sm text-ink-muted mb-4 font-mono">Dev Test Container</h2>
                <ProductGallery productId="dev-test-123" />
            </div>
        </QueryClientProvider>
    </React.StrictMode>,
);