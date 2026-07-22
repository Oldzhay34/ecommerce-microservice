import type { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import type { Product, UpdateStockRequest } from '../types';

// Adapter fonksiyonları — bileşen doğrudan axios çağırmaz.
export function makeProductApi(client: AxiosInstance) {
    return {
        // Mağaza ürünleri: search + client tarafında storeId filtresi
        async getMyProducts(
            storeId: string,
            params?: { keyword?: string; category?: string },
        ): Promise<Product[]> {
            const res = await client.get<Product[]>(ENDPOINTS.productSearch, {
                params: { keyword: params?.keyword, category: params?.category },
            });
            return res.data.filter((p) => p.storeId === storeId);
        },

        async updateStock(id: string, body: UpdateStockRequest): Promise<void> {
            await client.put(ENDPOINTS.productStock(id), body);
        },
    };
}

export type ProductApi = ReturnType<typeof makeProductApi>;