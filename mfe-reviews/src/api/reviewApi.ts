import type { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import type { ProductReviewsResponse, StoreReplyRequest } from '../types';

export function makeReviewApi(client: AxiosInstance) {
    return {
        async getByProduct(productId: string): Promise<ProductReviewsResponse> {
            const res = await client.get<ProductReviewsResponse>(
                ENDPOINTS.productReviews(productId),
            );
            return res.data;
        },
        async reply(id: string, body: StoreReplyRequest): Promise<void> {
            await client.patch(ENDPOINTS.reviewReply(id), body);
        },
    };
}