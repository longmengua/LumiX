import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';

const backendTarget = process.env.EXCHANGE_BACKEND || 'http://127.0.0.1:8080';

// Build output goes directly to Spring static resources so "/" is the exchange console in packaged mode.
export default defineConfig({
  plugins: [react()],
  base: './',
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/ws': {
        target: backendTarget,
        ws: true,
        changeOrigin: true,
        rewrite: (path) => path
      }
    }
  },
  build: {
    outDir: resolve(__dirname, '../src/main/resources/static'),
    emptyOutDir: false,
    rollupOptions: {
      input: {
        index: resolve(__dirname, 'index.html')
      },
      output: {
        entryFileNames: 'index.js',
        chunkFileNames: 'index-[name].js',
        assetFileNames: 'index[extname]'
      }
    }
  }
});
