import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makeOrderApi } from '../api/orderApi';
import { useAuthToken } from '../auth/useAuthToken';

export function useShipOrder(storeId: string) {
    const token = useAuthToken();
    const qc = useQueryClient();
    const api = useMemo(() => makeOrderApi(createApiClient(() => token)), [token]);

    return useMutation({
        mutationFn: (id: string) => api.updateOrderStatus(id, { status: 'SHIPPED' }),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['store-orders', storeId] });
            qc.invalidateQueries({ queryKey: ['store-order-metrics', storeId] });
        },
    });
}