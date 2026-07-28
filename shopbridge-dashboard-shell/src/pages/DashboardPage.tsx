import { lazy, Suspense } from 'react';
import { useNavigate } from 'react-router-dom';
import { WidgetErrorBoundary } from '../components/WidgetErrorBoundary';
import { WidgetSkeleton } from '../components/WidgetSkeleton';
import { SectionHeader } from '../components/SectionHeader';
import { TopBar } from '../components/TopBar';
import { useSessionStore } from '../app/session/store';

const OrdersWidget = lazy(() => import('mfe_orders/Widget'));
const CartWidget = lazy(() => import('mfe_cart/Widget'));
const PaymentsWidget = lazy(() => import('mfe_payments/Widget'));
const ReviewsWidget = lazy(() => import('mfe_reviews/Widget'));
const ProductCatalog = lazy(() => import('mfe_products/Widget'));

export function DashboardPage() {
    const navigate = useNavigate();
    const clear = useSessionStore((s) => s.clear);

    const handleLogout = () => {
        clear();
        navigate('/');
    };

    return (
        <div className="min-h-screen bg-canvas">
            <TopBar onLogout={handleLogout} />

            <main className="max-w-6xl mx-auto px-6 md:px-8 pt-12 pb-24">
                <section aria-labelledby="summary-heading" className="mb-14">
                    <SectionHeader>Hesap Özetim</SectionHeader>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                        <div className="bg-surface rounded-sb-lg p-6 shadow-sb hover:shadow-sb-lg transition-shadow duration-300">
                            <WidgetErrorBoundary widgetName="Siparişlerim">
                                <Suspense fallback={<WidgetSkeleton />}>
                                    <OrdersWidget />
                                </Suspense>
                            </WidgetErrorBoundary>
                        </div>

                        <div className="bg-surface rounded-sb-lg p-6 shadow-sb hover:shadow-sb-lg transition-shadow duration-300">
                            <WidgetErrorBoundary widgetName="Sepetim">
                                <Suspense fallback={<WidgetSkeleton />}>
                                    <CartWidget />
                                </Suspense>
                            </WidgetErrorBoundary>
                        </div>

                        <div className="bg-surface rounded-sb-lg p-6 shadow-sb hover:shadow-sb-lg transition-shadow duration-300">
                            <WidgetErrorBoundary widgetName="Ödemelerim">
                                <Suspense fallback={<WidgetSkeleton />}>
                                    <PaymentsWidget />
                                </Suspense>
                            </WidgetErrorBoundary>
                        </div>

                        <div className="bg-surface rounded-sb-lg p-6 shadow-sb hover:shadow-sb-lg transition-shadow duration-300">
                            <WidgetErrorBoundary widgetName="Yorumlarım">
                                <Suspense fallback={<WidgetSkeleton />}>
                                    <ReviewsWidget />
                                </Suspense>
                            </WidgetErrorBoundary>
                        </div>
                    </div>
                </section>

                <section aria-labelledby="catalog-heading">
                    <SectionHeader>Keşfet</SectionHeader>
                    <div className="bg-surface rounded-sb-lg p-6 md:p-8 shadow-sb">
                        <WidgetErrorBoundary widgetName="Ürünler">
                            <Suspense fallback={<WidgetSkeleton />}>
                                <ProductCatalog />
                            </Suspense>
                        </WidgetErrorBoundary>
                    </div>
                </section>
            </main>
        </div>
    );
}
