import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

import { Logo } from '../brand/Logo';
import { Card } from '../base/Card';
import { useI18n } from '../../i18n';

type AuthPageShellProps = {
  eyebrow: string;
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
  homeLabel?: string;
};

export function AuthPageShell({ eyebrow, title, description, children, footer, homeLabel }: AuthPageShellProps) {
  const { t } = useI18n();
  const showDevNotices = import.meta.env.VITE_SHOW_DEV_NOTICES === 'true';

  return (
    <main className="auth-page">
      <section className="auth-page__hero">
        <Logo className="auth-page__brand" size="lg" title={t('nav.logo')} variant="full" />
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="lead">{description}</p>

        <NavLink className="auth-page__home-link" to="/">
          {homeLabel ?? t('common.backHome')}
        </NavLink>

        {showDevNotices ? <p className="auth-page__notice">{t('auth.shell.devNotice')}</p> : null}
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
