/** @type {import('tailwindcss').Config} */
export default {
    content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
    theme: {
        extend: {
            colors: {
                brand: {
                    DEFAULT: '#1D4ED8',
                    hover: '#1E40AF',
                },
                ink: {
                    primary: '#111827',
                    secondary: '#6B7280',
                    muted: '#9CA3AF',
                },
                line: {
                    card: '#E5E7EB',
                    field: '#D1D5DB',
                },
                canvas: '#F4F5F7',
            },
        },
    },
    plugins: [],
};