import axios, { type AxiosInstance } from 'axios';
import { API_BASE } from './endpoints';
import type { ApiErrorResponse } from '../types';

// Normalize edilmiş hata: genel mesaj + alan bazlı harita birlikte taşınır.
export class ApiError extends Error {
    readonly status?: number;
    readonly fieldErrors?: Record<string, string>;

    constructor(message: string, status?: number, fieldErrors?: Record<string, string>) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

// Her MFE kendi axios instance'ı + interceptor (Adapter Pattern).
// Token, çağrı anında getToken() ile okunur (React context köprüsü).
export function createApiClient(getToken: () => string): AxiosInstance {
    const instance = axios.create({ baseURL: API_BASE });

    instance.interceptors.request.use((config) => {
        const token = getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    });

    instance.interceptors.response.use(
        (res) => res,
        (error) => {
            const data = error?.response?.data as ApiErrorResponse | undefined;
            const message = data?.message ?? error?.message ?? 'Beklenmeyen bir hata oluştu.';
            return Promise.reject(
                new ApiError(message, error?.response?.status ?? data?.status, data?.errors),
            );
        },
    );

    return instance;
}