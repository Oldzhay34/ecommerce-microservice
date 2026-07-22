import { useCallback, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { createApiClient, ApiError } from '../api/client';
import { makeMediaApi } from '../api/mediaApi';
import { useAuthToken } from '../auth/useAuthToken';
import type { PendingImage, UploadItemResult } from '../types';

/**
 * Görselleri SIRAYLA yükler (paralel değil).
 *
 * Neden sıralı: backend ilk görseli primary = true, sortOrder = 0 yapar ve sonraki her görsel
 * için sortOrder'ı lock altında artırır. Paralel gönderim sortOrder'ı kullanıcının seçim
 * sırasından farklı üretebilir. Sıralı gönderim seçim sırası = galeri sırası garantisi verir.
 *
 * Hata politikası [kullanıcı kararı]: bir dosya patlarsa DEVAM EDİLİR. Ürün zaten
 * oluşturulmuştur ve geri alınamaz; kullanıcı en azından yüklenebilenleri kazanır.
 * Sonuç raporu her dosya için ayrı durum taşır.
 */
export function useUploadProductImages() {
    const token = useAuthToken();
    const queryClient = useQueryClient();
    const api = useMemo(() => makeMediaApi(createApiClient(() => token)), [token]);

    const [results, setResults] = useState<UploadItemResult[]>([]);
    const [isUploading, setIsUploading] = useState(false);

    const upload = useCallback(
        async (productId: string, images: PendingImage[]): Promise<UploadItemResult[]> => {
            setIsUploading(true);

            const initial: UploadItemResult[] = images.map((img) => ({
                id: img.id,
                fileName: img.file.name,
                status: 'pending',
            }));
            setResults(initial);

            const final: UploadItemResult[] = [...initial];

            for (let i = 0; i < images.length; i++) {
                const img = images[i];

                final[i] = { ...final[i], status: 'uploading' };
                setResults([...final]);

                try {
                    const asset = await api.uploadProductImage(productId, img.file);
                    final[i] = { ...final[i], status: 'success', asset };
                } catch (err) {
                    const message =
                        err instanceof ApiError ? err.message : 'Görsel yüklenemedi.';
                    final[i] = { ...final[i], status: 'error', message };
                }

                setResults([...final]);
            }

            // Galeri cache'ini tazele — ürün detayı bu anahtarı kullanır.
            queryClient.invalidateQueries({ queryKey: ['product-media', productId] });

            setIsUploading(false);
            return final;
        },
        [api, queryClient],
    );

    const reset = useCallback(() => setResults([]), []);

    return { upload, results, isUploading, reset };
}