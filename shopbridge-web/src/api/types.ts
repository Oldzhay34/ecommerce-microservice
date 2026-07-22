// --- REQUEST (İSTEK) TİPLERİ ---

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    name: string;
    email: string;
    password: string;
    userType: 'CUSTOMER' | 'STORE'; // Backend'deki role karşılığı
}

export interface VerifyOtpRequest {
    email: string;
    plainTextOtp: string; // Backend'in beklediği OTP değişken adı
}

// --- RESPONSE (CEVAP) TİPLERİ ---

export interface ApiErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
}

export interface AuthResponse {
    accessToken: string;   // "token" değil "accessToken"
    message: string;
}
export interface ForgotPasswordRequest {
    email: string;
}
