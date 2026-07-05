import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

import { Card } from '../base/Card';
import { AuthVisualPanel, type AuthVisualVariant } from './AuthVisualPanel';
import { useI18n } from '../../i18n';

type AuthPageShellProps = {
  variant: AuthVisualVariant;
  children: ReactNode;
  footer?: ReactNode;
  homeLabel?: string;
};

export function AuthPageShell({ variant, children, footer, homeLabel }: AuthPageShellProps) {
  const { t } = useI18n();
  const showDevNotices = import.meta.env.VITE_SHOW_DEV_NOTICES === 'true';

  return (
    <div className="auth-page-shell">
      <header className="auth-page-shell__header">
        <NavLink className="auth-page__home-link" to="/">
          {homeLabel ?? t('common.backHome')}
        </NavLink>
        {showDevNotices ? <p className="auth-page__notice">{t('auth.shell.devNotice')}</p> : null}
      </header>
      <main className="auth-page-shell__body auth-page">
        <section className="auth-page__hero">
          <AuthVisualPanel variant={variant} />
        </section>
        <Card title={t('auth.shell.cardTitle')}>
          <div className="auth-page__panel">
            {children}
            {footer ? <div className="auth-page__footer">{footer}</div> : null}
          </div>
        </Card>
      </main>
    </div>
  );
}
