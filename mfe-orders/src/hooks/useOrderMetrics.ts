import { useMemo } from 'react';
import { useStoreOrders } from './useStoreOrders';

const SHIPPED_SET = new Set(['SHIPPED', 'DELIVERED']);
const PENDING_EXCLUDE = new Set(['SHIPPED', 'DELIVERED', 'CANCELLED']);

const tryFmt = new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' });

export function useOrderMetrics(storeId: string) {
    const { data: orders, isLoading, isError } = useStoreOrders(storeId);

    const metrics = useMemo(() => {
        const list = orders ?? [];
        // Bekleyen: durumu {SHIPPED,DELIVERED,CANCELLED} DIŞINDA
        const pending = list.filter((o) => !PENDING_EXCLUDE.has(o.status)).length;
        // Kargolanan: SHIPPED veya DELIVERED
        const shipped = list.filter((o) => SHIPPED_SET.has(o.status)).length;
        // Ciro: tüm siparişlerdeki bu mağazaya ait kalemlerin price*quantity toplamı
        const revenue = list.reduce((sum, o) => {
            const storeItems = o.items.filter((it) => it.storeId === storeId);
            return sum + storeItems.reduce((s, it) => s + it.price * it.quantity, 0);
        }, 0);
        return { pending, shipped, revenue: tryFmt.format(revenue) };
    }, [orders, storeId]);

    return { metrics, isLoading, isError };
}