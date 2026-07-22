// src/remotes.d.ts
declare module 'mfe_media_gallery/ProductGallery' {
    import { ComponentType } from 'react';

    export interface ProductGalleryProps {
        productId: string;
    }

    const ProductGallery: ComponentType<ProductGalleryProps>;
    export default ProductGallery;
}
declare module 'mfe_reviews/ProductReviews' {
    import type { ComponentType } from 'react';
    const ProductReviews: ComponentType<{ productId: string }>;
    export default ProductReviews;
}