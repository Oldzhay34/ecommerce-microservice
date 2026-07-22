// Review servisi API sözleşmesi — backend ReviewResponse ile birebir.
export interface ReviewResponse {
    id: string;
    productId: string;
    customerId: string;
    rating: number;
    comment: string;
    status: string;
    storeReplyText?: string;
    storeRepliedAt?: string;
    createdAt: string;
}

// GET /api/reviews/product/{productId} yanıtı.
// Controller Map<String, Object> döndürüyor: { averageRating, reviews }
export interface ProductReviewsResponse {
    averageRating: number;
    reviews: ReviewResponse[];
}

export interface ProductReviewsProps {
    productId: string;
}