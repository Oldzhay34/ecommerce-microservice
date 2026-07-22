export const ENDPOINTS = {
    AUTH: {
        LOGIN: '/api/v1/auth/login',
        REGISTER: '/api/v1/auth/register',
        VERIFY_OTP: '/api/v1/auth/verify-otp',
        FORGOT_PASSWORD: '/api/v1/auth/forgot-password',
    }
} as const;