import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makePaymentApi } from '../api/paymentApi';

export const useApproveRefund = () => {
    const token = useAuthToken();
    const queryClient = useQueryClient();
    const client = createApiClient(() => token);
    const paymentApi = makePaymentApi(client);

    return useMutation({
        mutationFn: (id: string) => paymentApi.approveRefund(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['admin-payments'] });
            queryClient.invalidateQueries({ queryKey: ['admin-refund-count'] });
        },
    });
};