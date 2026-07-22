import { useQuery } from '@tanstack/react-query';
import { publicApiClient } from '../api/publicClient';
import type { ProductReviewsResponse } from '../types';

/**
 * GET /api/reviews/product/{productId} -> { averageRating, reviews }
 *
 * Public: token gerekmez. Gateway JwtAuthenticationGlobalFilter'ında
 * PUBLIC_ENDPOINTS listesine eklenmelidir (bkz. GATEWAY ENTEGRASYON NOTU).
 */
export function useProductReviews(productId: string) {
    return useQuery<ProductReviewsResponse, Error>({
        queryKey: ['product-reviews', productId],
        queryFn: async () => {
            const { data } = await publicApiClient.get<ProductReviewsResponse>(
                `/api/reviews/product/${productId}`,
            );
            return data;
        },
        enabled: Boolean(productId),
        staleTime: 60_000,
        retry: 1,
    });
}