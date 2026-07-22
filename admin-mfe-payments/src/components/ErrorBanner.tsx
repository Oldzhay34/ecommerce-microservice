import React from 'react';

interface ErrorBannerProps {
    message: string;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({ message }) => {
    return (
        <div className="mb-4 p-4 border border-[#FCA5A5] bg-[#FEF2F2] text-[#B91C1C] text-sm font-medium rounded-lg">
            {message}
        </div>
    );
};