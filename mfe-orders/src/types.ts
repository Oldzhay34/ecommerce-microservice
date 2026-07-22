// Kesin (verilen controller/DTO):
export interface OrderItemDto {
    productId: string;
    storeId: string;
    quantity: number;
    price: number;
}
export interface OrderResponse {
    id: string;
    userId: string;
    status: string;
    totalAmount: number;
    createdAt: string;
    items: OrderItemDto[];
}
export interface PaymentResponse {
    id: string;
    orderId: string;
    customerId: string;
    amount: number;
    status: string | null;
}

// [Varsayım] Product (ad çözümü için)
export interface Product {
    id: string;
    name: string;
    category: string;
    price: number;
    stock: number;
    storeId: string;
    description?: string;
}

export interface ApiErrorResponse {
    timestamp: string; status: number; error: string; message: string; path: string;
}

export interface StoreSession {
    authToken: string; storeId: string; role: 'STORE';
}

export type OrderStatusChange = { status: 'SHIPPED' | 'CANCELLED' };