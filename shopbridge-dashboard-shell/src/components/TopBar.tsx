import { useSessionStore } from '../app/session/store';

/**
 * Üst bar: solda ShopBridge logosu, sağda mağaza kısaltması + "Çıkış Yap".
 */
export function TopBar({ onLogout }: { onLogout: () => void }) {
    const storeId = useSessionStore((s) => s.storeId);
    const shortId = storeId ? storeId.slice(0, 8) : '';

    return (
        <header
            style={{
                height: 64,
                background: '#FFFFFF',
                borderBottom: '1px solid #E5E7EB',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0 24px',
                position: 'sticky',
                top: 0,
                zIndex: 10,
            }}
        >
            <div style={{ fontWeight: 800, fontSize: 22, fontFamily: 'inherit' }}>
                <span style={{ color: '#000000' }}>Shop</span>
                <span style={{ color: '#1D4ED8' }}>Bridge</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <span style={{ color: '#6B7280', fontSize: 13 }}>Mağaza: {shortId}</span>
                <button
                    onClick={onLogout}
                    style={{
                        height: 40,
                        borderRadius: 8,
                        fontWeight: 600,
                        background: '#FFFFFF',
                        border: '1px solid #D1D5DB',
                        color: '#111827',
                        padding: '0 16px',
                        cursor: 'pointer',
                    }}
                >
                    Çıkış Yap
                </button>
            </div>
        </header>
    );
}