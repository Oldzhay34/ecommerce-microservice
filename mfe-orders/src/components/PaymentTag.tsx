import { useOrderPayment } from '../hooks/useOrderPayment';
import { StatusBadge } from './StatusBadge';

// Sipariş kartı içinde ödeme durumu rozeti; yoksa boş metni.
export function PaymentTag({ orderId }: { orderId: string }) {
    const { data, isLoading, isError } = useOrderPayment(orderId);

    if (isLoading) return <span className="text-muted text-xs">Ödeme yükleniyor…</span>;
    if (isError || !data || data.length === 0)
        return (
            <span className="text-muted text-xs">Bu siparişe ait ödeme bilgisi bulunamadı.</span>
        );

    const payment = data[0];
    return (
        <div className="flex items-center gap-2">
            <span className="text-muted text-xs">Ödeme:</span>
            <StatusBadge status={payment.status ?? 'PENDING'} />
        </div>
    );
}