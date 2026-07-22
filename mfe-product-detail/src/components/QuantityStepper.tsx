import { maxQuantityFor, MIN_QUANTITY } from '../utils/quantity';

/**
 * Presenter · veri çekmez.
 * Değer alanı salt okunur <span>'dir — <input> YOKTUR, geçersiz değer üretilemez.
 * Bu nedenle Zod / React Hook Form kullanılmaz (bkz. VALİDASYON).
 */
export interface QuantityStepperProps {
    quantity: number;
    stock: number;
    onChange: (next: number) => void;
    disabled?: boolean;
}

export function QuantityStepper({
                                    quantity,
                                    stock,
                                    onChange,
                                    disabled = false,
                                }: QuantityStepperProps) {
    const max = maxQuantityFor(stock);
    const isOutOfStock = stock <= 0;
    const groupDisabled = disabled || isOutOfStock;

    const decrementDisabled = groupDisabled || quantity <= MIN_QUANTITY;
    const incrementDisabled = groupDisabled || quantity >= max;

    const buttonBaseStyle: React.CSSProperties = {
        width: 40,
        height: '100%',
        backgroundColor: '#FFFFFF',
        color: '#111827',
        fontSize: 18,
        fontWeight: 600,
        border: 'none',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 0,
    };

    return (
        <div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#111827' }}>Adet</div>
            <div style={{ height: 8 }} />
            <div
                role="group"
                aria-label="Adet seçici"
                style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    border: '1px solid #D1D5DB',
                    borderRadius: 8,
                    overflow: 'hidden',
                    height: 40,
                }}
            >
                <button
                    type="button"
                    aria-label="Adet azalt"
                    disabled={decrementDisabled}
                    onClick={() => onChange(quantity - 1)}
                    className={decrementDisabled ? undefined : 'hover:bg-[#F9FAFB]'}
                    style={{
                        ...buttonBaseStyle,
                        borderRight: '1px solid #D1D5DB',
                        opacity: decrementDisabled ? 0.5 : 1,
                        cursor: decrementDisabled ? 'not-allowed' : 'pointer',
                    }}
                >
                    −
                </button>

                <span
                    aria-live="polite"
                    style={{
                        width: 56,
                        textAlign: 'center',
                        fontSize: 14,
                        fontWeight: 600,
                        color: '#111827',
                        userSelect: 'none',
                        opacity: groupDisabled ? 0.5 : 1,
                    }}
                >
          {isOutOfStock ? MIN_QUANTITY : quantity}
        </span>

                <button
                    type="button"
                    aria-label="Adet artır"
                    disabled={incrementDisabled}
                    onClick={() => onChange(quantity + 1)}
                    className={incrementDisabled ? undefined : 'hover:bg-[#F9FAFB]'}
                    style={{
                        ...buttonBaseStyle,
                        borderLeft: '1px solid #D1D5DB',
                        opacity: incrementDisabled ? 0.5 : 1,
                        cursor: incrementDisabled ? 'not-allowed' : 'pointer',
                    }}
                >
                    +
                </button>
            </div>
        </div>
    );
}