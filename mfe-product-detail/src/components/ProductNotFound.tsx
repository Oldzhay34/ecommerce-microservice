/**
 * Presenter · veri çekmez.
 * Pressman · User Help Facilities: ne olduğunu ve ne yapılacağını yazar.
 */
export interface ProductNotFoundProps {
    onBack: () => void;
}

export function ProductNotFound({ onBack }: ProductNotFoundProps) {
    return (
        <div style={{ textAlign: 'center', padding: '48px 24px' }}>
            <div style={{ fontSize: 16, fontWeight: 600, color: '#111827' }}>
                Ürün bulunamadı.
            </div>
            <div style={{ fontSize: 14, color: '#6B7280', marginTop: 8 }}>
                Aradığınız ürün kaldırılmış veya adresi değişmiş olabilir.
            </div>
            <div style={{ marginTop: 20 }}>
                <button
                    type="button"
                    onClick={onBack}
                    className="hover:bg-[#F9FAFB]"
                    style={{
                        height: 40,
                        borderRadius: 8,
                        fontWeight: 600,
                        fontSize: 14,
                        padding: '0 16px',
                        backgroundColor: '#FFFFFF',
                        border: '1px solid #D1D5DB',
                        color: '#111827',
                        cursor: 'pointer',
                    }}
                >
                    ← Ürünlere Dön
                </button>
            </div>
        </div>
    );
}