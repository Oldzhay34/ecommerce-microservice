import type { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import type { Product } from '../types';

// ProductPicker için mağaza ürünleri (search + client tarafında storeId filtresi).
export function makeProductApi(client: AxiosInstance) {
    return {
        async getStoreProducts(storeId: string): Promise<Product[]> {
            const res = await client.get<Product[]>(ENDPOINTS.productSearch);
            return res.data.filter((p) => p.storeId === storeId);
        },
    };
}