import { StarRating } from './StarRating';

export function RatingSummary({
                                  averageRating,
                                  totalCount,
                              }: {
    averageRating: number;
    totalCount: number;
}) {
    // Backend double döndürür; 0 ise hiç puan yok demektir.
    const rounded = Math.round(averageRating * 10) / 10;

    return (
        <div className="flex items-center gap-3">
            <span className="text-2xl font-bold text-gray-900">{rounded.toFixed(1)}</span>
            <div>
                <StarRating rating={Math.round(averageRating)} size="md" />
                <p className="text-xs text-gray-500 mt-0.5">
                    {totalCount} değerlendirme
                </p>
            </div>
        </div>
    );
}