import { lazy, Suspense } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DashboardPage } from './pages/DashboardPage';
import { WidgetErrorBoundary } from './components/WidgetErrorBoundary';
import { WidgetSkeleton } from './components/WidgetSkeleton';
import { useProductRoute } from './routing/useHashRoute';
import { getAccessToken, getUserIdFromToken } from './auth/readAuthContract';

const ProductDetail = lazy(() => import('mfe_product_detail/ProductDetail'));

const queryClient = new QueryClient();

function Routes() {
    const { productId, navigate } = useProductRoute();

    if (!productId) {
        return <DashboardPage />;
    }

    const session = {
        authToken: getAccessToken() ?? '',
        userId: getUserIdFromToken() ?? '',
        role: 'CUSTOMER' as const,
    };

    return (
        <div className="min-h-screen bg-gray-100">
            <header className="bg-white shadow-sm p-4 mb-6">
                <div className="max-w-6xl mx-auto flex items-center">
                    <h1 className="text-2xl font-bold tracking-tight">
                        <span style={{ color: '#000000' }}>Shop</span>
                        <span style={{ color: '#1D4ED8' }}>Bridge</span>
                    </h1>
                </div>
            </header>
            <main className="max-w-6xl mx-auto px-4 pb-12">
                <WidgetErrorBoundary widgetName="Ürün Detayı">
                    <Suspense fallback={<WidgetSkeleton />}>
                        <ProductDetail session={session} productId={productId} onNavigate={navigate} />
                    </Suspense>
                </WidgetErrorBoundary>
            </main>
        </div>
    );
}

function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <Routes />
        </QueryClientProvider>
    );
}

export default App;