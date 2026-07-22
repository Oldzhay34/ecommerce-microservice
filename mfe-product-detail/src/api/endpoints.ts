/**
 * URL sabitleri — Repository benzeri soyutlama.
 * Bileşenler URL bilmez; yalnızca adapter'lar bu dosyayı okur.
 */

export const API_BASE_URL: string =
    import.meta.env.VITE_API_GATEWAY_URL ?? 'http://localhost:8080';

/** Product Service — DOĞRULANMIŞ (controller kodundan). */
export const PRODUCT_BY_ID = (id: string): string =>
    `/api/v1/products/${encodeURIComponent(id)}`;

/**
 * Product Service — DOĞRULANMIŞ, ancak BU EKRANDA KULLANILMAZ.
 * Liste/arama ekranına aittir; yalnızca referans için tanımlıdır.
 */
export const PRODUCT_SEARCH = '/api/v1/products/search';

/**
 * Cart Service — [Varsayım: DOĞRULANMAMIŞ].
 * Endpoint yolu ve body şekli, Product servisinin /api/v1/... deseninden
 * ve "principal token'dan çıkarılır" konvansiyonundan türetilmiştir.
 * Backend farklıysa YALNIZCA bu sabit ve types.ts · AddToCartRequest güncellenir.
 */
export const CART_ITEMS = '/api/v1/cart/items';