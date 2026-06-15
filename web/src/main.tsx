import React from 'react';
import { createRoot } from 'react-dom/client';
import { ExchangeConsole } from './App';
import { I18nProvider } from './lib/i18n';
import './styles/app.css';

// React entrypoint: mount the exchange console into the static Spring resources bundle.
// Keep it tiny so build output stays predictable and root index.html can remain the exchange entry.
createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <I18nProvider>
      <ExchangeConsole />
    </I18nProvider>
  </React.StrictMode>
);
