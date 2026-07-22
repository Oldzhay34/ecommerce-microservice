import { useState } from 'react';
import type { Product } from '../types';
import { ProductRow } from './ProductRow';
import { StockUpdateModal } from './StockUpdateModal';

function Skeleton() {
    return (
        <div className="flex flex-col gap-3">
            {[0, 1, 2].map((i) => (
                <div key={i} className="h-20 rounded-card sb-shimmer" />
            ))}
        </div>
    );
}

// Basit toast
function Toast({ message }: { message: string }) {
    return (
        <div className="fixed bottom-6 right-6 z-50 bg-ink text-white text-sm px-4 py-2 rounded-btn shadow-lg">
            {message}
        </div>
    );
}

export function ProductList({
                                products,
                                storeId,
                                isLoading,
                                isError,
                            }: {
    products: Product[];
    storeId: string;
    isLoading: boolean;
    isError: boolean;
}) {
    const [selected, setSelected] = useState<Product | null>(null);
    const [toast, setToast] = useState<string | null>(null);

    const showToast = (m: string) => {
        setToast(m);
        setTimeout(() => setToast(null), 2500);
    };

    if (isLoading) return <Skeleton />;
    if (isError)
        return (
            <div className="text-err-tx text-sm">
                Ürünler yüklenirken bir hata oluştu.
            </div>
        );
    if (products.length === 0)
        return <div className="text-muted text-sm">Henüz ürün eklemediniz.</div>;

    return (
        <div className="flex flex-col gap-3">
            {products.map((p) => (
                <ProductRow key={p.id} product={p} onUpdateStock={setSelected} />
            ))}
            {selected && (
                <StockUpdateModal
                    product={selected}
                    storeId={storeId}
                    onClose={() => setSelected(null)}
                    onSuccess={showToast}
                />
            )}
            {toast && <Toast message={toast} />}
        </div>
    );
}