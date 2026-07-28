import { Logo } from './Logo';

interface TopBarProps {
    /** Sağda gösterilecek kısa kimlik metni (örn. mağaza kısaltması veya kullanıcı e-postası). */
    identity?: string;
    onLogout: () => void;
}

/**
 * Üst bar: solda ShopBridge logosu, sağda kimlik bilgisi + "Çıkış Yap".
 * Hem customer hem store dashboard'unda kullanılan paylaşılan chrome.
 */
export function TopBar({ identity, onLogout }: TopBarProps) {
    return (
        <header className="sticky top-0 z-10 h-[72px] bg-canvas/90 backdrop-blur-xl shadow-[0_1px_0_rgba(15,15,15,0.06)]">
            <div className="max-w-6xl mx-auto h-full px-6 md:px-8 flex items-center justify-between">
                <Logo />
                <div className="flex items-center gap-5">
                    {identity ? (
                        <span className="text-ink-muted text-sm hidden sm:inline">{identity}</span>
                    ) : null}
                    <button
                        onClick={onLogout}
                        className="h-10 px-5 rounded-full text-ink text-sm font-medium hover:bg-surface-hover active:scale-[0.97] transition-all duration-150"
                    >
                        Çıkış Yap
                    </button>
                </div>
            </div>
        </header>
    );
}
