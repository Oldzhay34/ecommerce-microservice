import React from 'react';

export const SkeletonGallery: React.FC = () => {
    return (
        <div className="w-full">
            <div
                className="w-full aspect-square rounded-xl sb-shimmer"
                style={{ aspectRatio: '1 / 1' }}
            />
        </div>
    );
};