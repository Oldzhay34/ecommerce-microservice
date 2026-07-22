
import React, { forwardRef } from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
    label: string;
    error?: string;
    helpText?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
    ({ label, error, helpText, id, ...props }, ref) => {
        const inputId = id || props.name;
        return (
            <div className="mb-4 text-left">
                <label htmlFor={inputId} className="block text-sm font-medium text-gray-700">
                    {label}
                </label>
                <div className="mt-1">
                    <input
                        id={inputId}
                        ref={ref}
                        className={`appearance-none block w-full px-3 py-2 border rounded-md shadow-sm placeholder-gray-400 focus:outline-none sm:text-sm ${
                            error ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-gray-300 focus:ring-brand-bridge focus:border-brand-bridge'
                        }`}
                        {...props}
                    />
                </div>
                {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
                {!error && helpText && <p className="mt-2 text-sm text-gray-500">{helpText}</p>}
            </div>
        );
    }
);
Input.displayName = 'Input';