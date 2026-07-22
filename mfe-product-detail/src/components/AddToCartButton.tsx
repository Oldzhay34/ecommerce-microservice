import { Spinner } from './Spinner';

/**
 * Presenter · veri çekmez.
 * Etiket her durumda "Sepete Ekle" — tek istisna stock <= 0 → "Tükendi"
 * (Pressman · Command/Menu Labeling: buton yapılamayacak bir eylem vaat etmez).
 */
export interface AddToCartButtonProps {
    stock: number;
    isPending: boolean;
    onClick: () => void;
}

export function AddToCartButton({ stock, isPending, onClick }: AddToCartButtonProps) {
    const isOutOfStock = stock <= 0;
    const disabled = isOutOfStock || isPending;
    const label = isOutOfStock ? 'Tükendi' : 'Sepete Ekle';

    return (
        <button
            type="button"
            disabled={disabled}
            onClick={onClick}
            className={disabled ? undefined : 'hover:bg-[#1E40AF]'}
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
                width: '100%',
                height: 48,
                borderRadius: 8,
                border: 'none',
                fontSize: 15,
                fontWeight: 600,
                color: '#FFFFFF',
                backgroundColor: isOutOfStock ? '#9CA3AF' : '#1D4ED8',
                opacity: disabled ? 0.5 : 1,
                cursor: disabled ? 'not-allowed' : 'pointer',
            }}
        >
            {isPending ? <Spinner /> : null}
            <span>{label}</span>
        </button>
    );
}