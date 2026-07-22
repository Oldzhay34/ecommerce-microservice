/// <reference types="vite/client" />

declare module 'mfe_orders/Widget';
declare module 'mfe_cart/Widget';
declare module 'mfe_payments/Widget';
declare module 'mfe_reviews/Widget';
declare module 'mfe_products/Widget';
declare module 'mfe_product_detail/ProductDetail' {
    import type { ComponentType } from 'react';

    interface CustomerSession {
        authToken: string;
        userId: string;
        role: 'CUSTOMER';
    }

    const ProductDetail: ComponentType<{
        session: CustomerSession;
        productId: string;
        onNavigate: (path: string) => void;
    }>;

    export default ProductDetail;
}