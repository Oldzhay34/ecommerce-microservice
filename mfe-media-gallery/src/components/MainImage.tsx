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
            className="w-full aspect-square rounded-sb-lg overflow-hidden border border-border bg-surface"
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