import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { registerSchema } from '../schemas';
import { Input, Button, ErrorBanner } from '../../../shared/ui';
import { useAuthStore } from '../store/authStore';
import { useMutation } from '@tanstack/react-query';
import { authApi } from '../../../api/authApi';
import type { RegisterRequest, ApiErrorResponse } from '../../../api/types';

export const RegisterForm: React.FC = () => {
    const navigate = useNavigate();
    const setRegistrationEmail = useAuthStore(state => state.setRegistrationEmail);

    // Schema ile tam uyumlu form tanımı
    const { register, handleSubmit, formState: { errors } } = useForm<RegisterRequest>({
        resolver: zodResolver(registerSchema),
        defaultValues: { userType: 'CUSTOMER' }
    });

    const { mutate, isPending, error } = useMutation<void, ApiErrorResponse, RegisterRequest>({
        mutationFn: authApi.register,
        onSuccess: (_, variables) => {
            setRegistrationEmail(variables.email);
            navigate('/verify-otp');
        }
    });

    const onSubmit = (data: RegisterRequest) => mutate(data);

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* Hata mesajı varsa banner göster */}
            {error && <ErrorBanner message={error.message} />}

            <Input label="Ad Soyad" placeholder="Ahmet Yılmaz" {...register('name')} error={errors.name?.message} />
            <Input label="E-posta" type="email" placeholder="ornek@mail.com" {...register('email')} error={errors.email?.message} />

            <div className="space-y-1">
                <Input
                    label="Şifre"
                    type="password"
                    placeholder="••••••••"
                    {...register('password')}
                    error={errors.password?.message}
                />
                <p className="text-xs text-gray-500">Şifreniz en az 8 karakter olmalıdır.</p>
            </div>

            <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">Hesap Tipi</label>
                <div className="flex gap-4">
                    <label className="flex items-center cursor-pointer">
                        <input type="radio" value="CUSTOMER" {...register('userType')} className="text-blue-600 focus:ring-blue-500" />
                        <span className="ml-2 text-sm text-gray-700">Müşteri</span>
                    </label>
                    <label className="flex items-center cursor-pointer">
                        <input type="radio" value="STORE" {...register('userType')} className="text-blue-600 focus:ring-blue-500" />
                        <span className="ml-2 text-sm text-gray-700">Mağaza</span>
                    </label>
                </div>
                {errors.userType && <p className="mt-1 text-sm text-red-600">{errors.userType.message}</p>}
            </div>

            <Button type="submit" isLoading={isPending}>Kayıt Ol</Button>

            <div className="text-center mt-4 text-sm text-gray-600">
                Zaten hesabınız var mı? <Link to="/login" className="text-blue-600 font-bold hover:underline">Giriş Yap</Link>
            </div>
        </form>
    );
};