import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { defineConfig, loadEnv, type ProxyOptions } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const repositoryDirectory = fileURLToPath(new URL('..', import.meta.url));
  const environment = loadEnv(mode, repositoryDirectory, 'LUMIX_');
  const apiPort = environment.LUMIX_API_PORT || process.env.LUMIX_API_PORT || '8080';
  if (!/^\d+$/.test(apiPort) || Number(apiPort) < 1 || Number(apiPort) > 65535) {
    throw new Error('LUMIX_API_PORT 必須是 1 到 65535 的整數');
  }
  const apiTarget = `http://127.0.0.1:${apiPort}`;
  // 未明確指定 surface 時採前台最小權限預設，避免一般開發 server 意外承載管理端。
  const surface = process.env.VITE_LUMIX_SURFACE === 'admin' ? 'admin' : 'client';

  // 本機行情驗收明確提供憑證才啟用 HTTPS；缺檔或只提供一半時拒絕啟動，不降級傳輸。
  const certificate = process.env.LUMIX_DEV_TLS_CERT;
  const privateKey = process.env.LUMIX_DEV_TLS_KEY;
  if (Boolean(certificate) !== Boolean(privateKey)) throw new Error('本機 HTTPS 必須同時提供憑證與私鑰');
  const https = certificate && privateKey ? { cert: readFileSync(certificate), key: readFileSync(privateKey) } : undefined;
  const apiProxy = (websocket = false): ProxyOptions => ({
    target: apiTarget,
    ws: websocket,
    configure(proxy) {
      // Vite 是本機 loopback TLS 入口；覆寫轉送協定，禁止瀏覽器偽造安全連線狀態。
      // 正式環境仍由受治理的 Nginx 終結 TLS，不使用此 dev server。
      const setTransport = (outgoing: import('node:http').ClientRequest, request: import('node:http').IncomingMessage) => {
        for (const header of ['forwarded', 'x-forwarded-proto', 'x-forwarded-host', 'x-forwarded-port', 'x-forwarded-for', 'x-forwarded-ssl']) outgoing.removeHeader(header);
        outgoing.setHeader('X-Forwarded-Proto', 'encrypted' in request.socket && request.socket.encrypted ? 'https' : 'http');
      };
      proxy.on('proxyReq', setTransport);
      proxy.on('proxyReqWs', setTransport);
    },
  });

  return {
    plugins: [
      react(),
      {
        // 前後台是獨立 bundle；在建置期設定英文管理端 title，避免 Chrome 分頁沿用前台的 LumiX 標題或混用中文名稱。
        name: 'lumix-surface-document-title',
        transformIndexHtml(html) {
          return html.replace('<title>LumiX</title>', `<title>${surface === 'admin' ? 'LumiX Admin' : 'LumiX'}</title>`);
        },
      },
      {
        name: 'lumix-surface-boundary',
        configureServer(server) {
          server.middlewares.use((request, response, next) => {
            const pathname = new URL(request.url ?? '/', 'http://localhost').pathname;
            const isAdminPath = pathname === '/admin' || pathname.startsWith('/admin/');
            const isAdminApi = pathname === '/api/admin' || pathname.startsWith('/api/admin/');
            const isApiPath = pathname === '/api' || pathname.startsWith('/api/');
            const isHtmlNavigation = request.headers.accept?.includes('text/html') ?? false;
            // 管理端根入口依既有服務契約導回同一 port 的登入頁，避免 boundary middleware 誤擋。
            if (surface === 'admin' && pathname === '/' && isHtmlNavigation
              && (request.method === 'GET' || request.method === 'HEAD')) {
              response.statusCode = 302;
              response.setHeader('Location', '/admin/login');
              response.end();
              return;
            }

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
      https,
      host: '127.0.0.1',
      proxy: surface === 'admin'
        ? {
            // 管理端只可同源呼叫高權限 admin API，避免測試時誤用前台 API。
            '/api/admin': apiProxy(),
          }
        : {
            // 前台可使用一般 API；上方 middleware 已先拒絕所有 admin API。
            // 僅固定行情端點可升級 WebSocket；其餘 API 沿用 HTTP 代理與原有認證。
            '^/api/v1/market-data/stream(?:[?].*)?$': apiProxy(true),
            '/api': apiProxy(),
          },
    },
  };
});
