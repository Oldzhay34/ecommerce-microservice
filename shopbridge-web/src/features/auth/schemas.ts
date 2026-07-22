import { z } from 'zod';

export const loginSchema = z.object({
    email: z.string().email('Geçerli bir e-posta adresi giriniz.'),
    password: z.string().min(1, 'Şifre zorunludur.'),
});

export const registerSchema = z.object({
    name: z.string().min(2, 'Ad Soyad en az 2 karakter olmalıdır.'),
    email: z.string().email('Geçerli bir e-posta adresi giriniz.'),
    password: z.string().min(8, 'Şifre en az 8 karakter olmalıdır.'),
    userType: z.enum(["CUSTOMER", "STORE"], { error: "Lütfen bir hesap tipi seçiniz" }),
});

export const otpSchema = z.object({
    email: z.string().email(),
    plainTextOtp: z.string().length(6, 'Doğrulama kodu 6 haneli olmalıdır.'),
});

export const forgotPasswordSchema = z.object({
    email: z.string().email('Geçerli bir e-posta adresi giriniz.'),
});