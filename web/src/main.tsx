async function bootstrap() {
  // Compose 的兩個入口在 build 時固定 surface，避免不同 port 又退回同一個 URL 判斷入口。
  const surface = import.meta.env.VITE_LUMIX_SURFACE;
  if (surface === 'admin') {
    const { mountAdmin } = await import('./admin.tsx');
    mountAdmin();
    return;
  }
  if (surface === 'client') {
    const { mountClient } = await import('./client.tsx');
    mountClient();
    return;
  }

  // Vite 單機開發仍可用 path 分流；此分支不得作為 Compose 的前後台隔離機制。
  const pathname = window.location.pathname;
  if (pathname === '/admin' || pathname.startsWith('/admin/')) {
    const { mountAdmin } = await import('./admin.tsx');
    mountAdmin();
    return;
  }

  const { mountClient } = await import('./client.tsx');
  mountClient();
}

void bootstrap();
