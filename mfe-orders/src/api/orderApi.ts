import type { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import type { OrderResponse, OrderStatusChange } from '../types';

export function makeOrderApi(client: AxiosInstance) {
    return {
        async getStoreOrders(storeId: string): Promise<OrderResponse[]> {
            const res = await client.get<OrderResponse[]>(ENDPOINTS.storeOrders(storeId));
            return res.data;
        },
        async updateOrderStatus(id: string, body: OrderStatusChange): Promise<OrderResponse> {
            const res = await client.patch<OrderResponse>(ENDPOINTS.orderStatus(id), body);
            return res.data;
        },
    };
}