import { useQuery } from '@tanstack/react-query';
import { fetchProductMedia } from '../api/endpoints';
import { MediaAssetResponse } from '../types';

export const useProductMedia = (productId: string) => {
    return useQuery<MediaAssetResponse[], Error>({
        queryKey: ['product-media', productId],
        queryFn: () => fetchProductMedia(productId),
        enabled: Boolean(productId),
        staleTime: 60_000,
        retry: 1,
    });
};