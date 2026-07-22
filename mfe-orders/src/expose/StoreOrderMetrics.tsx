import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useOrderMetrics } from '../hooks/useOrderMetrics';
import { MetricCard } from '../components/MetricCard';
import type { StoreSession } from '../types';

function StoreOrderMetricsInner({ session }: { session: StoreSession }) {
    const { metrics, isLoading } = useOrderMetrics(session.storeId);
    const dash = isLoading ? '—' : undefined;
    return (
        <>
            <MetricCard title="Bekleyen Sipariş" value={dash ?? metrics.pending} />
    <MetricCard title="Kargolanan Sipariş" value={dash ?? metrics.shipped} />
    <MetricCard title="Toplam Ciro" value={dash ?? metrics.revenue} />
    </>
);
}

export default function StoreOrderMetrics({ session }: { session: StoreSession }) {
    return (
        <AuthTokenProvider token={session.authToken}>
        <StoreOrderMetricsInner session={session} />
    </AuthTokenProvider>
);
}