import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { App } from './app/App';
import { I18nProvider } from './i18n';
import './styles/global.css';

export function mountClient() {
  // 客戶端入口只負責把 React App 掛到 #root；真實 API 與認證邏輯都應該在各自模組內處理。
  const container = document.getElementById('root');

  if (!container) {
    throw new Error('Root element #root not found');
  }

  ReactDOM.createRoot(container).render(
    <React.StrictMode>
      <I18nProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </I18nProvider>
    </React.StrictMode>,
  );
}
