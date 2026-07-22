import type { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import type { PaymentResponse } from '../types';

export function makePaymentApi(client: AxiosInstance) {
    return {
        async getByOrder(orderId: string): Promise<PaymentResponse[]> {
            const res = await client.get<PaymentResponse[]>(ENDPOINTS.paymentsByOrder, {
                params: { orderId },
            });
            return res.data;
        },
    };
}