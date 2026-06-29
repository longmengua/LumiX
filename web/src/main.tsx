import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { App } from './app/App';
import { I18nProvider } from './i18n';
import './styles/global.css';

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
