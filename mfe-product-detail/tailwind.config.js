/** @type {import('tailwindcss').Config} */
export default {
    content: ['./index.html', './src/**/*.{ts,tsx}'],
    theme: {
        extend: {
            colors: {
                brand: {
                    DEFAULT: '#1D4ED8',
                    hover: '#1E40AF',
                },
            },
        },
    },
    plugins: [],
};