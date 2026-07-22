import { useQuery } from '@tanstack/react-query';
import { getUserIdFromToken } from '../auth/readAuthContract';
import { apiClient } from '../api/client';
import { useProductNames } from './useProductNames';
import { Card, ErrorBanner, Skeleton } from '../ui/components';

interface ReviewResponse {
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

export default function ReviewsWidget() {
    const userId = getUserIdFromToken();

    const { data: reviews, isLoading, isError } = useQuery({
        queryKey: ['reviews', 'me'],
        queryFn: async () => {
            const { data } = await apiClient.get<ReviewResponse[]>('/api/reviews/me');
            return data;
        },
        enabled: !!userId,
    });

    const { nameMap } = useProductNames(reviews?.map((r) => r.productId) ?? []);

    if (!userId) return <ErrorBanner message="Oturum bilgisi bulunamadı." />;
    if (isLoading) return <Skeleton height="h-48" />;
    if (isError) return <ErrorBanner message="Yorumlarınız yüklenemedi." />;
    if (!reviews?.length)
        return <Card><p className="text-gray-500 text-center">Henüz bir ürün değerlendirmesi yapmadınız.</p></Card>;

    return (
        <Card>
            <h3 className="font-semibold text-lg mb-4 text-gray-800">Son Değerlendirmelerim</h3>
            <ul className="space-y-4">
                {reviews.map((review) => (
                    <li key={review.id} className="border-b pb-3 last:border-0 last:pb-0">
                        <div className="flex justify-between items-start mb-1">
                            <p className="font-medium text-gray-900">
                                {nameMap[review.productId] ?? `Ürün: ${review.productId}`}
                            </p>
                            <div className="flex text-yellow-400">
                                {[...Array(5)].map((_, i) => (
                                    <svg key={i} className={`w-4 h-4 ${i < review.rating ? 'fill-current' : 'text-gray-300 fill-current'}`} viewBox="0 0 20 20">
                                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                                    </svg>
                                ))}
                            </div>
                        </div>
                        {review.comment && <p className="text-sm text-gray-600 italic">"{review.comment}"</p>}
                        {review.storeReplyText && (
                            <p className="mt-2 text-sm text-gray-700 bg-gray-50 rounded p-2">
                                <span className="font-medium">Mağaza yanıtı:</span> {review.storeReplyText}
                            </p>
                        )}
                    </li>
                ))}
            </ul>
        </Card>
    );
}