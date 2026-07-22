import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../../api/authApi';
import type { RegisterRequest, ApiErrorResponse } from '../../../api/types';
import { useAuthStore } from '../store/authStore';

export const useRegister = () => {
    const navigate = useNavigate();
    const setRegistrationEmail = useAuthStore((state) => state.setRegistrationEmail);

    return useMutation<void, ApiErrorResponse, RegisterRequest>({
        mutationFn: authApi.register,
        onSuccess: (_, variables) => {
            // OTP ekranında tekrar sorulmaması için e-posta adresi state'e aktarılır
            setRegistrationEmail(variables.email);
            navigate('/verify-otp');
        },
    });
};