export interface AdminSession {
    authToken: string;
    userId: string;
    role: 'ADMIN';
}

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
}

export type OrderStatus = 'PENDING' | 'APPROVED' | 'SHIPPED' | 'CANCELLED';

export interface OrderItemDto {
    productId: string;
    storeId: string;
    quantity: number;
    price: number;
}

export interface OrderResponse {
    id: string;
    userId: string;
    status: OrderStatus;
    totalAmount: number;
    createdAt: string;
    items: OrderItemDto[];
}

export interface UpdateOrderStatusRequest {
    status: 'SHIPPED' | 'CANCELLED';
}

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
}