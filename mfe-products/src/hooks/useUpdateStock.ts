import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makeProductApi } from '../api/productApi';
import { useAuthToken } from '../auth/useAuthToken';

export function useUpdateStock(storeId: string) {
    const token = useAuthToken();
    const queryClient = useQueryClient();
    const api = useMemo(
        () => makeProductApi(createApiClient(() => token)),
        [token],
    );

    return useMutation({
        // DÜZELTİLDİ: backend UpdateStockRequest record'u newStock bekliyor
        mutationFn: ({ id, newStock }: { id: string; newStock: number }) =>
            api.updateStock(id, { newStock }),
        onSuccess: () => {
            // İlgili query'leri tazele
            queryClient.invalidateQueries({ queryKey: ['store-products', storeId] });
            queryClient.invalidateQueries({ queryKey: ['store-product-count', storeId] });
        },
    });
}