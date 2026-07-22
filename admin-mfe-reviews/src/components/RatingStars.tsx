import React from 'react';

interface RatingStarsProps {
    rating: number;
}

export const RatingStars: React.FC<RatingStarsProps> = ({ rating }) => {
    const fullStars = Math.max(0, Math.min(5, Math.floor(rating)));
    const emptyStars = Math.max(0, 5 - fullStars);

    return (
        <div className="flex items-center space-x-0.5">
            <div className="flex items-center text-sm">
                {Array.from({ length: fullStars }).map((_, i) => (
                    <span key={`full-${i}`} className="text-[#F59E0B]">★</span>
                ))}
                {Array.from({ length: emptyStars }).map((_, i) => (
                    <span key={`empty-${i}`} className="text-[#D1D5DB]">☆</span>
                ))}
            </div>
            <span className="text-xs text-[#6B7280] font-semibold ml-1">({rating})</span>
        </div>
    );
};