import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
    plugins: [
        react(),
        federation({
            name: 'shopbridge_dashboard_shell',
            remotes: {
                mfe_orders: 'http://localhost:5001/assets/remoteEntry.js',
                mfe_cart: 'http://localhost:5002/assets/remoteEntry.js',
                mfe_payments: 'http://localhost:5003/assets/remoteEntry.js',
                mfe_reviews: 'http://localhost:5004/assets/remoteEntry.js',
                mfe_products: 'http://localhost:5005/assets/remoteEntry.js',
                mfe_product_detail: 'http://localhost:6010/assets/remoteEntry.js',
                mfe_product_create: 'http://localhost:6006/assets/remoteEntry.js',
                // Store* bileşenleri (StoreProducts, StoreOrders, ...) mfe_orders/mfe_products/mfe_reviews'ın
                // customer Widget'ını sunan (5001/5004/5005) deployment'ında YOK — ayrı, sadece Store*
                // expose eden kardeş paketlerde (6001/6004/6005). Ayrı remote key ile bağlanır.
                mfe_orders_store: 'http://localhost:6001/assets/remoteEntry.js',
                mfe_reviews_store: 'http://localhost:6004/assets/remoteEntry.js',
                mfe_products_store: 'http://localhost:6005/assets/remoteEntry.js',
            },
            shared: {
                'react': { singleton: true, requiredVersion: '^18.3.1' },
                'react-dom': { singleton: true, requiredVersion: '^18.3.1' },
                '@tanstack/react-query': { singleton: true },
                'zustand': { singleton: true }
            } as any,
        }),
    ],
    resolve: {
        dedupe: ['react', 'react-dom']
    },
    build: { target: 'esnext' },
});