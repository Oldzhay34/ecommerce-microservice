import { useContext, useMemo } from 'react';
import type { AxiosInstance } from 'axios';
import { AuthTokenContext } from './AuthTokenProvider';
import { createApiClient } from '../api/client';

/** Bağlamdaki ham token. Token boş string olabilir (public ürün sorgusu). */
export function useAuthToken(): string {
    const context = useContext(AuthTokenContext);
    if (context === null) {
        throw new Error('useAuthToken, AuthTokenProvider içinde kullanılmalıdır.');
    }
    return context.token;
}

/** Token'ı interceptor'a bağlayan tekil axios instance. */
export function useApiClient(): AxiosInstance {
    const token = useAuthToken();
    return useMemo(() => createApiClient(() => token), [token]);
}