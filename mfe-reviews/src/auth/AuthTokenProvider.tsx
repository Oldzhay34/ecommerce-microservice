import { createContext, type PropsWithChildren } from 'react';

export const AuthTokenContext = createContext<string | null>(null);

export function AuthTokenProvider({
                                      token, children,
                                  }: PropsWithChildren<{ token: string }>) {
    return (
        <AuthTokenContext.Provider value={token}>{children}</AuthTokenContext.Provider>
    );
}