import { useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { createApiClient, ApiError } from '../api/client';
import { makeMediaApi } from '../api/mediaApi';
import type { PendingImage, UploadItemResult } from '../types';

/**
 * Expose edilen hook — host, product create başarılı olduktan SONRA productId ile çağırır.
 *
 * AuthTokenProvider'a bağımlı DEĞİLDİR: token doğrudan parametre olarak alınır, çünkü host
 * bu hook'u ProductImageUpload bileşeninin context'i dışında (form submit handler'ında)
 * kullanır.
 *
 * Sıralı yükler: seçim sırası = sortOrder. İlk görsel backend tarafından primary yapılır.
 * Bir dosya patlarsa DEVAM EDER; her dosya için ayrı sonuç döner.
 */
export function useProductImageUpload(authToken: string) {
    const queryClient = useQueryClient();
    const api = useMemo(() => makeMediaApi(createApiClient(() => authToken)), [authToken]);

    const uploadAll = async (
        productId: string,
        images: PendingImage[],
        onProgress?: (results: UploadItemResult[]) => void,
    ): Promise<UploadItemResult[]> => {
        const results: UploadItemResult[] = images.map((img) => ({
            id: img.id,
            fileName: img.file.name,
            status: 'pending',
        }));
        onProgress?.([...results]);

        for (let i = 0; i < images.length; i++) {
            results[i] = { ...results[i], status: 'uploading' };
            onProgress?.([...results]);

            try {
                const asset = await api.uploadProductImage(productId, images[i].file);
                results[i] = { ...results[i], status: 'success', asset };
            } catch (err) {
                results[i] = {
                    ...results[i],
                    status: 'error',
                    message: err instanceof ApiError ? err.message : 'Görsel yüklenemedi.',
                };
            }
            onProgress?.([...results]);
        }

        queryClient.invalidateQueries({ queryKey: ['product-media', productId] });
        return results;
    };

    return { uploadAll };
}