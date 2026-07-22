import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, Link } from 'react-router-dom';
import { otpSchema } from '../schemas';
import { Input, Button, ErrorBanner } from '../../../shared/ui';
import { useAuthStore } from '../store/authStore';
import { useVerifyOtp } from '../hooks/useVerifyOtp';
import type { VerifyOtpRequest } from '../../../api/types';

export const OtpForm: React.FC = () => {
    const navigate = useNavigate();
    const { registrationEmail } = useAuthStore();
    const [countdown, setCountdown] = useState(60);

    // [Varsayım: Kullanıcı sayfayı yenilerse ve email yoksa login'e at]
    useEffect(() => {
        if (!registrationEmail) navigate('/login');
    }, [registrationEmail, navigate]);

    useEffect(() => {
        if (countdown > 0) {
            const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
            return () => clearTimeout(timer);
        }
    }, [countdown]);

    const { register, handleSubmit, formState: { errors } } = useForm<VerifyOtpRequest>({
        resolver: zodResolver(otpSchema),
        defaultValues: { email: registrationEmail || '' }
    });

    // Token kaydetme + dashboard'a (5000, URL kopusuyle) yonlendirme
    // artik useVerifyOtp hook'unda merkezi olarak yapiliyor.
    const { mutate, isPending, error } = useVerifyOtp();

    return (
        <div className="space-y-6">
            <div className="bg-blue-50 p-4 rounded-md text-sm text-blue-800">
                <p><strong>{registrationEmail}</strong> adresine 6 haneli bir doğrulama kodu gönderdik.</p>
                <p className="mt-1">Lütfen mail kutunuzu (ve spam klasörünü) kontrol edin.</p>
            </div>

            <form onSubmit={handleSubmit((data) => mutate(data))} className="space-y-4">
                {error && <ErrorBanner message={error.message} />}

                <input type="hidden" {...register('email')} />
                <Input
                    label="Doğrulama Kodu"
                    placeholder="123456"
                    maxLength={6}
                    {...register('plainTextOtp')}
                    error={errors.plainTextOtp?.message}
                />

                <Button type="submit" isLoading={isPending}>Kodu Doğrula</Button>
            </form>

            <div className="flex flex-col items-center gap-2 mt-6">
                <Button
                    variant="text"
                    disabled={countdown > 0}
                    onClick={() => { /* [Varsayım: Tekrar gönder API çağrısı] */ setCountdown(60); }}
                >
                    {countdown > 0 ? `Tekrar Gönder (${countdown}s)` : 'Kodu Tekrar Gönder'}
                </Button>
                <Link to="/register" className="text-sm text-gray-500 hover:text-brand-shop underline">
                    E-posta adresini değiştir
                </Link>
            </div>
        </div>
    );
};