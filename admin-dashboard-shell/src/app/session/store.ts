import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload } from '../../types';

export interface SessionState {
    authToken: string | null;
    userId: string | null;
    role: string | null;
    setFromToken: (token: string) => void;
    clear: () => void;
}

export const SESSION_STORAGE_KEY = 'shopbridge_admin_session';

export const useSessionStore = create<SessionState>()(
    persist(
        (set) => ({
            authToken: null,
            userId: null,
            role: null,
            setFromToken: (token: string) => {
                const { sub, role } = jwtDecode<JwtPayload>(token);
                set({
                    authToken: token,
                    userId: sub,
                    role: role.replace(/^ROLE_/, ''),
                });
            },
            clear: () => {
                set({ authToken: null, userId: null, role: null });
            },
        }),
        {
            name: SESSION_STORAGE_KEY,
            storage: createJSONStorage(() => sessionStorage),
            // 'state' parametresine SessionState tipini açıkça veriyoruz
            partialize: (state: SessionState) => ({
                authToken: state.authToken,
                userId: state.userId,
                role: state.role,
            }),
            // Persist edilen şekil düz nesnedir: { authToken, userId, role }
            // Zustand storage objesini { state, version } formatında sarar, tipini açıkça belirtiyoruz
            serialize: (data: { state: SessionState; version?: number }) => JSON.stringify(data.state),
            deserialize: (str: string) => ({ state: JSON.parse(str), version: 0 } as { state: SessionState; version: number }),
        }
    )
);