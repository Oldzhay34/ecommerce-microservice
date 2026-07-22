// [Varsayım] Product (picker için)
export interface Product {
    id: string;
    name: string;
    category: string;
    price: number;
    stock: number;
    storeId: string;
    description?: string;
}

// [Varsayım] ReviewResponse alan adları
export interface ReviewResponse {
    id: string;
    productId: string;
    customerId: string;
    rating: number;
    comment: string;
    reply?: string | null;
    createdAt?: string;
}

// GET /api/reviews/product/{productId} yanıtı
export interface ProductReviewsResponse {
    averageRating: number;
    reviews: ReviewResponse[];
}

export interface StoreReplyRequest {
    replyText: string;
}

export interface ApiErrorResponse {
    timestamp: string; status: number; error: string; message: string; path: string;
}

export interface StoreSession {
    authToken: string; storeId: string; role: 'STORE';
}