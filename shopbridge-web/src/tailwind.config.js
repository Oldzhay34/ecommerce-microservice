/** @type {import('tailwindcss').Config} */
export default {
    content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
    theme: {
        extend: {
            colors: {
                brand: {
                    shop: "var(--color-brand-shop)",
                    bridge: "var(--color-brand-bridge)", // Mavi
                }
            },
            fontFamily: {
                sans: ['var(--font-family-sans)', 'sans-serif'],
            }
        },
    },
    plugins: [],
}