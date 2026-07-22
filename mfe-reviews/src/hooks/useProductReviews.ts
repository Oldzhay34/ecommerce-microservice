import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makeReviewApi } from '../api/reviewApi';
import { useAuthToken } from '../auth/useAuthToken';

export function useProductReviews(productId: string | null) {
    const token = useAuthToken();
    const api = useMemo(() => makeReviewApi(createApiClient(() => token)), [token]);

    return useQuery({
        queryKey: ['product-reviews', productId],
        queryFn: () => api.getByProduct(productId as string),
        enabled: !!productId, // ürün seçilene kadar çalışmaz
    });
}