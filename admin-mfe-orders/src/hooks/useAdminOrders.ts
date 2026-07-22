import { useQuery } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makeOrderApi } from '../api/orderApi';

export const useAdminOrders = (page: number) => {
    const token = useAuthToken();
    const client = createApiClient(() => token);
    const orderApi = makeOrderApi(client);

    return useQuery({
        queryKey: ['admin-orders', page],
        queryFn: () => orderApi.getOrders(page),
        placeholderData: (prev) => prev,
    });
};