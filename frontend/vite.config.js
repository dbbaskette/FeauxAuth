import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/admin/',
  build: {
    outDir: '../src/main/resources/static/admin',
    emptyOutDir: true,
  },
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080',
      '/oauth': 'http://localhost:8080',
      '/.well-known': 'http://localhost:8080',
    },
  },
})
