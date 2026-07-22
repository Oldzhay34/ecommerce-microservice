import { useMutation } from '@tanstack/react-query';
import { authApi } from '../../../api/authApi';
import type { ForgotPasswordRequest, ApiErrorResponse } from '../../../api/types';

export const useForgotPassword = () => {
    return useMutation<void, ApiErrorResponse, ForgotPasswordRequest>({
        mutationFn: authApi.forgotPassword,
    });
};