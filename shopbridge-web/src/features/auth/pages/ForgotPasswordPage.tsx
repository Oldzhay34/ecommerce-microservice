import React from 'react';
import { ForgotPasswordForm } from '../components/ForgotPasswordForm';

export const ForgotPasswordPage: React.FC = () => {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-lg">
                <div>
                    <h2 className="text-center text-3xl font-extrabold">
                        Şifremi <span style={{ color: '#1D4ED8' }}>Unuttum</span>
                    </h2>
                    <p className="mt-2 text-center text-sm text-gray-600">
                        Şifrenizi güvenle sıfırlayın
                    </p>
                </div>
                <ForgotPasswordForm />
            </div>
        </div>
    );
};