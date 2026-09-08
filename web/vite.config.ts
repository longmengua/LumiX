import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 開發時同源轉送 API，避免為測試帳號 API 放寬跨網域安全策略。
      '/api': 'http://127.0.0.1:8080',
    },
  },
});
