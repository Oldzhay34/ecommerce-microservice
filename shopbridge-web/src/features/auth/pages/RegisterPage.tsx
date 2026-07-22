import React from 'react';
import { RegisterForm } from '../components/RegisterForm';

export const RegisterPage: React.FC = () => {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-lg">
                <div>
                    <h2 className="text-center text-3xl font-extrabold">
                        <span style={{ color: '#000000' }}>Shop</span><span style={{ color: '#1D4ED8' }}>Bridge</span>
                    </h2>
                    <p className="mt-2 text-center text-sm text-gray-600">Yeni hesap oluşturun</p>
                </div>
                <RegisterForm />
            </div>
        </div>
    );
};