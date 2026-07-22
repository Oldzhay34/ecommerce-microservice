import type { AxiosInstance } from 'axios';
import { PRODUCT_BY_ID } from './endpoints';
import type { Product } from '../types';

/**
 * Adapter · Repository benzeri soyutlama.
 * Bileşenler doğrudan axios çağırmaz; bu fabrikadan geçer.
 */
export interface ProductApi {
    /**
     * GET /api/v1/products/{id} — public (@PreAuthorize yok).
     * [Varsayım: queryUseCase.findById(id) bulunamayan ürün için exception fırlatırsa 404 gelir;
     *  null dönerse 200 + boş body gelebilir. İkinci durum için null döndürülür,
     *  container `data == null` → ProductNotFound gösterir.]
     */
    getProductById(id: string): Promise<Product | null>;
}

export function makeProductApi(client: AxiosInstance): ProductApi {
    return {
        async getProductById(id: string): Promise<Product | null> {
            const response = await client.get<Product | null | ''>(PRODUCT_BY_ID(id));
            const data = response.data;

            if (data === null || data === undefined || data === '') {
                return null;
            }
            if (typeof data !== 'object' || typeof (data as Product).id !== 'string') {
                return null;
            }
            return data as Product;
        },
    };
}