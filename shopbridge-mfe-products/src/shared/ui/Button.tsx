import type { ButtonHTMLAttributes } from 'react';

export function Button({ className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
    return (
        <button
            className={`px-4 py-2 rounded-lg text-sm font-medium bg-blue-700 text-white hover:bg-blue-800 disabled:opacity-40 disabled:cursor-not-allowed transition ${className}`}
            {...props}
        />
    );
}