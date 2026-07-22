import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makePaymentApi } from '../api/paymentApi';
import { useAuthToken } from '../auth/useAuthToken';

export function useOrderPayment(orderId: string) {
    const token = useAuthToken();
    const api = useMemo(() => makePaymentApi(createApiClient(() => token)), [token]);

    return useQuery({
        queryKey: ['order-payment', orderId],
        queryFn: () => api.getByOrder(orderId),
    });
}