import type { Product } from '../types';

// Presenter: kategori → ad; alt satır fiyat (sol) / stok (sağ) + Stok Güncelle.
const priceFmt = new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
});

export function ProductRow({
                               product,
                               onUpdateStock,
                           }: {
    product: Product;
    onUpdateStock: (p: Product) => void;
}) {
    return (
        <div className="bg-white border border-line rounded-card px-6 py-5 flex items-start justify-between gap-4">
        <div className="min-w-0">
        <div className="text-faint text-[11px] uppercase tracking-wide">
            {product.category}
            </div>
            <div className="text-ink font-semibold text-base mt-0.5">
        {product.name}
        </div>
        <div className="mt-2 flex items-center gap-4">
    <span className="text-ink font-bold">
        {priceFmt.format(product.price)}
        </span>
        <span className="text-muted text-sm">
        Stokta {product.stock} adet
    </span>
    </div>
    </div>
    <button
    onClick={() => onUpdateStock(product)}
    className="shrink-0 h-9 rounded-btn font-semibold bg-white border border-line-strong text-ink px-3 text-sm"
        >
        Stok Güncelle
    </button>
    </div>
);
}