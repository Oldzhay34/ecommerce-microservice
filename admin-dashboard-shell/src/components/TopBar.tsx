import React from 'react';
import { useSessionStore } from '../app/session/store';

export const TopBar: React.FC = () => {
    const clear = useSessionStore((state) => state.clear);

    const handleLogout = () => {
        clear();
        window.location.href = '/';
    };

    return (
        <header className="sticky top-0 z-40 h-[72px] w-full bg-canvas/90 backdrop-blur-xl shadow-[0_1px_0_rgba(15,15,15,0.06)] px-6 md:px-8 flex items-center justify-between">
            <div className="flex items-center gap-3">
        <span className="text-xl font-extrabold tracking-tight">
          <span className="text-ink-primary">Shop</span>
          <span className="text-brand">Bridge</span>
        </span>
                <span className="text-xs text-ink-secondary font-semibold uppercase tracking-wider bg-surface-hover px-2.5 py-1 rounded-full">
          Yönetim Paneli
        </span>
            </div>
            <div>
                <button
                    onClick={handleLogout}
                    className="h-10 px-5 rounded-full text-ink-primary text-sm font-medium hover:bg-surface-hover active:scale-[0.97] transition-all duration-150"
                >
                    Çıkış Yap
                </button>
            </div>
        </header>
    );
};