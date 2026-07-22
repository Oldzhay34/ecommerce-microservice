export const ENDPOINTS = {
    productSearch: '/api/v1/products/search',
    productReviews: (productId: string) => `/api/reviews/product/${productId}`,
    reviewReply: (id: string) => `/api/reviews/${id}/reply`,
} as const;

// [Varsayım: VITE_API_GATEWAY_URL yoksa http://localhost:8080]
export const API_BASE =
    import.meta.env.VITE_API_GATEWAY_URL ?? 'http://localhost:8080';