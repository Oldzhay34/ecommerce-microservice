import React, { createContext, useContext } from 'react';

const AuthTokenContext = createContext<string | null>(null);

export const AuthTokenProvider: React.FC<{ token: string; children: React.ReactNode }> = ({ token, children }) => {
    return <AuthTokenContext.Provider value={token}>{children}</AuthTokenContext.Provider>;
};

export const useAuthToken = () => useContext(AuthTokenContext);