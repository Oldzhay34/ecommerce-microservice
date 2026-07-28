import { lazy, Suspense } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { WidgetErrorBoundary } from '../components/WidgetErrorBoundary';
import { WidgetSkeleton } from '../components/WidgetSkeleton';
import { Logo } from '../components/Logo';
import { useSessionStore } from '../app/session/store';

const ProductDetail = lazy(() => import('mfe_product_detail/ProductDetail'));

export function ProductDetailPage() {
    const { productId } = useParams<{ productId: string }>();
    const navigate = useNavigate();
    const { authToken } = useSessionStore();

    if (!productId) return null;

    const session = {
        authToken: authToken ?? '',
        userId: '',
        role: 'CUSTOMER' as const,
    };

    return (
        <div className="min-h-screen bg-canvas">
            <header className="border-b border-border px-6 py-4">
                <div className="max-w-6xl mx-auto">
                    <Logo />
                </div>
            </header>
            <main className="max-w-6xl mx-auto px-6 py-8">
                <WidgetErrorBoundary widgetName="Ürün Detayı">
                    <Suspense fallback={<WidgetSkeleton />}>
                        <ProductDetail session={session} productId={productId} onNavigate={navigate} />
                    </Suspense>
                </WidgetErrorBoundary>
            </main>
        </div>
    );
}
