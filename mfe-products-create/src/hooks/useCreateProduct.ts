import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createApiClient, type ApiError } from '../api/client';
import { makeProductApi } from '../api/productApi';
import { useAuthToken } from '../auth/useAuthToken';
import type { CreateProductRequest, Product } from '../types';

export function useCreateProduct(storeId: string) {
    const token = useAuthToken();
    const queryClient = useQueryClient(); // shell'in singleton cache'i (federation'da)
    const api = useMemo(
        () => makeProductApi(createApiClient(() => token)),
        [token],
    );

    return useMutation<Product, ApiError, CreateProductRequest>({
        mutationFn: (body) => api.createProduct(body),
        onSuccess: () => {
            // mfe-products'ın query anahtarlarıyla birebir — dashboard listesi tazelenir.
            queryClient.invalidateQueries({ queryKey: ['store-products', storeId] });
            queryClient.invalidateQueries({ queryKey: ['store-product-count', storeId] });
        },
    });
}