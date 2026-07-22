import { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import { ReviewResponse, ModerateReviewRequest } from '../types';

export const makeReviewApi = (client: AxiosInstance) => {
    return {
        getReviews: async (): Promise<ReviewResponse[]> => {
            const response = await client.get<ReviewResponse[]>(`${ENDPOINTS.reviews}/all`);
            return response.data;
        },
        moderateReview: async (id: string, status: ModerateReviewRequest['status']): Promise<void> => {
            await client.patch<void>(`${ENDPOINTS.reviews}/${id}/moderate`, { status });
        },
    };
};