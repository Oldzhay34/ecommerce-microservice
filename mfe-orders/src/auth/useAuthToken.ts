import { useContext } from 'react';
import { AuthTokenContext } from './AuthTokenProvider';

export function useAuthToken(): string {
    const token = useContext(AuthTokenContext);
    if (token == null) {
        throw new Error('useAuthToken, AuthTokenProvider içinde kullanılmalıdır.');
    }
    return token;
}