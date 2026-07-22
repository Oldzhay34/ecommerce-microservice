export interface AdminSession {
    authToken: string;
    userId: string;
    role: 'ADMIN';
}

export interface PaymentResponse {
    id: string;
    orderId: string;
    customerId: string;
    amount: number;
    status: string | null;
}

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
}