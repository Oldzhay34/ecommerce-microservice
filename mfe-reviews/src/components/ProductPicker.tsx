import type { Product } from '../types';

export function ProductPicker({
                                  products, value, onChange, isLoading,
                              }: {
    products: Product[];
    value: string;
    onChange: (id: string) => void;
    isLoading: boolean;
}) {
    return (
        <div className="mb-4">
            <label className="block text-sm font-semibold text-ink mb-1">Ürün Seçin</label>
            <select
                value={value}
                onChange={(e) => onChange(e.target.value)}
                disabled={isLoading}
                className="w-full max-w-md h-10 rounded-btn border border-line-strong px-3 text-ink bg-white outline-none focus:border-brand disabled:opacity-50"
            >
                <option value="">
                    Değerlendirmelerini görmek istediğiniz ürünü seçin.
                </option>
                {products.map((p) => (
                    <option key={p.id} value={p.id}>
                        {p.name}
                    </option>
                ))}
            </select>
        </div>
    );
}