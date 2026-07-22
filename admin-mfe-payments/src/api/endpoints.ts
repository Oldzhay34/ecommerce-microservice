const BASE_URL = import.meta.env.VITE_API_GATEWAY_URL ?? 'http://localhost:8080';

export const ENDPOINTS = {
    payments: `${BASE_URL}/api/payments`,
};