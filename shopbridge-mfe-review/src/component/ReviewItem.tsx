import type { ReviewResponse } from '../types';
import { StarRating } from './StarRating';
import { CustomerLabel } from './CustomerLabel';

const dateFmt = new Intl.DateTimeFormat('tr-TR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
});

function formatDate(iso: string): string {
    try {
        return dateFmt.format(new Date(iso));
    } catch {
        return '';
    }
}

export function ReviewItem({ review, index }: { review: ReviewResponse; index: number }) {
    return (
        <li className="border-b border-gray-100 pb-5 last:border-0 last:pb-0">
            <div className="flex justify-between items-start gap-4 mb-1.5">
                <div className="flex items-center gap-2 min-w-0">
                    <CustomerLabel index={index} />
                    <span className="text-gray-300">·</span>
                    <span className="text-xs text-gray-500 whitespace-nowrap">
                        {formatDate(review.createdAt)}
                    </span>
                </div>
                <StarRating rating={review.rating} />
            </div>

            {review.comment && (
                <p className="text-sm text-gray-700 leading-relaxed">{review.comment}</p>
            )}

            {/* Mağaza yanıtı — girintili ve ayrı arka planla ayrıştırılır */}
            {review.storeReplyText && (
                <div className="mt-3 ml-4 border-l-2 border-blue-200 pl-3 py-2 bg-blue-50/50 rounded-r">
                    <div className="flex items-center gap-2 mb-1">
                        <span className="text-xs font-semibold text-blue-700">Mağaza yanıtı</span>
                        {review.storeRepliedAt && (
                            <span className="text-xs text-gray-500">
                                {formatDate(review.storeRepliedAt)}
                            </span>
                        )}
                    </div>
                    <p className="text-sm text-gray-700 leading-relaxed">{review.storeReplyText}</p>
                </div>
            )}
        </li>
    );
}