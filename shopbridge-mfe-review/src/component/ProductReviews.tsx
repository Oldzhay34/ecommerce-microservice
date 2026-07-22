import { useProductReviews } from '../hooks/useProductReviews';
import { RatingSummary } from './RatingSummary';
import { ReviewItem } from './ReviewItem';
import { ErrorBanner, Skeleton } from '../ui/components';
import type { ProductReviewsProps } from '../types';

/**
 * Ürün detay ekranının altında görünen yorum listesi.
 *
 * Public: token gerekmez. Kendi QueryClientProvider'ını KURMAZ — host'un singleton
 * cache'ini kullanır (federation'da @tanstack/react-query singleton).
 *
 * Görsel dil: ürün detay kartıyla hizalı — beyaz kart, yumuşak border, aynı köşe yarıçapı.
 */
export default function ProductReviews({ productId }: ProductReviewsProps) {
    const { data, isLoading, isError } = useProductReviews(productId);

    if (isLoading) {
        return (
            <section className="bg-white border border-gray-200 rounded-xl p-6 mt-6">
                <Skeleton height="h-6" />
                <div className="mt-4">
                    <Skeleton height="h-32" />
                </div>
            </section>
        );
    }

    if (isError) {
        return (
            <section className="bg-white border border-gray-200 rounded-xl p-6 mt-6">
                <ErrorBanner message="Değerlendirmeler yüklenemedi." />
            </section>
        );
    }

    const reviews = data?.reviews ?? [];
    const averageRating = data?.averageRating ?? 0;

    return (
        <section className="bg-white border border-gray-200 rounded-xl p-6 mt-6">
            <div className="flex items-baseline justify-between gap-4 flex-wrap">
                <h2 className="text-lg font-semibold text-gray-900">Değerlendirmeler</h2>
                {reviews.length > 0 && (
                    <RatingSummary averageRating={averageRating} totalCount={reviews.length} />
                )}
            </div>

            {reviews.length === 0 ? (
                <p className="text-gray-500 text-sm text-center py-10">
                    Bu ürün için henüz değerlendirme yapılmamış.
                </p>
            ) : (
                <ul className="space-y-5 mt-6">
                    {reviews.map((review, index) => (
                        <ReviewItem key={review.id} review={review} index={index} />
                    ))}
                </ul>
            )}
        </section>
    );
}