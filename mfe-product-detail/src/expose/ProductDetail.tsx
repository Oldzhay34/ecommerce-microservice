import { useCallback, useEffect, useState, Suspense, lazy } from 'react';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useProductDetail } from '../hooks/useProductDetail';
import { useAddToCart } from '../hooks/useAddToCart';
import { isNotFound } from '../api/client';
import { clampQuantity, MIN_QUANTITY } from '../utils/quantity';
import { ProductInfoPanel } from '../components/ProductInfoPanel';
import { DetailSkeleton } from '../components/DetailSkeleton';
import { ProductNotFound } from '../components/ProductNotFound';
import { ErrorBanner } from '../components/ErrorBanner';
import { Toast } from '../components/Toast';
import type { CustomerSession } from '../types';
import '../index.css';

const RemoteProductGallery = lazy(() => import('mfe_media_gallery/ProductGallery'));
const ProductReviews = lazy(() => import('mfe_reviews/ProductReviews'));

const GalleryFallback = () => (
    <div
        className="animate-pulse"
        style={{ width: '100%', aspectRatio: '1/1', backgroundColor: '#F3F4F6', borderRadius: 12 }}
    />
);

const ReviewsFallback = () => (
    <div
        className="animate-pulse"
        style={{ width: '100%', height: 192, backgroundColor: '#F3F4F6', borderRadius: 12, marginTop: 24 }}
    />
);

export interface ProductDetailProps {
    session: CustomerSession;
    productId: string;
    onNavigate: (path: string) => void;
}

export default function ProductDetail({ session, productId, onNavigate }: ProductDetailProps) {
    return (
        <AuthTokenProvider token={session.authToken}>
            <ProductDetailContent productId={productId} onNavigate={onNavigate} />
        </AuthTokenProvider>
    );
}

interface ProductDetailContentProps {
    productId: string;
    onNavigate: (path: string) => void;
}

function ProductDetailContent({ productId, onNavigate }: ProductDetailContentProps) {
    const [quantity, setQuantity] = useState<number>(MIN_QUANTITY);
    const [toastVisible, setToastVisible] = useState(false);

    const query = useProductDetail(productId);
    const product = query.data ?? null;
    const stock = product?.stock ?? 0;

    const handleAddToCartSuccess = useCallback(() => {
        setQuantity(MIN_QUANTITY);
        setToastVisible(true);
    }, []);

    const mutation = useAddToCart({ productId, onSuccess: handleAddToCartSuccess });

    useEffect(() => {
        if (!product) {
            return;
        }
        setQuantity((current) => clampQuantity(current, product.stock));
    }, [product]);

    const handleBack = useCallback(() => {
        onNavigate('/dashboard');
    }, [onNavigate]);

    const handleGoToCart = useCallback(() => {
        setToastVisible(false);
        onNavigate('/cart');
    }, [onNavigate]);

    const handleDismissToast = useCallback(() => {
        setToastVisible(false);
    }, []);

    const handleQuantityChange = useCallback(
        (next: number) => {
            setQuantity(clampQuantity(next, stock));
        },
        [stock],
    );

    const handleAddToCart = useCallback(() => {
        if (!product) return;
        // DÜZELTME BURADA: Sepete eklerken price bilgisini de gönderiyoruz
        mutation.mutate({
            productId,
            quantity: clampQuantity(quantity, stock),
            price: product.price
        });
    }, [mutation, productId, quantity, stock, product]);

    const backLink = (
        <button
            type="button"
            onClick={handleBack}
            className="hover:!text-[#111827]"
            style={{
                fontSize: 14,
                fontWeight: 600,
                color: '#6B7280',
                background: 'transparent',
                border: 'none',
                padding: 0,
                cursor: 'pointer',
            }}
        >
            ← Ürünlere Dön
        </button>
    );

    const cardStyle: React.CSSProperties = {
        backgroundColor: '#FFFFFF',
        border: '1px solid #E5E7EB',
        borderRadius: 12,
        padding: '24px 28px',
    };

    return (
        <div style={{ width: '100%' }}>
            <style>{`
        .sb-detail-grid {
          display: grid;
          grid-template-columns: minmax(0, 420px) minmax(0, 1fr);
          gap: 32px;
          align-items: start;
        }
        @media (max-width: 900px) {
          .sb-detail-grid {
            grid-template-columns: 1fr;
          }
        }
      `}</style>

            {backLink}
            <div style={{ height: 20 }} />

            {query.isPending ? (
                <div style={cardStyle}>
                    <DetailSkeleton />
                </div>
            ) : null}

            {!query.isPending && query.isError && isNotFound(query.error) ? (
                <div style={cardStyle}>
                    <ProductNotFound onBack={handleBack} />
                </div>
            ) : null}

            {!query.isPending && query.isError && !isNotFound(query.error) ? (
                <div style={cardStyle}>
                    <ErrorBanner message={query.error.message}>
                        <button
                            type="button"
                            onClick={() => {
                                void query.refetch();
                            }}
                            className="hover:bg-[#F9FAFB]"
                            style={{
                                height: 32,
                                borderRadius: 8,
                                fontWeight: 600,
                                fontSize: 13,
                                padding: '0 12px',
                                backgroundColor: '#FFFFFF',
                                border: '1px solid #D1D5DB',
                                color: '#111827',
                                cursor: 'pointer',
                            }}
                        >
                            Tekrar Dene
                        </button>
                    </ErrorBanner>
                </div>
            ) : null}

            {!query.isPending && !query.isError && product === null ? (
                <div style={cardStyle}>
                    <ProductNotFound onBack={handleBack} />
                </div>
            ) : null}

            {!query.isPending && !query.isError && product !== null ? (
                <>
                    <div style={cardStyle}>
                        <div className="sb-detail-grid">
                            <Suspense fallback={<GalleryFallback />}>
                                <RemoteProductGallery productId={product.id} />
                            </Suspense>

                            <ProductInfoPanel
                                product={product}
                                quantity={quantity}
                                onQuantityChange={handleQuantityChange}
                                onAddToCart={handleAddToCart}
                                isPending={mutation.isPending}
                                errorSlot={
                                    mutation.isError ? <ErrorBanner message={mutation.error.message} /> : null
                                }
                            />
                        </div>
                    </div>

                    <Suspense fallback={<ReviewsFallback />}>
                        <ProductReviews productId={product.id} />
                    </Suspense>
                </>
            ) : null}

            {toastVisible ? (
                <Toast
                    message="Ürün sepete eklendi."
                    actionLabel="Sepete Git"
                    onAction={handleGoToCart}
                    onDismiss={handleDismissToast}
                />
            ) : null}
        </div>
    );
}