import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminReviews from './expose/AdminReviews';
import { AdminSession } from './types';
import './index.css';

const queryClient = new QueryClient();

// Standalone test için sahte bir admin oturumu
const mockSession: AdminSession = {
    authToken: 'standalone-mock-token',
    userId: 'mock-admin-id',
    role: 'ADMIN',
};

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <div className="p-8 max-w-[1280px] mx-auto bg-white rounded-xl shadow mt-10">
                <div className="mb-6">
                    <h2 className="text-xl font-bold text-[#111827]">Yorum Yönetimi Modülü</h2>
                    <p className="text-sm text-[#6B7280]">Bağımsız Geliştirme Ortamı (Standalone)</p>
                </div>
                <AdminReviews session={mockSession} />
            </div>
        </QueryClientProvider>
    </React.StrictMode>
);