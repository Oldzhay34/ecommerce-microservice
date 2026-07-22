import { createPublicApiClient } from './client';
import { makeMediaApi } from './mediaApi';
import type { MediaAssetResponse } from '../types';

export const API_BASE = import.meta.env.VITE_API_GATEWAY_URL ?? 'http://localhost:8080';

export const MEDIA_ENDPOINTS = {
    // GET  -> public (token yok)
    productImages: (productId: string) => `/api/v1/media/products/${productId}/images`,
    // POST -> ROLE_STORE, multipart/form-data, field adı: "file", tek dosya
    uploadImage: (productId: string) => `/api/v1/media/products/${productId}/images`,
} as const;

// Backend SABİT limitleri — application.yml ile hizalı.
// Değişirse ikisi birlikte güncellenmelidir.
export const MEDIA_LIMITS = {
    maxFileSizeBytes: 5 * 1024 * 1024, // spring.servlet.multipart.max-file-size: 5MB
    acceptedMimeTypes: ['image/png', 'image/jpeg', 'image/webp'] as const,
    // Backend media.max-images-per-product = 10; create ekranında 5 ile sınırlanır (ürün kararı).
    maxImagesOnCreate: 5,
} as const;

// Galeri okuma için tekil public istemci — her render'da yeni axios instance yaratılmasın.
const publicMediaApi = makeMediaApi(createPublicApiClient());

/**
 * Public okuma. Token gerekmez; ürün detay ekranı token'sız çalışır.
 * Görseli olmayan ürün için BOŞ DİZİ döner (404 değil).
 */
export function fetchProductMedia(productId: string): Promise<MediaAssetResponse[]> {
    return publicMediaApi.getProductImages(productId);
}