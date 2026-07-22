export const ENDPOINTS = {
    storeOrders: (storeId: string) => `/api/orders/store/${storeId}`,
    orderStatus: (id: string) => `/api/orders/${id}/status`,
    productById: (id: string) => `/api/v1/products/${id}`,
    paymentsByOrder: '/api/payments',
} as const;

// [Varsayım: VITE_API_GATEWAY_URL yoksa http://localhost:8080]
export const API_BASE =
    import.meta.env.VITE_API_GATEWAY_URL ?? 'http://localhost:8080';