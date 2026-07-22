import { importShared } from './__federation_fn_import-sHsD3zYY.js';
import { m as makeMediaApi, c as createApiClient, A as ApiError } from './endpoints-Bp5h6O00.js';

const {useMemo} = await importShared('react');

const {useQueryClient} = await importShared('@tanstack/react-query');
function useProductImageUpload(authToken) {
  const queryClient = useQueryClient();
  const api = useMemo(() => makeMediaApi(createApiClient(() => authToken)), [authToken]);
  const uploadAll = async (productId, images, onProgress) => {
    const results = images.map((img) => ({
      id: img.id,
      fileName: img.file.name,
      status: "pending"
    }));
    onProgress?.([...results]);
    for (let i = 0; i < images.length; i++) {
      results[i] = { ...results[i], status: "uploading" };
      onProgress?.([...results]);
      try {
        const asset = await api.uploadProductImage(productId, images[i].file);
        results[i] = { ...results[i], status: "success", asset };
      } catch (err) {
        results[i] = {
          ...results[i],
          status: "error",
          message: err instanceof ApiError ? err.message : "Görsel yüklenemedi."
        };
      }
      onProgress?.([...results]);
    }
    queryClient.invalidateQueries({ queryKey: ["product-media", productId] });
    return results;
  };
  return { uploadAll };
}

export { useProductImageUpload };
