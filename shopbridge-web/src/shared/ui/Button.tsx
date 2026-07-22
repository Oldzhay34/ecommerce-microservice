import React from 'react';
import { Spinner } from './Spinner';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    isLoading?: boolean;
    variant?: 'primary' | 'secondary' | 'outline' | 'text';
}

export const Button: React.FC<ButtonProps> = ({
                                                  children,
                                                  isLoading,
                                                  variant = 'primary',
                                                  className = '',
                                                  disabled,
                                                  ...props
                                              }) => {
    // Şerit efekti için baseStyle'a "relative overflow-hidden group" eklendi
    const baseStyle = "relative overflow-hidden group w-full flex justify-center py-2 px-4 border rounded-md shadow-sm text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed";

    const variants = {
        primary: "border-transparent text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-500", // Tam mavi tonlar
        secondary: "border-transparent text-white bg-brand-shop hover:bg-gray-800 focus:ring-brand-shop",
        outline: "border-gray-300 text-gray-700 bg-white hover:bg-gray-50 focus:ring-brand-bridge",
        text: "border-transparent bg-transparent text-brand-bridge hover:underline shadow-none"
    };

    return (
        <button
            className={`${baseStyle} ${variants[variant]} ${className}`}
            disabled={isLoading || disabled}
            {...props}
        >
            {/* İçeriğin şeridin üstünde kalması için relative z-10 kullanıyoruz */}
            <span className="relative z-10 flex items-center justify-center">
                {isLoading ? <Spinner size="sm" color="text-current" /> : children}
            </span>

            {/* Sadece primary (mavi) varyantında üzerine gelince kayan parlama/şerit efekti */}
            {variant === 'primary' && !isLoading && !disabled && (
                <div className="absolute top-0 left-0 w-full h-full transform -translate-x-[150%] skew-x-[45deg] bg-gradient-to-r from-transparent via-blue-300/40 to-transparent group-hover:translate-x-[150%] transition-transform duration-700 ease-in-out" />
            )}
        </button>
    );
};