import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
    plugins: [
        react(),
        federation({
            name: 'admin_dashboard_shell',
            remotes: {
                admin_mfe_orders: 'http://localhost:6101/assets/remoteEntry.js',
                admin_mfe_payments: 'http://localhost:6102/assets/remoteEntry.js',
                admin_mfe_reviews: 'http://localhost:6103/assets/remoteEntry.js',
            },
            // Katı sürüm kurallarını kaldırıp doğrudan paket isimlerini veriyoruz:
            shared: ['react', 'react-dom', '@tanstack/react-query', 'zustand']
        })
    ],
    server: {
        port: 3002,
        strictPort: true
    },
    build: {
        modulePreload: false,
        target: 'esnext',
        minify: false,
        cssCodeSplit: false
    }
});