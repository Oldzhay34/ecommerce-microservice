export const ENDPOINTS = {
    productCreate: '/api/v1/products', // POST -> Yeni ürün ekleme (@PreAuthorize hasRole('STORE'))
} as const;

export const API_BASE = import.meta.env.VITE_API_GATEWAY_URL ?? 'http://localhost:8080';