import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makeOrderApi } from '../api/orderApi';
import { useAuthToken } from '../auth/useAuthToken';

export function useStoreOrders(storeId: string) {
    const token = useAuthToken();
    const api = useMemo(() => makeOrderApi(createApiClient(() => token)), [token]);

    return useQuery({
        queryKey: ['store-orders', storeId],
        queryFn: () => api.getStoreOrders(storeId),
    });
}