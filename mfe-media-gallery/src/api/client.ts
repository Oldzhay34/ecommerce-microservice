import axios, { type AxiosInstance } from 'axios';
import { API_BASE } from './endpoints';
import type { ApiErrorResponse } from '../types';

// Normalize edilmiş hata.
export class ApiError extends Error {
    readonly status?: number;

    constructor(message: string, status?: number) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}

/**
 * Token'lı istemci — upload için (ROLE_STORE).
 * Token çağrı anında getToken() ile okunur (React context köprüsü).
 */
export function createApiClient(getToken: () => string): AxiosInstance {
    const instance = axios.create({ baseURL: API_BASE });

    instance.interceptors.request.use((config) => {
        const token = getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    });

    instance.interceptors.response.use((res) => res, normalizeError);
    return instance;
}

/**
 * Token'sız istemci — galeri okuma için.
 * GET /api/v1/media/products/{id}/images public'tir; ürün detay ekranı token'sız çalışır.
 */
export function createPublicApiClient(): AxiosInstance {
    const instance = axios.create({ baseURL: API_BASE });
    instance.interceptors.response.use((res) => res, normalizeError);
    return instance;
}

function normalizeError(error: any) {
    const data = error?.response?.data as ApiErrorResponse | undefined;
    const message = data?.message ?? error?.message ?? 'Beklenmeyen bir hata oluştu.';
    return Promise.reject(new ApiError(message, error?.response?.status ?? data?.status));
}