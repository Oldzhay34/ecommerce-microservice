// src/remotes.d.ts

// Remote MFE modül bildirimleri.
// DİKKAT: Bu dosyada top-level 'export' veya 'import' kullanılmamalıdır.

type StoreSession = {
    authToken: string;
    storeId: string;
    role: 'STORE';
};

declare module 'mfe_products/StoreProducts' {
    import type { ComponentType } from 'react';
    const StoreProducts: ComponentType<{
        session: StoreSession;
        onNavigate?: (path: string) => void;
    }>;
    export default StoreProducts;
}

declare module 'mfe_products/StoreProductCount' {
    import type { ComponentType } from 'react';
    const StoreProductCount: ComponentType<{
        session: StoreSession;
    }>;
    export default StoreProductCount;
}

declare module 'mfe_orders/StoreOrders' {
    import type { ComponentType } from 'react';
    const StoreOrders: ComponentType<{
        session: StoreSession;
    }>;
    export default StoreOrders;
}

declare module 'mfe_orders/StoreOrderMetrics' {
    import type { ComponentType } from 'react';
    const StoreOrderMetrics: ComponentType<{
        session: StoreSession;
    }>;
    export default StoreOrderMetrics;
}

declare module 'mfe_reviews/StoreReviews' {
    import type { ComponentType } from 'react';
    const StoreReviews: ComponentType<{
        session: StoreSession;
    }>;
    export default StoreReviews;
}
declare module 'mfe_product_create/StoreProductCreate' {
    import type { ComponentType } from 'react';

    const StoreProductCreate: ComponentType<{
        session: { authToken: string; storeId: string; role: 'STORE' };
        onNavigate?: (path: string) => void;
    }>;
    export default StoreProductCreate;
}