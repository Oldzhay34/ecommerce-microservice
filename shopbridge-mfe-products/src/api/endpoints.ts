export const API_BASE_URL = 'http://localhost:8080';

export const ENDPOINTS = {
    products: {
        search: '/api/v1/products/search',
        byId: (id: string) => `/api/v1/products/${id}`,
    },
};