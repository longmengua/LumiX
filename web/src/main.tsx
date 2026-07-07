async function bootstrap() {
  // 依 URL 決定要掛載一般客戶端或後台；這裡只是前端入口分流，不是權限判定。
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
