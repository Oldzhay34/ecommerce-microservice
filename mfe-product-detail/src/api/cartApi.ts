import type { AxiosInstance } from 'axios';
import { CART_ITEMS } from './endpoints';
import type { AddToCartRequest } from '../types';

/**
 * Adapter · Cart Service.
 * [Varsayım: Cart controller kodu doğrulanmamıştır. POST /api/v1/cart/items ve
 *  AddToCartRequest { productId, quantity } türetilmiş varsayımdır.
 *  customerId body'de GÖNDERİLMEZ — backend principal'dan çıkarır
 *  (Product servisindeki extractUserIdFromSecurityContext() deseniyle tutarlı).]
 */
export interface CartApi {
    addItem(request: AddToCartRequest): Promise<void>;
}

export function makeCartApi(client: AxiosInstance): CartApi {
    return {
        async addItem(request: AddToCartRequest): Promise<void> {
            await client.post(CART_ITEMS, request);
        },
    };
}