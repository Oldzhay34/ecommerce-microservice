import { defineConfig, Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

function stripFederationCss(): Plugin {
    return {
        name: 'strip-federation-css',
        generateBundle(_options, bundle) {
            for (const file of Object.values(bundle)) {
                if (file.type === 'chunk' && file.fileName.includes('remoteEntry')) {
                    file.code = file.code.replace(
                        /,?\s*[a-zA-Z_$][\w$]*\(`__v__css__[^`]*`[^)]*\)\s*,?/g,
                        (m) => (m.trim().startsWith(',') && m.trim().endsWith(',') ? ',' : ''),
                    );
                }
            }
        },
    };
}

export default defineConfig({
    plugins: [
        react(),
        federation({
            name: 'admin_mfe_payments',
            filename: 'remoteEntry.js',
            exposes: {
                './AdminPayments': './src/expose/AdminPayments.tsx',
                './AdminRefundCount': './src/expose/AdminRefundCount.tsx',
            },
            shared: ['react', 'react-dom', '@tanstack/react-query']
        }),
        stripFederationCss()
    ],
    server: {
        port: 6102,
        strictPort: true
    },
    build: {
        modulePreload: false,
        target: 'esnext',
        minify: false,
        cssCodeSplit: false
    }
});