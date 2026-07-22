import type { ReactNode } from 'react';
import { CategoryBadge } from './CategoryBadge';
import { StockBadge } from './StockBadge';
import { QuantityStepper } from './QuantityStepper';
import { AddToCartButton } from './AddToCartButton';
import { formatTRY, shortId } from '../utils/format';
import type { Product } from '../types';

/**
 * Presenter · sağ kolon. Veri çekmez, yalnızca prop alır.
 * Sıra SABİTTİR: kategori → ad → fiyat → stok → ayırıcı → adet → buton → hata → ÜRÜN NO.
 */
export interface ProductInfoPanelProps {
    product: Product;
    quantity: number;
    onQuantityChange: (next: number) => void;
    onAddToCart: () => void;
    isPending: boolean;
    errorSlot?: ReactNode;
}

export function ProductInfoPanel({
                                     product,
                                     quantity,
                                     onQuantityChange,
                                     onAddToCart,
                                     isPending,
                                     errorSlot,
                                 }: ProductInfoPanelProps) {
    return (
        <div>
            {/* 1 — Kategori */}
            <CategoryBadge category={product.category} />
            <div style={{ height: 12 }} />

            {/* 2 — Ürün adı */}
            <h1
                style={{
                    fontSize: 28,
                    fontWeight: 700,
                    color: '#111827',
                    lineHeight: 1.25,
                    wordBreak: 'break-word',
                    margin: 0,
                }}
            >
                {product.name}
            </h1>
            <div style={{ height: 16 }} />

            {/* 3 — Fiyat */}
            <div style={{ fontSize: 32, fontWeight: 700, color: '#111827' }}>
                {formatTRY(product.price)}
            </div>
            <div style={{ height: 16 }} />

            {/* 4 — Stok */}
            <StockBadge stock={product.stock} />
            <div style={{ height: 24 }} />

            {/* 5 — Ayırıcı */}
            <div style={{ height: 1, backgroundColor: '#E5E7EB', width: '100%' }} />
            <div style={{ height: 24 }} />

            {/* 6 — Adet seçici */}
            <QuantityStepper
                quantity={quantity}
                stock={product.stock}
                onChange={onQuantityChange}
                disabled={isPending}
            />
            <div style={{ height: 20 }} />

            {/* 7 — Sepete Ekle */}
            <AddToCartButton
                stock={product.stock}
                isPending={isPending}
                onClick={onAddToCart}
            />

            {/* 8 — Hata alanı */}
            {errorSlot ? <div style={{ marginTop: 16 }}>{errorSlot}</div> : null}

            {/* 9 — Ürün kimliği */}
            <div style={{ marginTop: 24, display: 'flex', alignItems: 'center', gap: 8 }}>
        <span
            style={{
                textTransform: 'uppercase',
                fontSize: 11,
                letterSpacing: '0.06em',
                fontWeight: 600,
                color: '#9CA3AF',
            }}
        >
          Ürün No
        </span>
                <span
                    title={product.id}
                    style={{
                        fontFamily: 'ui-monospace, monospace',
                        fontSize: 12,
                        color: '#6B7280',
                    }}
                >
          {shortId(product.id)}
        </span>
            </div>
        </div>
    );
}