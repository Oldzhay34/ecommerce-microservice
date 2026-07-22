import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useProductCount } from '../hooks/useProductCount';
import { MetricCard } from '../components/MetricCard';
import type { StoreSession } from '../types';

function StoreProductCountInner({ session }: { session: StoreSession }) {
    const { data, isLoading } = useProductCount(session.storeId);
    return <MetricCard title="Toplam Ürün" value={isLoading ? '—' : data ?? 0} />;
}

export default function StoreProductCount({ session }: { session: StoreSession }) {
    return (
        <AuthTokenProvider token={session.authToken}>
            <StoreProductCountInner session={session} />
        </AuthTokenProvider>
    );
}