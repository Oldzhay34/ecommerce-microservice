import React from 'react';
import { useSessionStore } from '../app/session/store';

export const TopBar: React.FC = () => {
    const clear = useSessionStore((state) => state.clear);

    const handleLogout = () => {
        clear();
        window.location.href = '/';
    };

    return (
        <header className="sticky top-0 z-40 h-16 w-full bg-white border-b border-[#E5E7EB] px-6 flex items-center justify-between">
            <div className="flex items-center space-x-3">
        <span className="text-xl font-bold tracking-tight">
          <span style={{ color: '#000000' }}>Shop</span>
          <span style={{ color: '#1D4ED8' }}>Bridge</span>
        </span>
                <span className="text-xs text-[#6B7280] font-semibold uppercase tracking-wider bg-gray-100 px-2.5 py-1 rounded">
          Yönetim Paneli
        </span>
            </div>
            <div>
                <button
                    onClick={handleLogout}
                    className="h-10 px-4 rounded-lg bg-white border border-[#D1D5DB] text-[#111827] text-sm font-semibold hover:bg-gray-50 transition-colors"
                >
                    Çıkış Yap
                </button>
            </div>
        </header>
    );
};