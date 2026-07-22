import { useQuery } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makeOrderApi } from '../api/orderApi';

export const useAdminOrderCount = () => {
    const token = useAuthToken();
    const client = createApiClient(() => token);
    const orderApi = makeOrderApi(client);

    return useQuery({
        queryKey: ['admin-order-count'],
        queryFn: async () => {
            const data = await orderApi.getOrders(0, 1);
            return data.totalElements;
        },
    });
};