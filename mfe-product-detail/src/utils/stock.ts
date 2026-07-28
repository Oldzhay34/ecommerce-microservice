export interface StockView {
    label: string;
    bg: string;
    text: string;
    note: string;
    isOutOfStock: boolean;
}

export function getStockView(stock: number): StockView {
    const safeStock = Number.isFinite(stock) ? Math.trunc(stock) : 0;

    if (safeStock <= 0) {
        return {
            label: 'Tükendi',
            bg: '#FEF2F2',
            text: '#DC2626',
            note: 'Bu ürün şu anda stokta yok.',
            isOutOfStock: true,
        };
    }

    if (safeStock <= 5) {
        return {
            label: `Son ${safeStock} adet`,
            bg: '#FFFBEB',
            text: '#D97706',
            note: 'Stok azalıyor.',
            isOutOfStock: false,
        };
    }

    return {
        label: 'Stokta',
        bg: '#ECFDF5',
        text: '#16A34A',
        note: `${safeStock} adet mevcut.`,
        isOutOfStock: false,
    };
}