import type { InputHTMLAttributes } from 'react';

export function Input({ className = '', ...props }: InputHTMLAttributes<HTMLInputElement>) {
    return (
        <input
            className={`px-3 py-2 rounded-sb border border-border bg-surface text-ink placeholder:text-ink-faint text-sm focus:outline-none focus:ring-2 focus:ring-brand ${className}`}
            {...props}
        />
    );
}