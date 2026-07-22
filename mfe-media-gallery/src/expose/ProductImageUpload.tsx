import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { ProductImageUploader } from '../features/ProductImageUploader';
import type { PendingImage, UploadItemResult } from '../types';

export type { PendingImage, UploadItemResult } from '../types';

/**
 * Expose: token'ı iç context'e köprüler.
 * Kendi QueryClientProvider'ını KURMAZ — host'un singleton cache'ini kullanır.
 *
 * Kontrollü bileşendir: images/onChange host'ta yaşar. Upload'ı host,
 * useUploadProductImages hook'uyla product create sonrası tetikler.
 */
export default function ProductImageUpload({
                                               authToken,
                                               images,
                                               onChange,
                                               results,
                                               disabled,
                                           }: {
    authToken: string;
    images: PendingImage[];
    onChange: (next: PendingImage[]) => void;
    results: UploadItemResult[];
    disabled: boolean;
}) {
    return (
        <AuthTokenProvider token={authToken}>
            <ProductImageUploader
                images={images}
                onChange={onChange}
                results={results}
                disabled={disabled}
            />
        </AuthTokenProvider>
    );
}