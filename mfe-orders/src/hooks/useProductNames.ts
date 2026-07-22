import { useMemo } from 'react';
import { useQueries } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makeProductApi } from '../api/productApi';
import { useAuthToken } from '../auth/useAuthToken';

// Birden çok productId'yi useQueries ile cache'leyerek çözer.
export function useProductNames(productIds: string[]) {
    const token = useAuthToken();
    const api = useMemo(() => makeProductApi(createApiClient(() => token)), [token]);

    const unique = useMemo(() => Array.from(new Set(productIds)), [productIds]);

    const results = useQueries({
        queries: unique.map((id) => ({
            queryKey: ['product-name', id],
            queryFn: () => api.getById(id),
            staleTime: 5 * 60_000,
        })),
    });

    // productId → ad haritası
    const nameMap = useMemo(() => {
        const map: Record<string, string> = {};
        unique.forEach((id, i) => {
            const data = results[i]?.data;
            map[id] = data?.name ?? `Ürün ${id.slice(0, 8)}`;
        });
        return map;
    }, [unique, results]);

    return nameMap;
}