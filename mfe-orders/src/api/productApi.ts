import type { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import type { Product } from '../types';

// Kalem adı çözümü (domain'ler arası). [Varsayım: ideal olan mfe_products'ın expose etmesi]
export function makeProductApi(client: AxiosInstance) {
    return {
        async getById(id: string): Promise<Product> {
            const res = await client.get<Product>(ENDPOINTS.productById(id));
            return res.data;
        },
    };
}