import React from 'react';

export const Card = ({ children, className = '' }: { children: React.ReactNode, className?: string }) => (
    <div className={`bg-surface border border-border rounded-sb-lg shadow-sb p-4 ${className}`}>
        {children}
    </div>
);

export const Button = ({ children, onClick, variant = 'primary', type = 'button', disabled = false }: { children: React.ReactNode, onClick?: () => void, variant?: 'primary' | 'danger' | 'secondary', type?: 'button' | 'submit', disabled?: boolean }) => {
    const base = "px-4 py-2 text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed";
    const variants = {
        primary: "rounded-full bg-brand text-white hover:bg-brand-hover",
        danger: "rounded-sb bg-danger text-white hover:bg-danger/90",
        secondary: "rounded-sb bg-surface-hover text-ink hover:bg-surface-raised border border-border"
    };
    return <button type={type} onClick={onClick} disabled={disabled} className={`${base} ${variants[variant]}`}>{children}</button>;
};

export const ErrorBanner = ({ message }: { message: string }) => (
    <div className="p-4 bg-danger/10 text-danger border-l-4 border-danger rounded-r">{message}</div>
);

export const Skeleton = ({ height = 'h-32' }: { height?: string }) => (
    <div className={`w-full ${height} bg-surface animate-pulse rounded-sb-lg`}></div>
);