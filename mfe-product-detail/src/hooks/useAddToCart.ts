import {
    useMutation,
    useQueryClient,
    type UseMutationResult,
} from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApiClient } from '../auth/useAuthToken';
import { makeCartApi } from '../api/cartApi';
import { productDetailQueryKey } from './useProductDetail';
import type { AddToCartRequest, ApiErrorResponse } from '../types';

export interface UseAddToCartOptions {
    productId: string;
    onSuccess?: () => void;
}

/**
 * POST /api/v1/cart/items — token gerektirir (ROLE_CUSTOMER).
 * Başarıda: ['cart'] ve ['product-detail', productId] invalidate edilir
 * (stok değişmiş olabilir). Otomatik yönlendirme YOK.
 */
export function useAddToCart({
                                 productId,
                                 onSuccess,
                             }: UseAddToCartOptions): UseMutationResult<void, ApiErrorResponse, AddToCartRequest> {
    const client = useApiClient();
    const queryClient = useQueryClient();
    const cartApi = useMemo(() => makeCartApi(client), [client]);

    return useMutation<void, ApiErrorResponse, AddToCartRequest>({
        mutationFn: (request: AddToCartRequest) => cartApi.addItem(request),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: ['cart'] });
            void queryClient.invalidateQueries({ queryKey: productDetailQueryKey(productId) });
            onSuccess?.();
        },
    });
}