import { useMutation } from '@tanstack/react-query';
import { authApi } from '../../../api/authApi';
import type { LoginRequest, AuthResponse, ApiErrorResponse } from '../../../api/types';
import { useAuthStore } from '../store/authStore';

// JWT içindeki payload'ı okumak için yardımcı fonksiyon
export const decodeJwt = (token: string) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error("Token parse hatası:", e);
        return null;
    }
};

// Rol kontrolü için yardımcı fonksiyon (STORE, ROLE_STORE, authorities vb.)
const hasRole = (decodedToken: any, role: string) =>
    decodedToken?.role === role ||
    decodedToken?.role === `ROLE_${role}` ||
    (Array.isArray(decodedToken?.roles) && decodedToken.roles.includes(role)) ||
    (Array.isArray(decodedToken?.roles) && decodedToken.roles.includes(`ROLE_${role}`)) ||
    (Array.isArray(decodedToken?.authorities) && decodedToken.authorities.includes(`ROLE_${role}`)) ||
    (Array.isArray(decodedToken?.authorities) && decodedToken.authorities.includes(role));

export const useLogin = () => {
    const setToken = useAuthStore((state) => state.setToken);

    return useMutation<AuthResponse, ApiErrorResponse, LoginRequest>({
        mutationFn: authApi.login,
        onSuccess: (data) => {
            const token = data.accessToken;
            setToken(token);

            const decodedToken = decodeJwt(token);

            const isAdmin = hasRole(decodedToken, 'ADMIN');
            const isStore = hasRole(decodedToken, 'STORE');

            // Admin ise 3002, mağaza ise 3001, müşteri ise 5000'e yönlendir
            const targetPort = isAdmin ? '3002' : isStore ? '3001' : '5000';
            const dashboardUrl = new URL(`http://localhost:${targetPort}/`);

            dashboardUrl.searchParams.set('token', token);
            window.location.href = dashboardUrl.toString();
        },
    });
};