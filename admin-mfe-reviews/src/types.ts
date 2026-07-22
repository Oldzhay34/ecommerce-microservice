export interface AdminSession {
    authToken: string;
    userId: string;
    role: 'ADMIN';
}

export interface ReviewResponse {
    id: string;
    productId: string;
    customerId: string;
    rating: number;
    comment: string;
    status: string;
    storeReplyText: string | null;
    storeRepliedAt: string | null;
    createdAt: string;
}

export interface ModerateReviewRequest {
    status: 'ACTIVE' | 'HIDDEN';
}

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
}