import React from 'react';

export const SectionSkeleton: React.FC = () => {
    return (
        <div className="space-y-4">
            <div className="h-14 w-full sb-shimmer rounded-lg" />
            <div className="h-14 w-full sb-shimmer rounded-lg" />
            <div className="h-14 w-full sb-shimmer rounded-lg" />
        </div>
    );
};