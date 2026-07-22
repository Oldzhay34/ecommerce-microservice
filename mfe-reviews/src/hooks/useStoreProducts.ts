import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makeProductApi } from '../api/productApi';
import { useAuthToken } from '../auth/useAuthToken';

export function useStoreProducts(storeId: string) {
    const token = useAuthToken();
    const api = useMemo(() => makeProductApi(createApiClient(() => token)), [token]);

    return useQuery({
        queryKey: ['review-store-products', storeId],
        queryFn: () => api.getStoreProducts(storeId),
    });
}