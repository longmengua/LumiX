async function bootstrap() {
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
