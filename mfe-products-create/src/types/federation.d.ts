// Module Federation remote'ları TypeScript'e tanıtılır.
declare module 'mfe_media_gallery/ProductImageUpload' {
    import type { ComponentType } from 'react';

    export interface PendingImage {
        id: string;
        file: File;
        previewUrl: string;
    }

    export type UploadItemStatus = 'pending' | 'uploading' | 'success' | 'error';

    export interface UploadItemResult {
        id: string;
        fileName: string;
        status: UploadItemStatus;
        message?: string;
        asset?: unknown;
    }

    const ProductImageUpload: ComponentType<{
        authToken: string;
        images: PendingImage[];
        onChange: (next: PendingImage[]) => void;
        results: UploadItemResult[];
        disabled: boolean;
    }>;

    export default ProductImageUpload;
}

declare module 'mfe_media_gallery/useProductImageUpload' {
    import type { PendingImage, UploadItemResult } from 'mfe_media_gallery/ProductImageUpload';

    export function useProductImageUpload(authToken: string): {
        uploadAll: (
            productId: string,
            images: PendingImage[],
            onProgress?: (results: UploadItemResult[]) => void,
        ) => Promise<UploadItemResult[]>;
    };
}