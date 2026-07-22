/**
 * ★ TEK DEĞİŞİM NOKTASI ★
 *
 * Görsel alanının TEK bilgi noktası burasıdır.
 * Backend Product modelinde görsel alanı bugün YOKTUR — bu fonksiyon null döner
 * ve ProductImage placeholder gösterir. Bu beklenen davranıştır, hata değildir.
 *
 * Backend görsel alanını eklediğinde (ya da farklı bir isim kullandığında,
 * ör. thumbnailUrl) YALNIZCA bu dosya güncellenir.
 * ProductImage, grid, skeleton, boyutlar ve diğer hiçbir bileşen değişmez.
 */

import type { Product } from '../types';

export function resolveImageUrl(product: Product): string | null {
    // 1) imageUrls bir dizi ve [0] boş olmayan string ise → onu döndür.
    const urls = product.imageUrls;
    if (Array.isArray(urls)) {
        const first = urls[0];
        if (typeof first === 'string' && first.trim().length > 0) {
            return first;
        }
    }

    // 2) imageUrl boş olmayan string ise → onu döndür.
    const url = product.imageUrl;
    if (typeof url === 'string' && url.trim().length > 0) {
        return url;
    }

    // 3) Aksi halde → null (placeholder gösterilir).
    return null;
}