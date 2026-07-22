import { create } from 'zustand';

interface AuthState {
    isAuthenticated: boolean;
    token: string | null;
    registrationEmail: string | null; // Kayıt sonrası OTP'ye taşınacak mail (Reduce Memory Load)
    setToken: (token: string) => void;
    setRegistrationEmail: (email: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    isAuthenticated: false,
    token: null,
    registrationEmail: null,
    setToken: (token) => set({ token, isAuthenticated: true }),
    setRegistrationEmail: (email) => set({ registrationEmail: email }),
    logout: () => set({ token: null, isAuthenticated: false, registrationEmail: null }),
}));