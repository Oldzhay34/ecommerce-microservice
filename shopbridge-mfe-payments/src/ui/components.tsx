import React from 'react';

export const Card = ({ children, className = '' }: { children: React.ReactNode, className?: string }) => (
    <div className={`bg-white border rounded-lg shadow-sm p-4 ${className}`}>
        {children}
    </div>
);

export const Button = ({ children, onClick, variant = 'primary', type = 'button', disabled = false }: { children: React.ReactNode, onClick?: () => void, variant?: 'primary' | 'danger' | 'secondary', type?: 'button' | 'submit', disabled?: boolean }) => {
    const base = "px-4 py-2 rounded text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed";
    const variants = {
        primary: "bg-blue-600 text-white hover:bg-blue-700",
        danger: "bg-red-500 text-white hover:bg-red-600",
        secondary: "bg-gray-200 text-gray-800 hover:bg-gray-300"
    };
    return <button type={type} onClick={onClick} disabled={disabled} className={`${base} ${variants[variant]}`}>{children}</button>;
};

export const ErrorBanner = ({ message }: { message: string }) => (
    <div className="p-4 bg-red-50 text-red-700 border-l-4 border-red-500 rounded-r">{message}</div>
);

export const Skeleton = ({ height = 'h-32' }: { height?: string }) => (
    <div className={`w-full ${height} bg-gray-200 animate-pulse rounded-lg`}></div>
);