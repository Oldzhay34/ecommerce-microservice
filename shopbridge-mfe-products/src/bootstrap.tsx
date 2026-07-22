import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ProductCatalog from './features/catalog/ProductCatalog';
import './index.css';

const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <QueryClientProvider client={queryClient}>
            <div className="max-w-6xl mx-auto p-8">
                <ProductCatalog />
            </div>
        </QueryClientProvider>
    </StrictMode>
);