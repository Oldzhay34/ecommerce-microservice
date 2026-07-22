// endpoints.ts

export const ENDPOINTS = {
    productSearch: '/api/v1/products/search',
    productCreate: '/api/v1/products', // POST -> Yeni ürün ekleme
    productDetail: (id: string) => `/api/v1/products/${id}`, // GET -> Ürün detayı
    productStock: (id: string) => `/api/v1/products/${id}/stock`, // PUT -> Stok güncelleme

} as const;

export const API_BASE = import.meta.env.VITE_API_GATEWAY_URL ?? 'http://localhost:8080';