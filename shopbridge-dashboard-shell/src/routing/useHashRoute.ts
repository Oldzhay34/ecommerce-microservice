import { useEffect, useState } from 'react';

/** '#/product/<uuid>' → uuid · aksi halde null */
function readProductIdFromHash(): string | null {
    const match = window.location.hash.match(/^#\/product\/([^/?#]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

export function useProductRoute() {
    const [productId, setProductId] = useState<string | null>(readProductIdFromHash);

    useEffect(() => {
        const onHashChange = () => setProductId(readProductIdFromHash());
        window.addEventListener('hashchange', onHashChange);
        return () => window.removeEventListener('hashchange', onHashChange);
    }, []);

    /** Remote'un onNavigate sözleşmesi: '/dashboard' | '/cart' | '/product/:id' */
    const navigate = (path: string) => {
        if (path === '/dashboard') {
            window.location.hash = '';
            return;
        }
        window.location.hash = `#${path}`;
    };

    return { productId, navigate };
}