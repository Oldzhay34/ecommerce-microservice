import type { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import type { CreateProductRequest, Product } from '../types';

// Adapter fonksiyonları — bileşen doğrudan axios çağırmaz.
export function makeProductApi(client: AxiosInstance) {
    return {
        // storeId GÖNDERİLMEZ — backend extractUserIdFromSecurityContext() ile JWT'den çıkarır.
        async createProduct(body: CreateProductRequest): Promise<Product> {
            const res = await client.post<Product>(ENDPOINTS.productCreate, body);
            return res.data; // 201 Created + Product body
        },
    };
}

export type ProductApi = ReturnType<typeof makeProductApi>;