import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

// remoteEntry.js'teki bozuk __v__css__ çağrılarını build sırasında temizler.
// (mfe-products'taki çalışan çözümden birebir kopyalandı — dokunma.)
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
            name: 'mfe_product_create',
            filename: 'remoteEntry.js',
            // Media galerisi upload bileşenini buradan tüketiyoruz.
            remotes: {
                mfe_media_gallery: 'http://localhost:6008/assets/remoteEntry.js',
            },
            exposes: {
                './StoreProductCreate': './src/expose/StoreProductCreate.tsx',
            },
            shared: {
                'react':                 { singleton: true, requiredVersion: '^18.3.1' },
                'react-dom':             { singleton: true, requiredVersion: '^18.3.1' },
                '@tanstack/react-query': { singleton: true },
            },
        }),
        stripFederationCss(),
    ],
    resolve: { dedupe: ['react', 'react-dom'] },
    build: { target: 'esnext', cssCodeSplit: false },
    server: { port: 6006, cors: { origin: ['http://localhost:3000', 'http://localhost:3001'] } },
    preview: { port: 6006, cors: { origin: ['http://localhost:3000', 'http://localhost:3001'] } },
});