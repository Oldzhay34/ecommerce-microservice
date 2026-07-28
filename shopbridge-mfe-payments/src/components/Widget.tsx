import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { getUserIdFromToken } from '../auth/readAuthContract';
import { apiClient } from '../api/client';
import { Card, Button, ErrorBanner, Skeleton } from '../ui/components';

const refundSchema = z.object({
    reason: z.string().min(10, 'İade sebebi en az 10 karakter olmalıdır.')
});

type RefundFormValues = z.infer<typeof refundSchema>;

// Backend PaymentResponse ile birebir
interface PaymentResponse {
    id: string;          // UUID
    orderId: string;
    customerId: string;
    amount: number;
    status: string;
}

// PaymentStatus enum -> okunabilir etiket + renk
const STATUS_LABELS: Record<string, { text: string; className: string }> = {
    PENDING:          { text: 'Beklemede',         className: 'bg-warning/10 text-warning' },
    COMPLETED:        { text: 'Tamamlandı',        className: 'bg-success/10 text-success' },
    REFUND_REQUESTED: { text: 'İade Talep Edildi', className: 'bg-brand/10 text-brand' },
    FAILED:           { text: 'Başarısız',         className: 'bg-danger/10 text-danger' },
    REFUNDED:         { text: 'İade Edildi',       className: 'bg-surface-hover text-ink-muted' },
};

export default function PaymentsWidget() {
    const userId = getUserIdFromToken();
    const queryClient = useQueryClient();
    const [selectedPaymentId, setSelectedPaymentId] = useState<string | null>(null);

    const {data: payments, isLoading, isError} = useQuery({
        queryKey: ['payments', userId],
        queryFn: async () => {
            const res = await apiClient.get<PaymentResponse[]>('/api/payments/me');
            return res.data;
        },
        enabled: !!userId,
    });

    const {register, handleSubmit, formState: {errors}, reset} = useForm<RefundFormValues>({
        resolver: zodResolver(refundSchema)
    });

    const refundMutation = useMutation({
        mutationFn: async (data: RefundFormValues) => {
            await apiClient.post(`/api/payments/${selectedPaymentId}/refund-request`, data);
        },
        onSuccess: () => {
            alert('İade talebiniz başarıyla alındı.');
            setSelectedPaymentId(null);
            reset();
            queryClient.invalidateQueries({queryKey: ['payments', userId]});
        },
        onError: () => {
            alert('İade talebi gönderilemedi. Lütfen tekrar deneyin.');
        }
    });

    if (!userId) return <ErrorBanner message="Oturum bilgisi bulunamadı."/>;
    if (isLoading) return <Skeleton height="h-48"/>;
    if (isError) return <ErrorBanner message="Ödemeler yüklenemedi."/>;
    if (!payments?.length) return <Card><p className="text-ink-muted text-center">Henüz bir ödemeniz bulunmuyor.</p>
    </Card>;

    return (
        <Card>
            <h3 className="font-semibold text-lg mb-4 text-ink">Geçmiş Ödemelerim</h3>
            <ul className="space-y-3">
                {payments.map((payment) => {
                    const badge = STATUS_LABELS[payment.status] ?? {
                        text: payment.status,
                        className: 'bg-surface-hover text-ink-muted'
                    };
                    return (
                        <li key={payment.id}
                            className="flex justify-between items-center border-b border-border pb-3 last:border-0 last:pb-0">
                            <div>
                                <p className="font-medium text-ink">
                                    {payment.amount.toLocaleString('tr-TR', {style: 'currency', currency: 'TRY'})}
                                </p>
                                <div className="flex items-center gap-2 mt-1">
                                    <span className="text-sm text-ink-muted">#{payment.orderId.slice(0, 8)}</span>
                                    <span
                                        className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${badge.className}`}>
                                        {badge.text}
                                    </span>
                                </div>
                            </div>
                            {payment.status === 'COMPLETED' && (
                                <Button variant="secondary" onClick={() => setSelectedPaymentId(payment.id)}>
                                    İade İste
                                </Button>
                            )}
                        </li>
                    );
                })}
            </ul>

            {selectedPaymentId && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
                    <div className="bg-surface-raised p-6 rounded-sb-lg w-full max-w-sm shadow-sb-lg border border-border">
                        <h4 className="font-bold text-lg mb-4 text-ink">İade Talebi Oluştur</h4>
                        <form onSubmit={handleSubmit((data) => refundMutation.mutate(data))}>
                            <textarea
                                {...register('reason')}
                                className={`w-full bg-surface border p-3 rounded-sb mb-1 text-ink focus:ring-2 outline-none ${errors.reason ? 'border-danger focus:ring-danger/20' : 'border-border focus:ring-brand/20'}`}
                                placeholder="Lütfen iade sebebini detaylıca açıklayınız (min 10 karakter)..."
                                rows={4}
                            ></textarea>
                            {errors.reason && <p className="text-danger text-sm mb-4">{errors.reason.message}</p>}

                            <div className="flex justify-end gap-3 mt-4">
                                <Button variant="secondary" onClick={() => {
                                    setSelectedPaymentId(null);
                                    reset();
                                }}>Vazgeç</Button>
                                <Button type="submit" variant="primary" disabled={refundMutation.isPending}>
                                    {refundMutation.isPending ? 'Gönderiliyor...' : 'Talebi Gönder'}
                                </Button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </Card>
    );
}