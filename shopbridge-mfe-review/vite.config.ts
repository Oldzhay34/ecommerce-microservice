import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

/**
 * @originjs/vite-plugin-federation 1.3.3, remoteEntry.js içinde bozuk __v__css__
 * çağrıları üretiyor. Bu plugin onları temizler.
 * (mfe-product-detail ve mfe-media-gallery'deki çalışan çözümden birebir kopyalandı.)
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
            name: 'mfe_reviews',
            filename: 'remoteEntry.js',
            exposes: {
                './Widget': './src/component/Widget.tsx',
                './ProductReviews': './src/component/ProductReviews.tsx',
            },
            // DIZI DEGIL NESNE: dizi formu singleton garantisi ve versiyon eslesmesi VERMEZ.
            // Host shell singleton: true, requiredVersion: '^18.3.1' istiyor; uyusmazlik
            // federation'da cssFilePaths / hook hatalarina yol acar.
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
        port: 5004,
        strictPort: true,
        cors: true,
    },
    preview: {
        port: 5004,
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