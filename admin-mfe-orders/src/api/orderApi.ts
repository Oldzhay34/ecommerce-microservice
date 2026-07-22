import { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import { Page, OrderResponse, UpdateOrderStatusRequest } from '../types';

export const makeOrderApi = (client: AxiosInstance) => {
    return {
        getOrders: async (page: number, size = 10): Promise<Page<OrderResponse>> => {
            // Backend düz List<OrderResponse> dönüyor, pagination yok.
            const response = await client.get<OrderResponse[]>(ENDPOINTS.orders);
            const all = response.data ?? [];

            // En yeni sipariş üstte
            const sorted = [...all].sort(
                (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
            );

            const totalElements = sorted.length;
            const totalPages = Math.max(1, Math.ceil(totalElements / size));
            const start = page * size;
            const content = sorted.slice(start, start + size);

            return {
                content,
                totalElements,
                totalPages,
                number: page,
                size,
                first: page === 0,
                last: page >= totalPages - 1,
            } as Page<OrderResponse>;
        },
        updateStatus: async (id: string, status: UpdateOrderStatusRequest['status']): Promise<OrderResponse> => {
            const response = await client.patch<OrderResponse>(`${ENDPOINTS.orders}/${id}/status`, {
                status,
            });
            return response.data;
        },
    };
};