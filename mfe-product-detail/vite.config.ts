import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

/**
 * @originjs/vite-plugin-federation 1.3.3, remoteEntry.js içinde bozuk
 * __v__css__ çağrıları üretiyor. Bu plugin onları temizler.
 * ÇALIŞAN ÇÖZÜM — DEĞİŞTİRME.
 */
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
            name: 'mfe_product_detail',
            filename: 'remoteEntry.js',
            // BUNU EKLEMELİSİN: Medya galerisinin çalıştığı adresi Host projeye bildiriyoruz
            remotes: {
                mfe_media_gallery: 'http://localhost:6008/assets/remoteEntry.js',
                mfe_reviews: 'http://localhost:5004/assets/remoteEntry.js',
            },
            exposes: {
                './ProductDetail': './src/expose/ProductDetail.tsx',
            },
            shared: {
                'react': { singleton: true, requiredVersion: '^18.3.1' },
                'react-dom': { singleton: true, requiredVersion: '^18.3.1' },
                '@tanstack/react-query': { singleton: true }
            } as any,
        }),
        stripFederationCss(),
    ],
    server: {
        port: 6010,
        strictPort: true,
        cors: true,
    },
    preview: {
        port: 6010,
        strictPort: true,
        cors: true,   // ← EKLE
    },
    build: {
        target: 'esnext',
        minify: false,
        cssCodeSplit: false,
        modulePreload: false,
    },
});