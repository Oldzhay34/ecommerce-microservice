import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminOrders from './expose/AdminOrders';
import { AdminSession } from './types';

const queryClient = new QueryClient();

const mockSession: AdminSession = {
    authToken: 'standalone-mock-token',
    userId: 'mock-admin-id',
    role: 'ADMIN',
};

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <div className="p-8 max-w-[1280px] mx-auto bg-white rounded-xl shadow mt-10">
                <h2 className="text-lg font-bold mb-4">Sipariş Yönetimi Modülü (Standalone)</h2>
                <AdminOrders session={mockSession} />
            </div>
        </QueryClientProvider>
    </React.StrictMode>
);