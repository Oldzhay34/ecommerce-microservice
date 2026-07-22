import { useQuery } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makePaymentApi } from '../api/paymentApi';

export const useAdminPayments = () => {
    const token = useAuthToken();
    const client = createApiClient(() => token);
    const paymentApi = makePaymentApi(client);

    return useQuery({
        queryKey: ['admin-payments'],
        queryFn: () => paymentApi.getPayments(),
    });
};