import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApiClient } from '../auth/useAuthToken';
import { makeProductApi } from '../api/productApi';
import { isNotFound } from '../api/client';
import type { ApiErrorResponse, Product } from '../types';

export const productDetailQueryKey = (productId: string) =>
    ['product-detail', productId] as const;

/**
 * GET /api/v1/products/{id} — public endpoint, token olmadan da çalışır.
 * 404'te retry YOK (Pressman · System Response Time:
 * kullanıcı olmayan ürünü 3 kez beklemez).
 */
export function useProductDetail(
    productId: string,
): UseQueryResult<Product | null, ApiErrorResponse> {
    const client = useApiClient();
    const productApi = useMemo(() => makeProductApi(client), [client]);

    return useQuery<Product | null, ApiErrorResponse>({
        queryKey: productDetailQueryKey(productId),
        queryFn: () => productApi.getProductById(productId),
        enabled: Boolean(productId),
        retry: (failureCount, error) => (isNotFound(error) ? false : failureCount < 2),
        staleTime: 30_000,
    });
}