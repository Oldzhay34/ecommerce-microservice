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
            <div style={{ fontSize: 16, fontWeight: 600, color: '#F2F2F5' }}>
                Ürün bulunamadı.
            </div>
            <div style={{ fontSize: 14, color: '#9C9CA8', marginTop: 8 }}>
                Aradığınız ürün kaldırılmış veya adresi değişmiş olabilir.
            </div>
            <div style={{ marginTop: 20 }}>
                <button
                    type="button"
                    onClick={onBack}
                    className="hover:bg-surface-hover"
                    style={{
                        height: 40,
                        borderRadius: 10,
                        fontWeight: 600,
                        fontSize: 14,
                        padding: '0 16px',
                        backgroundColor: '#131317',
                        border: '1px solid #232329',
                        color: '#F2F2F5',
                        cursor: 'pointer',
                    }}
                >
                    ← Ürünlere Dön
                </button>
            </div>
        </div>
    );
}