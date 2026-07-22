import { createContext, type PropsWithChildren } from 'react';

// MFE'ye özel token context'i. Host prop olarak token'ı geçer;
// bu provider onu alt ağaca (axios adapter) taşır.
export const AuthTokenContext = createContext<string | null>(null);

export function AuthTokenProvider({
                                      token,
                                      children,
                                  }: PropsWithChildren<{ token: string }>) {
    return <AuthTokenContext.Provider value={token}>{children}</AuthTokenContext.Provider>;
}