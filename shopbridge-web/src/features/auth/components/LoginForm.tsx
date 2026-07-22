
import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import { loginSchema } from '../schemas';
import { Input, Button, ErrorBanner } from '../../../shared/ui';
import { useLogin } from '../hooks/useLogin';
import type { LoginRequest } from '../../../api/types';

export const LoginForm: React.FC = () => {
    const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>({
        resolver: zodResolver(loginSchema)
    });
    const { mutate: login, isPending, error } = useLogin();

    const onSubmit = (data: LoginRequest) => login(data);

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {error && <ErrorBanner message={error.message} />}

            <Input
                label="E-posta"
                type="email"
                placeholder="ornek@shopbridge.com"
                {...register('email')}
                error={errors.email?.message}
            />
            <Input
                label="Şifre"
                type="password"
                placeholder="••••••••"
                {...register('password')}
                error={errors.password?.message}
            />

            <div className="flex items-center justify-end">
                <Link to="/forgot-password" className="text-sm text-brand-bridge hover:underline">
                    Şifremi Unuttum
                </Link>
            </div>

            <Button type="submit" isLoading={isPending} className="mt-4">
                Giriş Yap
            </Button>

            <div className="text-center mt-4">
                <span className="text-sm text-gray-600">Hesabınız yok mu? </span>
                <Link to="/register" className="text-sm text-brand-shop font-bold hover:underline">
                    Kayıt Ol
                </Link>
            </div>
        </form>
    );
};