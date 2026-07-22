import axios, { AxiosError } from 'axios';
import type { ApiErrorResponse } from './types';

export const apiClient = axios.create({
    // Auth servisinin dışa açılan portu (8085 - API Gateway)
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.response.use(
    (response) => response,
    (error: AxiosError<ApiErrorResponse>) => {
        if (error.response?.data) {
            return Promise.reject(error.response.data);
        }
        return Promise.reject({
            message: 'Sunucuya ulaşılamıyor veya beklenmeyen bir ağ hatası oluştu.',
            status: 500,
        } as ApiErrorResponse);
    }
);