import type { CSSProperties, ReactNode } from 'react';

import { AuthVisualPanel, type AuthVisualVariant } from './AuthVisualPanel';
import { useI18n } from '../../i18n';

type AuthPageShellProps = {
  variant: AuthVisualVariant;
  children: ReactNode;
  footer?: ReactNode;
  formAreaRatio?: 2 | 3 | 4 | 5;
};

export function AuthPageShell({ variant, children, footer, formAreaRatio = 4 }: AuthPageShellProps) {
  const { t } = useI18n();
  const showDevNotices = import.meta.env.VITE_SHOW_DEV_NOTICES === 'true';

  return (
    <div className="auth-page-shell">
      {showDevNotices ? <p className="auth-page__notice">{t('auth.shell.devNotice')}</p> : null}
      <main className="auth-page-shell__body auth-page card">
        <section className="auth-page__hero">
          <AuthVisualPanel variant={variant} />
        </section>
        <section
          className="auth-page__panel-wrapper"
          aria-label={t('auth.shell.cardTitle')}
          style={{ '--auth-form-area-ratio': formAreaRatio } as CSSProperties}
        >
          {/* 外層專責桌面的 1:N 留白比例，比例由各 auth 頁面明確指定。 */}
          <div className="auth-page__panel">
            <div className="auth-page__form-container">{children}</div>
            {footer ? <div className="auth-page__footer">{footer}</div> : null}
          </div>
        </section>
      </main>
    </div>
  );
}
