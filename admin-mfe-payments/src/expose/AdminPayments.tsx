import React, { useState } from 'react';
import { AdminSession } from '../types';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useAdminPayments } from '../hooks/useAdminPayments';
import { useApproveRefund } from '../hooks/useApproveRefund';
import { TableSkeleton } from '../components/TableSkeleton';
import { ErrorBanner } from '../components/ErrorBanner';
import { PaymentStatusBadge } from '../components/PaymentStatusBadge';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Toast } from '../components/Toast';
import { formatTRY, shortId } from '../utils/format';

const InnerAdminPayments: React.FC = () => {
    const { data: payments = [], isLoading, error, isError } = useAdminPayments();
    const { mutateAsync: approveRefund, isPending: isMutating } = useApproveRefund();

    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [dialogConfig, setDialogConfig] = useState<{
        isOpen: boolean;
        paymentId: string;
    }>({ isOpen: false, paymentId: '' });

    const handleOpenDialog = (paymentId: string) => {
        setDialogConfig({ isOpen: true, paymentId });
    };

    const handleCloseDialog = () => {
        setDialogConfig({ isOpen: false, paymentId: '' });
    };

    const handleConfirmAction = async () => {
        try {
            await approveRefund(dialogConfig.paymentId);
            setToastMessage('İade onaylandı.');
        } catch (err: any) {
            alert(err.message || 'İade onayı başarısız.');
        } finally {
            handleCloseDialog();
        }
    };

    if (isLoading) return <TableSkeleton />;
    if (isError && error) return <ErrorBanner message={error.message} />;

    return (
        <div className="space-y-4">
            <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                    <thead>
                    <tr className="border-b border-[#E5E7EB]">
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Ödeme No</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Sipariş No</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Müşteri</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Tutar</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Durum</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-right p-3 py-4">İşlemler</th>
                    </tr>
                    </thead>
                    <tbody>
                    {payments.length === 0 ? (
                        <tr>
                            <td colSpan={6} className="p-8 text-center text-sm text-[#6B7280]">
                                Henüz ödeme kaydı yok.
                            </td>
                        </tr>
                    ) : (
                        payments.map((payment) => {
                            const isRefundable = payment.status === 'REFUND_REQUESTED';

                            return (
                                <tr key={payment.id} className="border-b border-[#F3F4F6] hover:bg-gray-50 transition-colors">
                                    <td className="p-3 py-4 text-sm font-mono text-[#6B7280]" title={payment.id}>
                                        {shortId(payment.id)}
                                    </td>
                                    <td className="p-3 py-4 text-sm font-mono text-[#6B7280]" title={payment.orderId}>
                                        {shortId(payment.orderId)}
                                    </td>
                                    <td className="p-3 py-4 text-sm font-mono text-[#6B7280]" title={payment.customerId}>
                                        {shortId(payment.customerId)}
                                    </td>
                                    <td className="p-3 py-4 text-sm font-semibold text-[#111827]">
                                        {formatTRY(payment.amount)}
                                    </td>
                                    <td className="p-3 py-4 text-sm">
                                        <PaymentStatusBadge status={payment.status} />
                                    </td>
                                    <td className="p-3 py-4 text-sm text-right">
                                        {isRefundable ? (
                                            <button
                                                onClick={() => handleOpenDialog(payment.id)}
                                                className="h-8 px-3 rounded bg-[#1D4ED8] hover:bg-[#1E40AF] text-xs font-semibold text-white transition-colors"
                                            >
                                                İadeyi Onayla
                                            </button>
                                        ) : (
                                            <span className="text-[#9CA3AF]">—</span>
                                        )}
                                    </td>
                                </tr>
                            );
                        })
                    )}
                    </tbody>
                </table>
            </div>

            <ConfirmDialog
                isOpen={dialogConfig.isOpen}
                title="İade onaylansın mı?"
                description={`${shortId(dialogConfig.paymentId)} numaralı ödemenin iadesi onaylanacak. Bu işlem geri alınamaz.`}
                onConfirm={handleConfirmAction}
                onCancel={handleCloseDialog}
                confirmLabel="Onayla"
                isLoading={isMutating}
            />

            {toastMessage && <Toast message={toastMessage} onClose={() => setToastMessage(null)} />}
        </div>
    );
};

const AdminPayments: React.FC<{ session: AdminSession }> = ({ session }) => {
    return (
        <AuthTokenProvider token={session.authToken}>
            <InnerAdminPayments />
        </AuthTokenProvider>
    );
};

export default AdminPayments;