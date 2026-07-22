import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makeReviewApi } from '../api/reviewApi';

export const useModerateReview = () => {
    const token = useAuthToken();
    const queryClient = useQueryClient();
    const client = createApiClient(() => token);
    const reviewApi = makeReviewApi(client);

    return useMutation({
        mutationFn: ({ id, status }: { id: string; status: 'ACTIVE' | 'HIDDEN' }) =>
            reviewApi.moderateReview(id, status),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['admin-reviews'] });
            queryClient.invalidateQueries({ queryKey: ['admin-review-count'] });
        },
    });
};