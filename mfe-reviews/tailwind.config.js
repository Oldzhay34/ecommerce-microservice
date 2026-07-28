/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // ShopBridge premium light palette — legacy token names kept, values
        // remapped to the light system so class names elsewhere stay valid.
        brand: '#1D4ED8', 'brand-dark': '#1E40AF',
        ink: '#0A0A0A', muted: '#6B7280', faint: '#9CA3AF',
        canvas: '#FFFFFF', line: '#E5E7EB', 'line-strong': '#D1D5DB',
        surface: { DEFAULT: '#FFFFFF', hover: '#F7F8FA', raised: '#FFFFFF' },
        'ok-bg': '#ECFDF5', 'ok-tx': '#16A34A',
        'info-bg': '#EFF6FF', 'info-tx': '#1D4ED8',
        'warn-bg': '#FFFBEB', 'warn-tx': '#D97706',
        'err-bg': '#FEF2F2', 'err-tx': '#DC2626',
        'neutral-bg': '#F3F4F6', 'neutral-tx': '#6B7280',
        'danger-line': '#DC2626',
        star: '#F5A524',
      },
      borderRadius: { card: '12px', btn: '8px', pill: '999px' },
      boxShadow: {
        sb: '0 1px 2px rgba(0,0,0,0.4), 0 0 0 1px rgba(255,255,255,0.03)',
        'sb-lg': '0 12px 32px -8px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.04)',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      maxWidth: { content: '1120px' },
    },
  },
  plugins: [],
};