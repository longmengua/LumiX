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
    <main className="auth-page">
      <section className="auth-page__hero">
        <AuthVisualPanel variant={variant} />
        <div className="auth-page__hero-links">
          <NavLink className="auth-page__home-link" to="/">
            {homeLabel ?? t('common.backHome')}
          </NavLink>
          {showDevNotices ? <p className="auth-page__notice">{t('auth.shell.devNotice')}</p> : null}
        </div>
      </section>

      <Card title={t('auth.shell.cardTitle')}>
        <div className="auth-page__panel">
          {children}
          {footer ? <div className="auth-page__footer">{footer}</div> : null}
        </div>
      </Card>
    </main>
  );
}
