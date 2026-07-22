import { apiClient } from './client';
import { ENDPOINTS } from './endpoints';
import type { LoginRequest, RegisterRequest, VerifyOtpRequest, AuthResponse, ForgotPasswordRequest } from './types';

export const authApi = {
    login: async (data: LoginRequest): Promise<AuthResponse> => {
        const response = await apiClient.post<AuthResponse>(ENDPOINTS.AUTH.LOGIN, data);
        return response.data;
    },
    register: async (data: RegisterRequest): Promise<void> => {
        await apiClient.post(ENDPOINTS.AUTH.REGISTER, data);
    },
    verifyOtp: async (data: VerifyOtpRequest): Promise<AuthResponse> => {
        const response = await apiClient.post<AuthResponse>(ENDPOINTS.AUTH.VERIFY_OTP, data);
        return response.data;
    },
    forgotPassword: async (data: ForgotPasswordRequest): Promise<void> => {
        // Backend henüz hazır olmadığı için isteği şimdilik simüle ediyoruz:
        console.log("Simüle edilen şifremi unuttum isteği:", data);
        return new Promise((resolve) => {
            setTimeout(() => {
                // Burada geçici olarak başarılı dönüyoruz
                resolve();
            }, 1000);
        });

        // Backend hazır olduğunda üstteki kısmı silip aşağıdaki yorum satırını açabilirsin:
        // await apiClient.post(ENDPOINTS.AUTH.FORGOT_PASSWORD, data);
    }
};