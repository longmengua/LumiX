import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { I18nProvider } from './i18n';
import { AdminApp } from './admin/AdminApp';
import './styles/global.css';

export function mountAdmin() {
  // 後台入口使用 /admin basename，避免與客戶端路由互相污染。
  const container = document.getElementById('root');

  if (!container) {
    throw new Error('Root element #root not found');
  }

  ReactDOM.createRoot(container).render(
    <React.StrictMode>
      <I18nProvider>
        <BrowserRouter basename="/admin">
          <AdminApp />
        </BrowserRouter>
      </I18nProvider>
    </React.StrictMode>,
  );
}
