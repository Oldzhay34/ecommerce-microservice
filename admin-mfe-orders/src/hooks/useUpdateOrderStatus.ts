import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makeOrderApi } from '../api/orderApi';

export const useUpdateOrderStatus = () => {
    const token = useAuthToken();
    const queryClient = useQueryClient();
    const client = createApiClient(() => token);
    const orderApi = makeOrderApi(client);

    return useMutation({
        mutationFn: ({ id, status }: { id: string; status: 'SHIPPED' | 'CANCELLED' }) =>
            orderApi.updateStatus(id, status),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
            queryClient.invalidateQueries({ queryKey: ['admin-order-count'] });
        },
    });
};