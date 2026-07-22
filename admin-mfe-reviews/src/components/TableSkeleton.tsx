import React from 'react';

export const TableSkeleton: React.FC = () => {
    return (
        <div className="space-y-3">
        <div className="h-14 w-full sb-shimmer rounded-lg" />
        <div className="h-14 w-full sb-shimmer rounded-lg" />
        <div className="h-14 w-full sb-shimmer rounded-lg" />
            </div>
    );
};