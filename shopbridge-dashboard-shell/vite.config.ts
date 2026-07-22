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