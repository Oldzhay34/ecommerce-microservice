import React from 'react';
import { OtpForm } from '../components/OtpForm';

export const OtpVerifyPage: React.FC = () => {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-lg">
                <div>
                    <h2 className="text-center text-3xl font-extrabold text-brand-shop">E-posta <span className="text-brand-bridge">Onayı</span></h2>
                </div>
                <OtpForm />
            </div>
        </div>
    );
};