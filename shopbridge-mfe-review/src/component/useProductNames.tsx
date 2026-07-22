import { useQueries } from '@tanstack/react-query';
import { apiClient } from '../api/client';

interface Product {
    id: string;
    name: string;
    category: string;
    price: number;
    stock: number;
}

export function useProductNames(productIds: string[]) {
    const uniqueIds = [...new Set(productIds)];

    const results = useQueries({
        queries: uniqueIds.map((id) => ({
            queryKey: ['product', id],
            queryFn: async () => {
                const { data } = await apiClient.get<Product>(`/api/v1/products/${id}`);
                return data;
            },
            staleTime: 5 * 60 * 1000,
        })),
    });

    const nameMap: Record<string, string> = {};
    results.forEach((r, i) => {
        if (r.data) nameMap[uniqueIds[i]] = r.data.name;
    });

    const isLoading = results.some((r) => r.isLoading);
    return { nameMap, isLoading };
}