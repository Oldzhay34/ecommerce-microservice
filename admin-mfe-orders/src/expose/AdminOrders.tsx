import React, { useState } from 'react';
import { AdminSession } from '../types';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useAdminOrders } from '../hooks/useAdminOrders';
import { useUpdateOrderStatus } from '../hooks/useUpdateOrderStatus';
import { TableSkeleton } from '../components/TableSkeleton';
import { ErrorBanner } from '../components/ErrorBanner';
import { OrderStatusBadge } from '../components/OrderStatusBadge';
import { PaginationBar } from '../components/PaginationBar';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Toast } from '../components/Toast';
import { formatTRY, formatDate, shortId } from '../utils/format';

const InnerAdminOrders: React.FC = () => {
    const [page, setPage] = useState(0);
    const { data, isLoading, error, isError } = useAdminOrders(page);
    const { mutateAsync: updateStatus, isPending: isMutating } = useUpdateOrderStatus();

    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [dialogConfig, setDialogConfig] = useState<{
        isOpen: boolean;
        orderId: string;
        action: 'SHIPPED' | 'CANCELLED';
    }>({ isOpen: false, orderId: '', action: 'SHIPPED' });

    const handleOpenDialog = (orderId: string, action: 'SHIPPED' | 'CANCELLED') => {
        setDialogConfig({ isOpen: true, orderId, action });
    };

    const handleCloseDialog = () => {
        setDialogConfig({ isOpen: false, orderId: '', action: 'SHIPPED' });
    };

    const handleConfirmAction = async () => {
        try {
            await updateStatus({ id: dialogConfig.orderId, status: dialogConfig.action });
            setToastMessage(
                dialogConfig.action === 'SHIPPED' ? 'Sipariş kargolandı.' : 'Sipariş iptal edildi.'
            );
        } catch (err: any) {
            alert(err.message || 'İşlem gerçekleştirilemedi.');
        } finally {
            handleCloseDialog();
        }
    };

    if (isLoading) return <TableSkeleton />;
    if (isError && error) return <ErrorBanner message={error.message} />;

    const orders = data?.content || [];

    return (
        <div className="space-y-4">
            <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                    <thead>
                    <tr className="border-b border-[#E5E7EB]">
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Sipariş No</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Müşteri</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Tarih</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Ürün</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Tutar</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Durum</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-right p-3 py-4">İşlemler</th>
                    </tr>
                    </thead>
                    <tbody>
                    {orders.length === 0 ? (
                        <tr>
                            <td colSpan={7} className="p-8 text-center text-sm text-[#6B7280]">
                                Henüz sipariş yok.
                            </td>
                        </tr>
                    ) : (
                        orders.map((order) => {
                            const canShip = order.status === 'PENDING' || order.status === 'APPROVED';
                            const canCancel = order.status !== 'CANCELLED';

                            return (
                                <tr key={order.id} className="border-b border-[#F3F4F6] hover:bg-gray-50 transition-colors">
                                    <td className="p-3 py-4 text-sm font-mono text-[#6B7280]" title={order.id}>
                                        {shortId(order.id)}
                                    </td>
                                    <td className="p-3 py-4 text-sm font-mono text-[#6B7280]" title={order.userId}>
                                        {shortId(order.userId)}
                                    </td>
                                    <td className="p-3 py-4 text-sm text-[#111827]">
                                        {formatDate(order.createdAt)}
                                    </td>
                                    <td className="p-3 py-4 text-sm text-[#111827]">
                                        {order.items.length} kalem
                                    </td>
                                    <td className="p-3 py-4 text-sm font-semibold text-[#111827]">
                                        {formatTRY(order.totalAmount)}
                                    </td>
                                    <td className="p-3 py-4 text-sm">
                                        <OrderStatusBadge status={order.status} />
                                    </td>
                                    <td className="p-3 py-4 text-sm text-right space-x-2 whitespace-nowrap">
                                        {canShip && (
                                            <button
                                                onClick={() => handleOpenDialog(order.id, 'SHIPPED')}
                                                className="h-8 px-3 rounded bg-[#1D4ED8] hover:bg-[#1E40AF] text-xs font-semibold text-white transition-colors"
                                            >
                                                Kargola
                                            </button>
                                        )}
                                        {canCancel && (
                                            <button
                                                onClick={() => handleOpenDialog(order.id, 'CANCELLED')}
                                                className="h-8 px-3 rounded bg-[#B91C1C] hover:bg-[#991B1B] text-xs font-semibold text-white transition-colors"
                                            >
                                                İptal Et
                                            </button>
                                        )}
                                        {!canShip && !canCancel && <span className="text-[#9CA3AF]">—</span>}
                                    </td>
                                </tr>
                            );
                        })
                    )}
                    </tbody>
                </table>
            </div>

            {data && (
                <PaginationBar
                    currentPage={page}
                    totalPages={data.totalPages}
                    totalElements={data.totalElements}
                    onPageChange={setPage}
                    isFirst={data.first}
                    isLast={data.last}
                />
            )}

            <ConfirmDialog
                isOpen={dialogConfig.isOpen}
                title={dialogConfig.action === 'SHIPPED' ? 'Sipariş kargolansın mı?' : 'Sipariş iptal edilsin mi?'}
                description={
                    dialogConfig.action === 'SHIPPED'
                        ? `${shortId(dialogConfig.orderId)} numaralı sipariş "Kargolandı" olarak işaretlenecek.`
                        : `${shortId(dialogConfig.orderId)} numaralı sipariş iptal edilecek. Bu işlem geri alınamaz.`
                }
                onConfirm={handleConfirmAction}
                onCancel={handleCloseDialog}
                confirmLabel={dialogConfig.action === 'SHIPPED' ? 'Kargola' : 'İptal Et'}
                isDanger={dialogConfig.action === 'CANCELLED'}
                isLoading={isMutating}
            />

            {toastMessage && <Toast message={toastMessage} onClose={() => setToastMessage(null)} />}
        </div>
    );
};

const AdminOrders: React.FC<{ session: AdminSession }> = ({ session }) => {
    return (
        <AuthTokenProvider token={session.authToken}>
            <InnerAdminOrders />
        </AuthTokenProvider>
    );
};

export default AdminOrders;