import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Relative base so the built assets work when served from Spring Boot's
  // classpath:/static (i.e. from any path, not just the domain root).
  base: './',
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
