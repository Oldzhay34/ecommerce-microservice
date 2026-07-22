import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createApiClient } from '../api/client';
import { makeReviewApi } from '../api/reviewApi';
import { useAuthToken } from '../auth/useAuthToken';

export function useReplyReview(productId: string | null) {
    const token = useAuthToken();
    const qc = useQueryClient();
    const api = useMemo(() => makeReviewApi(createApiClient(() => token)), [token]);

    return useMutation({
        mutationFn: ({ id, reply }: { id: string; reply: string }) =>
            // Backend StoreReplyRequest DTO'su 'replyText' alanını bekliyor.
            api.reply(id, { replyText: reply }),
        onSuccess: () => {
            // Yalnız o ürünün review query'si tazelenir
            qc.invalidateQueries({ queryKey: ['product-reviews', productId] });
        },
    });
}