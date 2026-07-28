import { useState } from 'react';
import type { OrderResponse } from '../types';
import { StatusBadge } from './StatusBadge';
import { PaymentTag } from './PaymentTag';
import { ConfirmDialog } from './ConfirmDialog';
import { useProductNames } from '../hooks/useProductNames';
import { useShipOrder } from '../hooks/useShipOrder';
import { useCancelOrder } from '../hooks/useCancelOrder';

const priceFmt = new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' });
const dateFmt = new Intl.DateTimeFormat('tr-TR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

// Aksiyon uygunluğu: durum {SHIPPED,DELIVERED,CANCELLED} DIŞINDA ise
const ACTION_LOCK = new Set(['SHIPPED', 'DELIVERED', 'CANCELLED']);

export function OrderCard({
                              order, storeId, onToast,
                          }: {
    order: OrderResponse;
    storeId: string;
    onToast: (m: string) => void;
}) {
    const storeItems = order.items.filter((it) => it.storeId === storeId);
    const nameMap = useProductNames(storeItems.map((it) => it.productId));
    const subtotal = storeItems.reduce((s, it) => s + it.price * it.quantity, 0);
    const canAct = !ACTION_LOCK.has(order.status.toUpperCase());

    const ship = useShipOrder(storeId);
    const cancel = useCancelOrder(storeId);
    const [dialog, setDialog] = useState<null | 'ship' | 'cancel'>(null);

    const confirmShip = () =>
        ship.mutate(order.id, {
            onSuccess: () => { onToast('Sipariş kargolandı.'); setDialog(null); },
        });
    const confirmCancel = () =>
        cancel.mutate(order.id, {
            onSuccess: () => { onToast('Sipariş iptal edildi.'); setDialog(null); },
        });

    return (
        <div className="bg-surface border border-line rounded-card shadow-sb px-6 py-5">
            <div className="flex items-center justify-between">
                <div className="text-ink font-semibold">Sipariş #{order.id.slice(0, 8)}</div>
                <StatusBadge status={order.status} />
            </div>
            <div className="text-muted text-sm mt-1">{dateFmt.format(new Date(order.createdAt))}</div>

            <div className="mt-4 flex flex-col gap-1">
                {storeItems.map((it, i) => (
                    <div key={i} className="text-ink text-sm">
                        {it.quantity} × {nameMap[it.productId] ?? `Ürün ${it.productId.slice(0, 8)}`} — {priceFmt.format(it.price)}
                    </div>
                ))}
            </div>

            <div className="mt-3 text-ink font-semibold text-sm">
                Ara Toplam (Mağazanız): {priceFmt.format(subtotal)}
            </div>

            <div className="mt-3">
                <PaymentTag orderId={order.id} />
            </div>

            {canAct && (
                <div className="mt-4 flex gap-3">
                    <button
                        onClick={() => setDialog('ship')}
                        className="h-10 rounded-full font-semibold bg-brand text-white px-4 hover:bg-brand-dark"
                    >
                        Kargola
                    </button>
                    <button
                        onClick={() => setDialog('cancel')}
                        className="h-10 rounded-btn font-semibold bg-danger-soft border border-danger-line text-err-tx px-4"
                    >
                        İptal Et
                    </button>
                </div>
            )}

            {dialog === 'ship' && (
                <ConfirmDialog
                    message="Bu siparişi kargolamak istediğinize emin misiniz?"
                    confirmLabel="Kargola"
                    isPending={ship.isPending}
                    onConfirm={confirmShip}
                    onCancel={() => setDialog(null)}
                />
            )}
            {dialog === 'cancel' && (
                <ConfirmDialog
                    message="Bu siparişi iptal etmek istediğinize emin misiniz?"
                    confirmLabel="İptal Et"
                    danger
                    isPending={cancel.isPending}
                    onConfirm={confirmCancel}
                    onCancel={() => setDialog(null)}
                />
            )}
        </div>
    );
}