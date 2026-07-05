import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { I18nProvider } from './i18n';
import { AdminApp } from './admin/AdminApp';
import './styles/global.css';

export function mountAdmin() {
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
