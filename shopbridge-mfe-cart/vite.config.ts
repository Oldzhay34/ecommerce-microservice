import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
    plugins: [
        react(),
        federation({
            name: 'mfe_cart',
            filename: 'remoteEntry.js',
            exposes: {
                './Widget': './src/components/Widget.tsx',
            },
            shared: ['react', 'react-dom', '@tanstack/react-query'],
        }),
    ],
    build: { target: 'esnext', modulePreload: false, cssCodeSplit: false },
});