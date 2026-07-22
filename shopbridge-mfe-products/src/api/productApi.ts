import { apiClient } from './client';
import { ENDPOINTS } from './endpoints';
import type { Product } from '../features/catalog/types/product';

export interface SearchParams {
    keyword?: string;
    category?: string;
}

export const productApi = {
    async search(params: SearchParams = {}): Promise<Product[]> {
        const { data } = await apiClient.get<Product[]>(ENDPOINTS.products.search, {
            params: {
                keyword: params.keyword || undefined,
                category: params.category || undefined,
            },
        });
        return data;
    },

    async getById(id: string): Promise<Product> {
        const { data } = await apiClient.get<Product>(ENDPOINTS.products.byId(id));
        return data;
    },
};