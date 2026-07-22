import { create } from 'zustand';

export type Role = 'STORE' | 'CUSTOMER' | 'ADMIN' | string;

const STORAGE_KEY = 'shopbridge_session';

// sessionStorage'dan başlangıç değeri oku
function loadInitial() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY);
        if (raw) return JSON.parse(raw);
    } catch { /* yoksay */ }
    return { authToken: null, storeId: null, role: null };
}

export interface SessionState {
    authToken: string | null;
    storeId: string | null;
    role: Role | null;
    setSession: (s: { authToken: string; storeId: string; role: Role }) => void;
    clear: () => void;
}

export const useSessionStore = create<SessionState>((set) => ({
    ...loadInitial(),
    setSession: ({ authToken, storeId, role }) => {
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ authToken, storeId, role }));
        } catch { /* yoksay */ }
        set({ authToken, storeId, role });
    },
    clear: () => {
        try { sessionStorage.removeItem(STORAGE_KEY); } catch { /* yoksay */ }
        set({ authToken: null, storeId: null, role: null });
    },
}));