import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

/**
 * @originjs/vite-plugin-federation 1.3.3, remoteEntry.js içinde bozuk
 * __v__css__ çağrıları üretiyor. Bu plugin onları temizler.
 * (mfe-product-detail'deki çalışan çözümden birebir kopyalandı — DEĞİŞTİRME.)
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
        (federation as any)({
            name: 'mfe_media_gallery',
            filename: 'remoteEntry.js',
            exposes: {
                './ProductGallery': './src/expose/ProductGallery.tsx',
                './ProductImageUpload': './src/expose/ProductImageUpload.tsx',
                './useProductImageUpload': './src/expose/useProductImageUpload.ts',
            },
            shared: {
                'react': { singleton: true, requiredVersion: '^18.3.1' },
                'react-dom': { singleton: true, requiredVersion: '^18.3.1' },
                '@tanstack/react-query': { singleton: true },
            },
        }),
        stripFederationCss(),
    ],
    resolve: { dedupe: ['react', 'react-dom'] },
    server: {
        port: 6008,
        strictPort: true,
        cors: true,
    },
    preview: {
        port: 6008,
        strictPort: true,
        cors: true,
    },
    build: {
        target: 'esnext',
        minify: false,
        cssCodeSplit: false,
        modulePreload: false,
    },
});