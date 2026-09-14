async function bootstrap() {
  // 每份 bundle 在建置時固定服務對象；不得再用 browser path 讓同一份 HTML 同時承載前後台。
  if (import.meta.env.VITE_LUMIX_SURFACE === 'admin') {
    const { mountAdmin } = await import('./admin.tsx');
    mountAdmin();
    return;
  }

  // 未指定時固定為前台，避免本機開發 server 意外提供管理端畫面。
  const { mountClient } = await import('./client.tsx');
  mountClient();
}

void bootstrap();
