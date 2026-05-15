/** @type {import('tailwindcss').Config} */
// Aurora design system tokens — see docs/superpowers/specs/2026-05-13-aurora-design-system-design.md
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        bg: '#07080d',
        'surface-0': '#0c0f17',
        'surface-1': '#11141d',
        'surface-2': '#161a25',
        'surface-3': '#1d2230',
        border: {
          DEFAULT: 'rgba(255,255,255,0.06)',
          strong: 'rgba(255,255,255,0.10)',
        },
        text: {
          DEFAULT: '#e8ebf2',
          dim: '#9aa3b7',
          mute: '#5d6478',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      fontSize: {
        display: ['40px', { lineHeight: '1.1', letterSpacing: '-0.02em', fontWeight: '700' }],
        h1: ['26px', { lineHeight: '1.2', letterSpacing: '-0.015em', fontWeight: '700' }],
        h2: ['20px', { lineHeight: '1.3', letterSpacing: '-0.01em', fontWeight: '600' }],
        body: ['14px', { lineHeight: '1.5' }],
        sm: ['13px', { lineHeight: '1.5' }],
        label: ['11px', { lineHeight: '1.4', letterSpacing: '0.18em', fontWeight: '600' }],
        'mono-sm': ['13px', { lineHeight: '1.6' }],
      },
      borderRadius: {
        sm: '6px',
        DEFAULT: '10px',
        lg: '14px',
        xl: '20px',
      },
      boxShadow: {
        soft: '0 1px 0 rgba(255,255,255,0.04) inset, 0 1px 2px rgba(0,0,0,0.4)',
        elev: '0 20px 60px -20px rgba(0,0,0,0.6), 0 2px 0 rgba(255,255,255,0.03) inset',
        'glow-primary': '0 0 0 1px rgba(139,92,246,0.25), 0 10px 40px -10px rgba(99,102,241,0.5)',
      },
      backgroundImage: {
        'primary-gradient': 'linear-gradient(135deg, #8b5cf6 0%, #6366f1 50%, #22d3ee 110%)',
        'primary-gradient-soft': 'linear-gradient(135deg, rgba(139,92,246,0.18), rgba(99,102,241,0.10) 60%, rgba(34,211,238,0.10))',
        'aurora-bg': 'radial-gradient(ellipse 80% 60% at 20% -10%, rgba(108,75,255,0.18), transparent 60%), radial-gradient(ellipse 60% 50% at 90% 0%, rgba(34,211,238,0.10), transparent 55%), #07080d',
      },
      keyframes: {
        shimmer: {
          '0%': { backgroundPosition: '200% 0' },
          '100%': { backgroundPosition: '-200% 0' },
        },
      },
      animation: {
        shimmer: 'shimmer 1.4s infinite linear',
      },
    },
  },
  plugins: [],
}
