import { createContext, useMemo, type ReactNode } from 'react';

/**
 * Token bağlamı — remote kendi kopyasını taşır (vendor'lanmıştır).
 * Ortak @shopbridge/ui paketi yoktur; bu bilinçli bir karardır.
 */
export interface AuthTokenContextValue {
    token: string;
}

export const AuthTokenContext = createContext<AuthTokenContextValue | null>(null);

export interface AuthTokenProviderProps {
    token: string;
    children: ReactNode;
}

export function AuthTokenProvider({ token, children }: AuthTokenProviderProps) {
    const value = useMemo<AuthTokenContextValue>(() => ({ token }), [token]);

    return (
        <AuthTokenContext.Provider value={value}>{children}</AuthTokenContext.Provider>
    );
}