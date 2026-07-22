import React, { useState } from 'react';
import { AdminSession } from '../types';
import { AuthTokenProvider } from '../auth/AuthTokenProvider';
import { useAdminReviews } from '../hooks/useAdminReviews';
import { useModerateReview } from '../hooks/useModerateReview';
import { TableSkeleton } from '../components/TableSkeleton';
import { ErrorBanner } from '../components/ErrorBanner';
import { ReviewStatusBadge } from '../components/ReviewStatusBadge';
import { RatingStars } from '../components/RatingStars';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Toast } from '../components/Toast';
import { formatDate, shortId } from '../utils/format';

const InnerAdminReviews: React.FC = () => {
    const { data: reviews = [], isLoading, error, isError } = useAdminReviews();
    const { mutateAsync: moderateReview, isPending: isMutating } = useModerateReview();

    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [dialogConfig, setDialogConfig] = useState<{
        isOpen: boolean;
        reviewId: string;
        action: 'ACTIVE' | 'HIDDEN';
    }>({ isOpen: false, reviewId: '', action: 'HIDDEN' });

    const handleOpenDialog = (reviewId: string, action: 'ACTIVE' | 'HIDDEN') => {
        setDialogConfig({ isOpen: true, reviewId, action });
    };

    const handleCloseDialog = () => {
        setDialogConfig({ isOpen: false, reviewId: '', action: 'HIDDEN' });
    };

    const handleConfirmAction = async () => {
        try {
            await moderateReview({ id: dialogConfig.reviewId, status: dialogConfig.action });
            setToastMessage(
                dialogConfig.action === 'HIDDEN' ? 'Yorum gizlendi.' : 'Yorum yayınlandı.'
            );
        } catch (err: any) {
            alert(err.message || 'Yorum moderasyonu başarısız.');
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
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Ürün</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Müşteri</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Puan</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Yorum</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Mağaza Yanıtı</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Tarih</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-left p-3 py-4">Durum</th>
                        <th className="text-[11px] font-semibold text-[#6B7280] uppercase tracking-wider text-right p-3 py-4">İşlemler</th>
                    </tr>
                    </thead>
                    <tbody>
                    {reviews.length === 0 ? (
                        <tr>
                            <td colSpan={8} className="p-8 text-center text-sm text-[#6B7280]">
                                Henüz yorum yok.
                            </td>
                        </tr>
                    ) : (
                        reviews.map((review) => {
                            const isActive = review.status === 'ACTIVE';

                            return (
                                <tr key={review.id} className="border-b border-[#F3F4F6] hover:bg-gray-50 transition-colors">
                                    <td className="p-3 py-4 text-sm font-mono text-[#6B7280]" title={review.productId}>
                                        {shortId(review.productId)}
                                    </td>
                                    <td className="p-3 py-4 text-sm font-mono text-[#6B7280]" title={review.customerId}>
                                        {shortId(review.customerId)}
                                    </td>
                                    <td className="p-3 py-4 text-sm">
                                        <RatingStars rating={review.rating} />
                                    </td>
                                    <td className="p-3 py-4 text-sm text-[#111827]" title={review.comment}>
                                        <div className="max-w-[320px] line-clamp-2 overflow-hidden text-ellipsis">
                                            {review.comment ? review.comment : <span className="text-[#9CA3AF]">—</span>}
                                        </div>
                                    </td>
                                    <td className="p-3 py-4 text-sm">
                                        {review.storeReplyText ? (
                                            <span className="text-xs font-semibold text-[#065F46] bg-[#D1FAE5] px-2 py-0.5 rounded">
                          Yanıtlandı
                        </span>
                                        ) : (
                                            <span className="text-[#9CA3AF]">—</span>
                                        )}
                                    </td>
                                    <td className="p-3 py-4 text-sm text-[#111827]">
                                        {formatDate(review.createdAt)}
                                    </td>
                                    <td className="p-3 py-4 text-sm">
                                        <ReviewStatusBadge status={review.status} />
                                    </td>
                                    <td className="p-3 py-4 text-sm text-right whitespace-nowrap">
                                        {isActive ? (
                                            <button
                                                onClick={() => handleOpenDialog(review.id, 'HIDDEN')}
                                                className="h-8 px-3 rounded bg-[#B91C1C] hover:bg-[#991B1B] text-xs font-semibold text-white transition-colors"
                                            >
                                                Gizle
                                            </button>
                                        ) : (
                                            <button
                                                onClick={() => handleOpenDialog(review.id, 'ACTIVE')}
                                                className="h-8 px-3 rounded bg-[#1D4ED8] hover:bg-[#1E40AF] text-xs font-semibold text-white transition-colors"
                                            >
                                                Yayınla
                                            </button>
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
                title={dialogConfig.action === 'HIDDEN' ? 'Yorum gizlensın mi?' : 'Yorum yayınlansın mı?'}
                description={
                    dialogConfig.action === 'HIDDEN'
                        ? 'Yorum müşteri listelerinde görünmeyecek.'
                        : 'Yorum müşteri listelerinde tekrar görünecek.'
                }
                onConfirm={handleConfirmAction}
                onCancel={handleCloseDialog}
                confirmLabel={dialogConfig.action === 'HIDDEN' ? 'Gizle' : 'Yayınla'}
                isDanger={dialogConfig.action === 'HIDDEN'}
                isLoading={isMutating}
            />

            {toastMessage && <Toast message={toastMessage} onClose={() => setToastMessage(null)} />}
        </div>
    );
};

const AdminReviews: React.FC<{ session: AdminSession }> = ({ session }) => {
    return (
        <AuthTokenProvider token={session.authToken}>
            <InnerAdminReviews />
        </AuthTokenProvider>
    );
};

export default AdminReviews;