import type { ReactNode } from 'react';

import { AuthVisualPanel, type AuthVisualVariant } from './AuthVisualPanel';
import { useI18n } from '../../i18n';

type AuthPageShellProps = {
  variant: AuthVisualVariant;
  children: ReactNode;
  footer?: ReactNode;
};

export function AuthPageShell({ variant, children, footer }: AuthPageShellProps) {
  const { t } = useI18n();
  const showDevNotices = import.meta.env.VITE_SHOW_DEV_NOTICES === 'true';

  return (
    <div className="auth-page-shell">
      {showDevNotices ? <p className="auth-page__notice">{t('auth.shell.devNotice')}</p> : null}
      <main className="auth-page-shell__body auth-page card">
        <section className="auth-page__hero">
          <AuthVisualPanel variant={variant} />
        </section>
        <section className="auth-page__panel-wrapper" aria-label={t('auth.shell.cardTitle')}>
          {/* 外層專責桌面的 1:3 留白比例，避免表單內容高度影響其起始位置。 */}
          <div className="auth-page__panel">
            <div className="auth-page__form-container">{children}</div>
            {footer ? <div className="auth-page__footer">{footer}</div> : null}
          </div>
        </section>
      </main>
    </div>
  );
}
