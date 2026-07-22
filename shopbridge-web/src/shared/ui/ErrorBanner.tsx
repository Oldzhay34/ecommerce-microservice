
import React from 'react';

interface ErrorBannerProps {
    message: string;
    title?: string;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({ message, title = 'Bir Hata Oluştu' }) => {
    if (!message) return null;
    return (
        <div className="bg-red-50 border-l-4 border-red-400 p-4 mb-4" role="alert">
            <div className="flex">
                <div className="ml-3">
                    <h3 className="text-sm font-medium text-red-800">{title}</h3>
                    <div className="mt-2 text-sm text-red-700">
                        <p>{message}</p>
                    </div>
                </div>
            </div>
        </div>
    );
};