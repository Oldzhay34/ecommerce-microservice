import type { ButtonHTMLAttributes } from 'react';

export function Button({ className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
    return (
        <button
            className={`px-4 py-2 rounded-full text-sm font-medium bg-brand text-white hover:bg-brand-hover disabled:opacity-40 disabled:cursor-not-allowed transition ${className}`}
            {...props}
        />
    );
}