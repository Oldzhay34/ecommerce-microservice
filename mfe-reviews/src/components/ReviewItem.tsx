import type { ReviewResponse } from '../types';
import { StarRating } from './StarRating';
import { ReplyForm } from './ReplyForm';

const dateFmt = new Intl.DateTimeFormat('tr-TR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

export function ReviewItem({
                               review, productId, onToast,
                           }: {
    review: ReviewResponse;
    productId: string;
    onToast: (m: string) => void;
}) {
    return (
        <div className="bg-white border border-line rounded-card px-6 py-5">
            <div className="flex items-center justify-between">
                <StarRating rating={review.rating} />
                <span className="text-faint text-[11px] uppercase tracking-wide">
          {review.customerId.slice(0, 8)}
        </span>
            </div>

            <p className="text-ink text-sm mt-2">{review.comment}</p>

            {review.createdAt && (
                <div className="text-muted text-xs mt-1">
                    {dateFmt.format(new Date(review.createdAt))}
                </div>
            )}

            {review.reply ? (
                <div className="mt-3 bg-neutral-bg rounded-btn px-3 py-2 text-sm text-ink">
                    <span className="font-semibold">Mağaza Yanıtı: </span>
                    {review.reply}
                </div>
            ) : (
                <ReplyForm reviewId={review.id} productId={productId} onSuccess={onToast} />
            )}
        </div>
    );
}