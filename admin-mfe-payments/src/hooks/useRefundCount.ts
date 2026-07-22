import { useQuery } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makePaymentApi } from '../api/paymentApi';

export const useRefundCount = () => {
    const token = useAuthToken();
    const client = createApiClient(() => token);
    const paymentApi = makePaymentApi(client);

    return useQuery({
        queryKey: ['admin-refund-count'],
        queryFn: async () => {
            const data = await paymentApi.getPayments();
            return data.filter((payment) => payment.status === 'REFUND_REQUESTED').length;
        },
    });
};