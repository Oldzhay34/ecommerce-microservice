import { lazy, Suspense, useEffect } from 'react';
import { WidgetErrorBoundary } from '../components/WidgetErrorBoundary';
import { WidgetSkeleton } from '../components/WidgetSkeleton';
import { getAccessToken } from '../auth/readAuthContract';

const OrdersWidget = lazy(() => import('mfe_orders/Widget'));
const CartWidget = lazy(() => import('mfe_cart/Widget'));
const PaymentsWidget = lazy(() => import('mfe_payments/Widget'));
const ReviewsWidget = lazy(() => import('mfe_reviews/Widget'));
const ProductCatalog = lazy(() => import('mfe_products/Widget'));

export function DashboardPage() {

    useEffect(() => {
        const token = getAccessToken();
        if (!token) {
            // [Varsayım: Local testlerde sayfayı yenileyip durmaması için yönlendirme yerine konsola uyarı bırakıldı.]
            console.warn("Auth Contract: Token bulunamadı. Kullanıcının Login uygulamasına (VITE_AUTH_APP_URL) yönlendirilmesi gerekir.");
        }
    }, []);

    return (
        <div className="min-h-screen bg-gray-100">
            <header className="bg-white shadow-sm p-4 mb-6">
                <div className="max-w-6xl mx-auto flex items-center">
                    <h1 className="text-2xl font-bold tracking-tight">
                        <span className="text-black">Shop</span>
                        <span className="text-blue-600">Bridge</span>
                    </h1>
                </div>
            </header>

            <main className="max-w-6xl mx-auto px-4 pb-12">
                {/* Kişisel özet kartları: siparişler, sepet, ödemeler, yorumlar */}
                <section aria-labelledby="summary-heading" className="mb-10">
                    <h2 id="summary-heading" className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-4">
                        Hesap Özetim
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <WidgetErrorBoundary widgetName="Siparişlerim">
                            <Suspense fallback={<WidgetSkeleton />}>
                                <OrdersWidget />
                            </Suspense>
                        </WidgetErrorBoundary>

                        <WidgetErrorBoundary widgetName="Sepetim">
                            <Suspense fallback={<WidgetSkeleton />}>
                                <CartWidget />
                            </Suspense>
                        </WidgetErrorBoundary>

                        <WidgetErrorBoundary widgetName="Ödemelerim">
                            <Suspense fallback={<WidgetSkeleton />}>
                                <PaymentsWidget />
                            </Suspense>
                        </WidgetErrorBoundary>

                        <WidgetErrorBoundary widgetName="Yorumlarım">
                            <Suspense fallback={<WidgetSkeleton />}>
                                <ReviewsWidget />
                            </Suspense>
                        </WidgetErrorBoundary>
                    </div>
                </section>

                {/* Ürün kataloğu: tam genişlik, kendi başlıklı bölüm */}
                <section aria-labelledby="catalog-heading">
                    <h2 id="catalog-heading" className="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-4">
                        Keşfet
                    </h2>
                    <div className="bg-white rounded-lg border border-gray-100 shadow-sm p-6">
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