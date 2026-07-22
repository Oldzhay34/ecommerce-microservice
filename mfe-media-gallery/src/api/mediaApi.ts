import type { AxiosInstance } from 'axios';
import { MEDIA_ENDPOINTS } from './endpoints';
import type { MediaAssetResponse } from '../types';

// Adapter — bileşen doğrudan axios çağırmaz.
export function makeMediaApi(client: AxiosInstance) {
    return {
        // Public: token gerekmez, görseli yoksa boş dizi döner (404 değil).
        async getProductImages(productId: string): Promise<MediaAssetResponse[]> {
            const res = await client.get<MediaAssetResponse[]>(
                MEDIA_ENDPOINTS.productImages(productId),
            );
            return res.data;
        },

        /**
         * ROLE_STORE. storeId GÖNDERİLMEZ — backend extractUserIdFromSecurityContext() ile
         * JWT'den çıkarır (IDOR koruması).
         *
         * Content-Type header'ı ELLE SET EDİLMEZ: axios FormData görünce boundary ile birlikte
         * kendisi yazar. Elle yazmak boundary'yi düşürür ve backend multipart'ı parse edemez.
         */
        async uploadProductImage(productId: string, file: File): Promise<MediaAssetResponse> {
            const form = new FormData();
            form.append('file', file);
            const res = await client.post<MediaAssetResponse>(
                MEDIA_ENDPOINTS.uploadImage(productId),
                form,
            );
            return res.data; // 201 Created + UploadMediaResponse
        },
    };
}

export type MediaApi = ReturnType<typeof makeMediaApi>;