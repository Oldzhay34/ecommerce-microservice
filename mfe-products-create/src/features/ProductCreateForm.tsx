import { lazy, Suspense, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { createProductSchema, type CreateProductFormValues } from '../schemas/createProductSchema';
import { useCreateProduct } from '../hooks/useCreateProduct';
import { ProductFormFields } from '../components/ProductFormFields';
import { ProductPreviewCard } from '../components/ProductPreviewCard';
import { FormErrorBanner } from '../components/FormErrorBanner';
import { DiscardConfirmDialog } from '../components/DiscardConfirmDialog';
import { Spinner } from '../components/Spinner';
import { Toast } from '../components/Toast';
import type { StoreSession } from '../types';
import { useProductImageUpload } from 'mfe_media_gallery/useProductImageUpload';
import type { PendingImage, UploadItemResult } from 'mfe_media_gallery/ProductImageUpload';

const ProductImageUpload = lazy(() => import('mfe_media_gallery/ProductImageUpload'));

const DASHBOARD_PATH = '/storedashboard';

export function ProductCreateForm({
                                      session,
                                      onNavigate,
                                  }: {
    session: StoreSession;
    onNavigate?: (path: string) => void;
}) {
    const [discardOpen, setDiscardOpen] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    // Görseller ürün oluşana kadar TARAYICIDA tutulur; productId olmadan upload edilemez.
    const [images, setImages] = useState<PendingImage[]>([]);
    const [uploadResults, setUploadResults] = useState<UploadItemResult[]>([]);
    const [isUploading, setIsUploading] = useState(false);

    const { uploadAll } = useProductImageUpload(session.authToken);

    const {
        register,
        handleSubmit,
        watch,
        setError,
        formState: { errors, isDirty },
    } = useForm<CreateProductFormValues>({
        resolver: zodResolver(createProductSchema),
        mode: 'onBlur',
        reValidateMode: 'onChange',
        defaultValues: { name: '', category: '', price: undefined, stock: undefined } as never,
    });

    const { mutate, isPending, error } = useCreateProduct(session.storeId);

    // Canlı önizleme — her tuş vuruşunda güncellenir.
    const name = watch('name') ?? '';
    const category = watch('category') ?? '';
    const price = watch('price');
    const stock = watch('stock');

    const busy = isPending || isUploading;

    const leave = () => onNavigate?.(DASHBOARD_PATH);

    const handleCancel = () => {
        if (isDirty || images.length > 0) setDiscardOpen(true);
        else leave();
    };

    const revokeAllPreviews = () => {
        images.forEach((img) => URL.revokeObjectURL(img.previewUrl));
    };

    /**
     * (b) akışı: önce ürün oluşturulur, dönen productId ile görseller SIRAYLA yüklenir.
     *
     * Bilinçli trade-off: ürün oluşup upload patlarsa ürün görselsiz kalır. Rollback YOKTUR —
     * Media servisi Product servisine bağlı değildir. Bu yüzden kullanıcıya kısmi başarı
     * açıkça bildirilir ve panele dönüş geciktirilir.
     */
    const onSubmit = (values: CreateProductFormValues) => {
        mutate(
            {
                name: values.name.trim(),
                category: values.category.trim().toUpperCase(),
                price: values.price,
                stock: values.stock,
            },
            {
                onSuccess: async (product) => {
                    if (images.length === 0) {
                        setToast('Ürün eklendi.');
                        setTimeout(leave, 300);
                        return;
                    }

                    setIsUploading(true);
                    setToast('Ürün eklendi. Görseller yükleniyor…');

                    const results = await uploadAll(product.id, images, setUploadResults);

                    setIsUploading(false);

                    const failed = results.filter((r) => r.status === 'error').length;
                    const succeeded = results.length - failed;

                    if (failed === 0) {
                        setToast('Ürün ve görseller eklendi.');
                        revokeAllPreviews();
                        setTimeout(leave, 600);
                    } else {
                        // Kısmi başarı: kullanıcı hangi dosyanın neden patladığını listede görür.
                        setToast(
                            `Ürün eklendi. ${succeeded} görsel yüklendi, ${failed} görsel yüklenemedi.`,
                        );
                    }
                },
                onError: (err) => {
                    const fields = err.fieldErrors;
                    if (fields) {
                        (Object.keys(fields) as Array<keyof CreateProductFormValues>).forEach((k) => {
                            if (k in values) setError(k, { type: 'server', message: fields[k as string] });
                        });
                    }
                },
            },
        );
    };

    const bannerMessage = error && !error.fieldErrors ? error.message : null;

    return (
        <div className="w-full max-w-[1120px] mx-auto">
            <button
                type="button"
                onClick={handleCancel}
                disabled={busy}
                className="text-ink-muted text-sm hover:text-ink disabled:opacity-50"
            >
                ← Panele Dön
            </button>

            <h1 className="text-ink text-2xl font-bold mt-4">Yeni Ürün Ekle</h1>
            <p className="text-ink-muted text-sm mt-1">
                Ürün bilgilerini girin; sağdaki önizleme müşterilerin göreceği kartı yansıtır.
            </p>

            <form onSubmit={handleSubmit(onSubmit)} className="mt-8 flex flex-wrap items-start gap-6">
                {/* Sol sütun — form kartı */}
                <div className="flex-[1_1_640px] min-w-0">
                    <div className="bg-surface border border-border shadow-sb rounded-card px-7 py-6">
                        <div className="text-ink-muted text-[13px] font-semibold uppercase tracking-[0.06em] mb-4">
                            Ürün Bilgileri
                        </div>

                        {bannerMessage && <FormErrorBanner message={bannerMessage} />}

                        <ProductFormFields
                            register={register}
                            errors={errors}
                            nameLength={name.length}
                            disabled={busy}
                        />
                    </div>

                    {/* Görseller — mfe-media-gallery'den federation ile gelir */}
                    <div className="bg-surface border border-border shadow-sb rounded-card px-7 py-6 mt-6">
                        <div className="text-ink-muted text-[13px] font-semibold uppercase tracking-[0.06em] mb-4">
                            Ürün Görselleri
                        </div>

                        <Suspense
                            fallback={
                                <div className="h-28 rounded-btn sb-shimmer" aria-label="Yükleniyor" />
                            }
                        >
                            <ProductImageUpload
                                authToken={session.authToken}
                                images={images}
                                onChange={setImages}
                                results={uploadResults}
                                disabled={busy}
                            />
                        </Suspense>
                    </div>

                    {/* Buton çubuğu */}
                    <div className="flex justify-end gap-3 mt-6">
                        <button
                            type="button"
                            onClick={handleCancel}
                            disabled={busy}
                            className="h-10 rounded-btn font-semibold bg-surface border border-border-strong text-ink px-4 hover:bg-surface-hover disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            Vazgeç
                        </button>
                        <button
                            type="submit"
                            disabled={busy}
                            className="h-10 rounded-full font-semibold bg-brand text-white px-4 hover:bg-brand-hover disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-2"
                        >
                            {busy && <Spinner />}
                            {isUploading ? 'Görseller yükleniyor…' : 'Ürünü Kaydet'}
                        </button>
                    </div>
                </div>

                {/* Sağ sütun — canlı önizleme (sticky) */}
                <div className="flex-[0_0_360px] max-lg:flex-[1_1_100%] lg:sticky lg:top-[88px]">
                    <ProductPreviewCard
                        name={name}
                        category={category}
                        price={price}
                        stock={stock}
                        coverPreviewUrl={images[0]?.previewUrl}
                    />
                </div>
            </form>

            <DiscardConfirmDialog
                open={discardOpen}
                onCancel={() => setDiscardOpen(false)}
                onConfirm={() => {
                    setDiscardOpen(false);
                    revokeAllPreviews();
                    leave();
                }}
            />

            {toast && <Toast message={toast} />}
        </div>
    );
}