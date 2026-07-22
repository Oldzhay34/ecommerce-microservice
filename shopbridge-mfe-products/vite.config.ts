import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
    plugins: [
        react(),
        federation({
            name: 'mfe_products',
            filename: 'remoteEntry.js',
            exposes: {
                './Widget': './src/features/catalog/ProductCatalog.tsx',
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
    server: { port: 5005 },
    preview: { port: 5005 },
    build: { target: 'esnext' },
});