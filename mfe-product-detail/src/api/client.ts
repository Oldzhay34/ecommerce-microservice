import axios, {
    AxiosError,
    type AxiosInstance,
    type InternalAxiosRequestConfig,
} from 'axios';
import { API_BASE_URL } from './endpoints';
import type { ApiErrorResponse } from '../types';

/**
 * Axios instance fabrikası.
 * Token boş string ise Authorization header EKLENMEZ —
 * GET /api/v1/products/{id} public olduğu için istek yine gönderilir.
 */
export function createApiClient(getToken: () => string): AxiosInstance {
    const client = axios.create({
        baseURL: API_BASE_URL,
        headers: {
            'Content-Type': 'application/json',
        },
    });

    client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
        const token = getToken();
        if (token && token.trim().length > 0) {
            config.headers.set('Authorization', `Bearer ${token}`);
        } else {
            config.headers.delete('Authorization');
        }
        return config;
    });

    client.interceptors.response.use(
        (response) => response,
        (error: unknown) => Promise.reject(normalizeApiError(error)),
    );

    return client;
}

/** Her hatayı ApiErrorResponse şekline indirger (Pressman · Error Information Handling). */
export function normalizeApiError(error: unknown): ApiErrorResponse {
    const now = new Date().toISOString();

    if (isApiErrorResponse(error)) {
        return error;
    }

    if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError<Partial<ApiErrorResponse>>;
        const data = axiosError.response?.data;
        const status = axiosError.response?.status ?? 0;
        const path = axiosError.config?.url ?? '';

        if (data && typeof data === 'object' && typeof data.message === 'string') {
            return {
                timestamp: typeof data.timestamp === 'string' ? data.timestamp : now,
                status: typeof data.status === 'number' ? data.status : status,
                error: typeof data.error === 'string' ? data.error : axiosError.name,
                message: data.message,
                path: typeof data.path === 'string' ? data.path : path,
            };
        }

        if (status === 0) {
            return {
                timestamp: now,
                status: 0,
                error: 'Network Error',
                message: 'Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.',
                path,
            };
        }

        return {
            timestamp: now,
            status,
            error: axiosError.response?.statusText ?? 'Error',
            message: messageForStatus(status),
            path,
        };
    }

    return {
        timestamp: now,
        status: 0,
        error: 'Unknown Error',
        message: 'Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.',
        path: '',
    };
}

/** 404 kontrolü — retry politikası ve ProductNotFound kararı bu fonksiyona bağlıdır. */
export function isNotFound(error: unknown): boolean {
    if (isApiErrorResponse(error)) {
        return error.status === 404;
    }
    if (axios.isAxiosError(error)) {
        return error.response?.status === 404;
    }
    return false;
}

export function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    if (typeof value !== 'object' || value === null) {
        return false;
    }
    const candidate = value as Partial<ApiErrorResponse>;
    return (
        typeof candidate.message === 'string' &&
        typeof candidate.status === 'number' &&
        typeof candidate.timestamp === 'string' &&
        typeof candidate.path === 'string'
    );
}

/** Hata mesajı yoksa kullanıcıya gösterilebilir bir karşılık üretir. */
function messageForStatus(status: number): string {
    switch (status) {
        case 400:
            return 'İstek geçersiz. Lütfen bilgileri kontrol edip tekrar deneyin.';
        case 401:
            return 'Bu işlem için oturum açmanız gerekiyor.';
        case 403:
            return 'Bu işlem için yetkiniz yok.';
        case 404:
            return 'Kayıt bulunamadı.';
        case 409:
            return 'İşlem mevcut durumla çakışıyor. Sayfayı yenileyip tekrar deneyin.';
        case 500:
            return 'Sunucu tarafında bir hata oluştu. Lütfen tekrar deneyin.';
        default:
            return 'İşlem tamamlanamadı. Lütfen tekrar deneyin.';
    }
}