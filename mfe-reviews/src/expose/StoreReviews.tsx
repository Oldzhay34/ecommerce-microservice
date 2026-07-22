import { useState } from 'react';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useStoreProducts } from '../hooks/useStoreProducts';
import { useProductReviews } from '../hooks/useProductReviews';
import { ProductPicker } from '../components/ProductPicker';
import { ReviewItem } from '../components/ReviewItem';
import { StarRating } from '../components/StarRating';
import { Toast } from '../components/Toast';
import type { StoreSession } from '../types';

function Skeleton() {
    return (
        <div className="flex flex-col gap-3">
            {[0, 1, 2].map((i) => <div key={i} className="h-28 rounded-card sb-shimmer" />)}
        </div>
    );
}

function StoreReviewsInner({ session }: { session: StoreSession }) {
    const [productId, setProductId] = useState('');
    const [toast, setToast] = useState<string | null>(null);
    const showToast = (m: string) => { setToast(m); setTimeout(() => setToast(null), 2500); };

    const { data: products, isLoading: productsLoading } = useStoreProducts(session.storeId);
    const { data: reviewsData, isLoading: reviewsLoading, isError } = useProductReviews(
        productId || null,
    );

    return (
        <div>
            <ProductPicker
                products={products ?? []}
                value={productId}
                onChange={setProductId}
                isLoading={productsLoading}
            />

            {!productId && (
                <div className="text-muted text-sm">
                    Değerlendirmelerini görmek için bir ürün seçin.
                </div>
            )}

            {productId && reviewsLoading && <Skeleton />}

            {productId && isError && (
                <div className="text-err-tx text-sm">
                    Değerlendirmeler yüklenirken bir hata oluştu.
                </div>
            )}

            {productId && !reviewsLoading && reviewsData && (
                <>
                    <div className="flex items-center gap-2 mb-4">
            <span className="text-ink text-sm font-semibold">
              Ortalama Puan: {reviewsData.averageRating.toFixed(1)} / 5
            </span>
                        <StarRating rating={reviewsData.averageRating} />
                    </div>

                    {reviewsData.reviews.length === 0 ? (
                        <div className="text-muted text-sm">
                            Bu ürün için henüz değerlendirme yapılmamış.
                        </div>
                    ) : (
                        <div className="flex flex-col gap-3">
                            {reviewsData.reviews.map((r) => (
                                <ReviewItem
                                    key={r.id}
                                    review={r}
                                    productId={productId}
                                    onToast={showToast}
                                />
                            ))}
                        </div>
                    )}
                </>
            )}

            {toast && <Toast message={toast} />}
        </div>
    );
}

export default function StoreReviews({ session }: { session: StoreSession }) {
    return (
        <AuthTokenProvider token={session.authToken}>
            <StoreReviewsInner session={session} />
        </AuthTokenProvider>
    );
}