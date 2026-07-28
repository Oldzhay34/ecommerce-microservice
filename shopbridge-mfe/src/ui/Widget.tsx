import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getUserIdFromToken } from '../auth/readAuthContract';
import { apiClient } from '../api/client';
import { Card, Button, ErrorBanner, Skeleton } from '../ui/components';

// Backend OrderResponse ile birebir eşleşen tip
interface OrderResponse {
    id: string;
    userId: string;
    status: 'PENDING' | 'PAID' | 'SHIPPED' | 'CANCELLED' | string;
    totalAmount: number;
    createdAt: string;       // LocalDateTime -> ISO string
    items: Array<{
        productId: string;
        storeId: string;
        quantity: number;
        price: number;
    }>;
}

export default function OrdersWidget() {
    const userId = getUserIdFromToken();
    const queryClient = useQueryClient();

    const { data: orders, isLoading, isError } = useQuery({
        queryKey: ['orders', userId],
        queryFn: async () => {
            const res = await apiClient.get<OrderResponse[]>(`/api/orders/customer/${userId}`);
            return res.data;
        },
        enabled: !!userId,
    });

    const cancelMutation = useMutation({
        mutationFn: async (orderId: string) => {
            await apiClient.patch(`/api/orders/${orderId}/status`, { status: 'CANCELLED' });
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['orders', userId] }),
    });

    if (!userId) return <ErrorBanner message="Kullanıcı kimliği bulunamadı." />;
    if (isLoading) return <Skeleton height="h-48" />;
    if (isError) return <ErrorBanner message="Siparişler yüklenirken bir hata oluştu." />;
    if (!orders?.length) return <Card><p className="text-ink-muted text-center">Henüz siparişiniz bulunmuyor.</p></Card>;

    return (
        <Card>
            <h3 className="font-semibold text-lg mb-4 text-ink">Son Siparişlerim</h3>
            <ul className="space-y-3">
                {orders.map((order) => (
                    <li key={order.id} className="flex justify-between items-center border-b border-border pb-3 last:border-0 last:pb-0">
                        <div>
                            <p className="font-medium text-ink">#{order.id}</p>
                            <p className="text-sm text-ink-muted">
                                {new Date(order.createdAt).toLocaleDateString('tr-TR')} •{' '}
                                <span className="font-medium">{order.status}</span>
                            </p>
                            <p className="text-sm text-ink-muted">
                                {order.totalAmount.toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                            </p>
                        </div>
                        {order.status !== 'CANCELLED' && order.status !== 'SHIPPED' && (
                            <Button
                                variant="danger"
                                onClick={() => { if (confirm('Bu siparişi iptal etmek istediğinize emin misiniz?')) cancelMutation.mutate(order.id); }}
                            >
                                İptal Et
                            </Button>
                        )}
                    </li>
                ))}
            </ul>
        </Card>
    );
}