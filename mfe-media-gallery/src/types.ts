// Media servisi API sözleşmesi — backend MediaAssetResponse ile birebir.
// Bu dosya galeri (okuma) ve upload (yazma) tarafının ORTAK tip kaynağıdır.
export interface MediaAssetResponse {
    assetId: string;
    productId: string;
    url: string;          // MEDIUM varyant, her zaman .webp
    thumbUrl: string;     // THUMB varyant, her zaman .webp
    originalUrl: string;  // ORIGINAL varyant, her zaman .webp
    contentType: string;  // her zaman "image/webp"
    sizeBytes: number;
    width: number;
    height: number;
    sortOrder: number;
    primary: boolean;
    createdAt: string;    // ISO-8601
}

// Backend GlobalExceptionHandler'ın ApiErrorResponse şeması.
export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
}

export interface ProductGalleryProps {
    productId: string;
}

// Upload akışında tarayıcıda tutulan, henüz yüklenmemiş dosya.
export interface PendingImage {
    id: string;           // client-side geçici kimlik
    file: File;
    previewUrl: string;   // URL.createObjectURL — revoke edilmeli
}

export type UploadItemStatus = 'pending' | 'uploading' | 'success' | 'error';

export interface UploadItemResult {
    id: string;
    fileName: string;
    status: UploadItemStatus;
    message?: string;
    asset?: MediaAssetResponse;
}