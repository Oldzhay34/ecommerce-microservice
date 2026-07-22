import React, { useEffect } from 'react';

interface ToastProps {
    message: string;
    onClose: () => void;
}

export const Toast: React.FC<ToastProps> = ({ message, onClose }) => {
    useEffect(() => {
        const timer = setTimeout(() => {
            onClose();
        }, 2500);
        return () => clearTimeout(timer);
    }, [onClose]);

    return (
        <div className="fixed bottom-6 right-6 z-50 bg-[#111827] text-white text-sm font-semibold px-4 py-3 rounded-lg shadow-lg flex items-center space-x-2">
            <span>{message}</span>
        </div>
    );
};