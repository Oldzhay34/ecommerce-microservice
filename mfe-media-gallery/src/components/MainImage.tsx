import React, { useState } from 'react';
import { MediaAssetResponse } from '../types';
import { PlaceholderBox } from './PlaceholderBox';

interface MainImageProps {
    asset: MediaAssetResponse;
}

export const MainImage: React.FC<MainImageProps> = ({ asset }) => {
    const [hasError, setHasError] = useState(false);

    if (hasError) {
        return <PlaceholderBox />;
    }

    return (
        <div
            className="w-full aspect-square rounded-xl overflow-hidden border border-[#E5E7EB] bg-[#F3F4F6]"
            style={{ aspectRatio: '1 / 1' }}
        >
            <img
                src={asset.url}
                alt="Product"
                className="w-full h-full object-cover"
                onError={() => setHasError(true)}
            />
        </div>
    );
};