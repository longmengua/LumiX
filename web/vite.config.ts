import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiTarget = 'http://127.0.0.1:8080';

export default defineConfig(() => {
  // 未明確指定 surface 時採前台最小權限預設，避免一般開發 server 意外承載管理端。
  const surface = process.env.VITE_LUMIX_SURFACE === 'admin' ? 'admin' : 'client';

  return {
    plugins: [
      react(),
      {
        name: 'lumix-surface-boundary',
        configureServer(server) {
          server.middlewares.use((request, response, next) => {
            const pathname = new URL(request.url ?? '/', 'http://localhost').pathname;
            const isAdminPath = pathname === '/admin' || pathname.startsWith('/admin/');
            const isAdminApi = pathname === '/api/admin' || pathname.startsWith('/api/admin/');
            const isApiPath = pathname === '/api' || pathname.startsWith('/api/');
            const isHtmlNavigation = request.headers.accept?.includes('text/html') ?? false;
            const shouldRejectApi = surface === 'admin'
              ? isApiPath && !isAdminApi
              : isAdminApi;
            const shouldRejectPage = isHtmlNavigation && (surface === 'admin' ? !isAdminPath : isAdminPath);

            // 僅限制 browser 導覽，讓 Vite 自己的 module、HMR 與靜態資源可正常載入。
            if (shouldRejectApi || shouldRejectPage) {
              response.statusCode = 404;
              response.setHeader('Content-Type', 'text/plain; charset=utf-8');
              response.end('Not Found');
              return;
            }

            next();
          });
        },
      },
    ],
    server: {
      proxy: surface === 'admin'
        ? {
            // 管理端只可同源呼叫高權限 admin API，避免測試時誤用前台 API。
            '/api/admin': apiTarget,
          }
        : {
            // 前台可使用一般 API；上方 middleware 已先拒絕所有 admin API。
            '/api': apiTarget,
          },
    },
  };
});
