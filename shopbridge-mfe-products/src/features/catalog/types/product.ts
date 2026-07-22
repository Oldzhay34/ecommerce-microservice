export interface Product {
    id: string;
    storeId: string;
    name: string;
    category: string;
    price: number;
    stock: number;
}

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
}