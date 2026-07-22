import { useQuery } from '@tanstack/react-query';
import { useAuthToken } from '../auth/AuthTokenProvider';
import { createApiClient } from '../api/client';
import { makeReviewApi } from '../api/reviewApi';

export const useReviewCount = () => {
    const token = useAuthToken();
    const client = createApiClient(() => token);
    const reviewApi = makeReviewApi(client);

    return useQuery({
        queryKey: ['admin-review-count'],
        queryFn: async () => {
            const data = await reviewApi.getReviews();
            return data.length;
        },
    });
};