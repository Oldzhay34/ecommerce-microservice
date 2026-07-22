import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useStoreOrders } from '../hooks/useStoreOrders';
import { OrderList } from '../components/OrderList';
import type { StoreSession } from '../types';

function StoreOrdersInner({ session }: { session: StoreSession }) {
    const { data, isLoading, isError } = useStoreOrders(session.storeId);
    return (
        <OrderList
            orders={data ?? []}
            storeId={session.storeId}
            isLoading={isLoading}
            isError={isError}
        />
    );
}

export default function StoreOrders({ session }: { session: StoreSession }) {
    return (
        <AuthTokenProvider token={session.authToken}>
            <StoreOrdersInner session={session} />
        </AuthTokenProvider>
    );
}