import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { router } from './router';
import './shared/theme/tailwind.css'; // [Varsayım: Tailwind direktiflerini içerir]

// URL'deki token'ı yakala, localStorage'a yaz, adres çubuğunu temizle.
// React render'ından ÖNCE çalışmalı ki API interceptor'ları token'ı bulabilsin.
(() => {
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get('token');
    if (urlToken) {
        localStorage.setItem('shopbridge_access_token', urlToken);
        params.delete('token');
        const clean = window.location.pathname + (params.toString() ? '?' + params.toString() : '');
        window.history.replaceState({}, '', clean);
    }
})();

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <RouterProvider router={router} />
        </QueryClientProvider>
    </React.StrictMode>
);