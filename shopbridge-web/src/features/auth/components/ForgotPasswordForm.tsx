import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import { forgotPasswordSchema } from '../schemas';
import { Input, Button, ErrorBanner } from '../../../shared/ui';
import { useForgotPassword } from '../hooks/useForgotPassword';
import type { ForgotPasswordRequest } from '../../../api/types';

export const ForgotPasswordForm: React.FC = () => {
    const [isSuccess, setIsSuccess] = useState(false);
    const { register, handleSubmit, formState: { errors } } = useForm<ForgotPasswordRequest>({
        resolver: zodResolver(forgotPasswordSchema)
    });

    const { mutate, isPending, error } = useForgotPassword();

    const onSubmit = (data: ForgotPasswordRequest) => {
        mutate(data, {
            onSuccess: () => setIsSuccess(true)
        });
    };

    if (isSuccess) {
        return (
            <div className="text-center space-y-4">
            <div className="bg-green-50 text-green-800 p-4 rounded-md text-sm">
                E-posta adresinize şifre sıfırlama bağlantısı gönderdik. Lütfen gelen kutunuzu kontrol edin.
        </div>
        <Link to="/login" className="inline-block mt-4 text-sm text-blue-600 font-bold hover:underline">
            Giriş Ekranına Dön
        </Link>
        </div>
    );
    }

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
    {error && <ErrorBanner message={error.message} />}

    <p className="text-sm text-gray-600 mb-4">
        Hesabınıza kayıtlı e-posta adresini girin. Size şifrenizi sıfırlamanız için bir bağlantı göndereceğiz.
    </p>

    <Input
    label="E-posta"
    type="email"
    placeholder="ornek@shopbridge.com"
    {...register('email')}
    error={errors.email?.message}
    />

    <Button type="submit" isLoading={isPending} className="mt-4">
        Sıfırlama Bağlantısı Gönder
    </Button>

    <div className="text-center mt-4">
    <Link to="/login" className="text-sm text-gray-600 hover:text-blue-600 hover:underline">
        Geri Dön
    </Link>
    </div>
    </form>
);
};