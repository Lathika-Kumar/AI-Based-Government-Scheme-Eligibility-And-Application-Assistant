import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // ── Auth Service — port 8080 ──────────────────────────────────────────
      '/api/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api/admin/users': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      // ── Scheme Service — port 8081 ────────────────────────────────────────
      '/api/profile': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/dashboard': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/citizen': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/schemes': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/categories': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/applications': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/notifications': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/grievances': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/documents': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/feedback': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/recommendations': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/eligibility': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/admin': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      // Portal namespaces
      '@admin':    path.resolve(__dirname, 'src/admin'),
      '@user':     path.resolve(__dirname, 'src/user'),
      '@public':   path.resolve(__dirname, 'src/public'),
      // Shared infrastructure (used by both portals)
      '@context':    path.resolve(__dirname, 'src/context'),
      '@data':       path.resolve(__dirname, 'src/data'),
      '@utils':      path.resolve(__dirname, 'src/utils'),
      '@components': path.resolve(__dirname, 'src/components'),
      '@config':     path.resolve(__dirname, 'src/config'),
      '@services':   path.resolve(__dirname, 'src/services'),
      '@assets':     path.resolve(__dirname, 'src/assets'),
      '@constants':  path.resolve(__dirname, 'src/constants'),
    },
  },
})
