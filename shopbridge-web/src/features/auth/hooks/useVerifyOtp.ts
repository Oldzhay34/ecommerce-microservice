import { useMutation } from '@tanstack/react-query';
import { authApi } from '../../../api/authApi';
import type { VerifyOtpRequest, AuthResponse, ApiErrorResponse } from '../../../api/types';
import { useAuthStore } from '../store/authStore';
import { decodeJwt } from './useLogin'; // Yukaridaki decode metodunu kullanıyoruz

export const useVerifyOtp = () => {
    const setToken = useAuthStore((state) => state.setToken);

    return useMutation<AuthResponse, ApiErrorResponse, VerifyOtpRequest>({
        mutationFn: authApi.verifyOtp,
        onSuccess: (data) => {
            const token = data.accessToken;
            setToken(token);

            const decodedToken = decodeJwt(token);

            const isStore =
                decodedToken?.role === 'STORE' ||
                decodedToken?.role === 'ROLE_STORE' ||
                (decodedToken?.roles && decodedToken.roles.includes('STORE')) ||
                (decodedToken?.authorities && decodedToken.authorities.includes('ROLE_STORE'));

            const targetPort = isStore ? '6005' : '5000';
            const dashboardUrl = new URL(`http://localhost:${targetPort}/`);

            dashboardUrl.searchParams.set('token', token);
            window.location.href = dashboardUrl.toString();
        },
    });
};