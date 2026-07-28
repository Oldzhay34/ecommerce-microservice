import { create } from 'zustand';

export type Role = 'STORE' | 'CUSTOMER' | 'ADMIN' | string;

const STORAGE_KEY = 'shopbridge_session';

// Eski (customer) Widget remote'ları (mfe_orders, mfe_cart, mfe_payments, mfe_reviews)
// kendi vendored auth/readAuthContract.ts'lerinde bu anahtarla localStorage'dan token
// okur — shell'in prop geçirdiği yeni store remote'larından FARKLI bir sözleşmedir.
// İkisini de senkron tutmak için token her set/clear'da buraya da yazılır/silinir.
const LEGACY_TOKEN_KEY = 'shopbridge_access_token';

// sessionStorage'dan başlangıç değeri oku
function loadInitial() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEY);
        if (raw) {
            const parsed = JSON.parse(raw);
            // Bu fix'ten önce kurulmuş bir sekme sessionStorage'da session'a sahip
            // olabilir ama localStorage'daki eski anahtar hiç yazılmamış olabilir —
            // geriye dönük olarak da senkronla.
            if (parsed.authToken) {
                try { localStorage.setItem(LEGACY_TOKEN_KEY, parsed.authToken); } catch { /* yoksay */ }
            }
            return parsed;
        }
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
            localStorage.setItem(LEGACY_TOKEN_KEY, authToken);
        } catch { /* yoksay */ }
        set({ authToken, storeId, role });
    },
    clear: () => {
        try {
            sessionStorage.removeItem(STORAGE_KEY);
            localStorage.removeItem(LEGACY_TOKEN_KEY);
        } catch { /* yoksay */ }
        set({ authToken: null, storeId: null, role: null });
    },
}));