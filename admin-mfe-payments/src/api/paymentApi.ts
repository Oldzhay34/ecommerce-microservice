import { AxiosInstance } from 'axios';
import { ENDPOINTS } from './endpoints';
import { PaymentResponse } from '../types';

export const makePaymentApi = (client: AxiosInstance) => {
    return {
        getPayments: async (): Promise<PaymentResponse[]> => {
            const response = await client.get<PaymentResponse[]>(ENDPOINTS.payments);
            return response.data;
        },
        approveRefund: async (id: string): Promise<void> => {
            await client.patch<void>(`${ENDPOINTS.payments}/${id}/refund-approve`);
        },
    };
};