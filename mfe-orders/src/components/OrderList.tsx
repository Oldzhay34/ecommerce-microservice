import { useState } from 'react';
import type { OrderResponse } from '../types';
import { OrderCard } from './OrderCard';
import { Toast } from './Toast';

function Skeleton() {
    return (
        <div className="flex flex-col gap-3">
            {[0, 1, 2].map((i) => <div key={i} className="h-32 rounded-card sb-shimmer" />)}
    </div>
);
}

export function OrderList({
                              orders, storeId, isLoading, isError,
                          }: {
    orders: OrderResponse[];
    storeId: string;
    isLoading: boolean;
    isError: boolean;
}) {
    const [toast, setToast] = useState<string | null>(null);
    const showToast = (m: string) => { setToast(m); setTimeout(() => setToast(null), 2500); };

    if (isLoading) return <Skeleton />;
    if (isError) return <div className="text-err-tx text-sm">Siparişler yüklenirken bir hata oluştu.</div>;
    if (orders.length === 0) return <div className="text-muted text-sm">Henüz siparişiniz bulunmuyor.</div>;

    return (
        <div className="flex flex-col gap-3">
            {orders.map((o) => (
                    <OrderCard key={o.id} order={o} storeId={storeId} onToast={showToast} />
))}
    {toast && <Toast message={toast} />}
    </div>
    );
    }