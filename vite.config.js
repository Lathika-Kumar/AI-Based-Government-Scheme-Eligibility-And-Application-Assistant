import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
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
    },
  },
})
