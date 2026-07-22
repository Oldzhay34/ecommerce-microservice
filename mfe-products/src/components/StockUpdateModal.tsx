import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { stockSchema, type StockFormValues } from '../schemas/stockSchema';
import { useUpdateStock } from '../hooks/useUpdateStock';
import type { Product } from '../types';

export function StockUpdateModal({
                                     product,
                                     storeId,
                                     onClose,
                                     onSuccess,
                                 }: {
    product: Product;
    storeId: string;
    onClose: () => void;
    onSuccess: (message: string) => void;
}) {
    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<StockFormValues>({
        resolver: zodResolver(stockSchema),
        defaultValues: { stock: product.stock },
    });

    const { mutate, isPending, error } = useUpdateStock(storeId);

    const onSubmit = (values: StockFormValues) => {
        mutate(
            { id: product.id, newStock: values.stock }, // DÜZELTİLDİ: stock → newStock
            {
                onSuccess: () => {
                    onSuccess('Stok güncellendi.');
                    onClose();
                },
            },
        );
    };

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
            role="dialog"
            aria-modal="true"
        >
            <div className="bg-white border border-line rounded-card w-full max-w-md p-6">
                <h3 className="text-ink text-lg font-bold">
                    Stok Güncelle — {product.name}
                </h3>
                <p className="text-muted text-sm mt-1">Mevcut stok: {product.stock}</p>

                <form onSubmit={handleSubmit(onSubmit)} className="mt-4">
                    <label className="block text-sm font-semibold text-ink mb-1">
                        Yeni Stok Adedi
                    </label>
                    <input
                        type="number"
                        min={0}
                        {...register('stock', { valueAsNumber: true })}
                        className="w-full h-10 rounded-btn border border-line-strong px-3 text-ink outline-none focus:border-brand"
                    />
                    <p className="text-muted text-xs mt-1">
                        0 veya daha büyük bir tam sayı girin.
                    </p>
                    {errors.stock && (
                        <p className="text-err-tx text-xs mt-1">{errors.stock.message}</p>
                    )}
                    {error && (
                        <p className="text-err-tx text-xs mt-2">{error.message}</p>
                    )}

                    <div className="flex justify-end gap-3 mt-6">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={isPending}
                            className="h-10 rounded-btn font-semibold bg-white border border-line-strong text-ink px-4 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            Vazgeç
                        </button>
                        <button
                            type="submit"
                            disabled={isPending}
                            className="h-10 rounded-btn font-semibold bg-brand text-white px-4 hover:bg-brand-dark disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-2"
                        >
                            {isPending && (
                                <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                            )}
                            Kaydet
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}