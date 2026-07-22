import { useQuery } from '@tanstack/react-query';
import { productApi } from '../../../api/productApi';

export function useProducts(keyword: string, category: string) {
    return useQuery({
        queryKey: ['products', keyword, category],
        queryFn: () => productApi.search({ keyword, category }),
        staleTime: 60 * 1000,
    });
}