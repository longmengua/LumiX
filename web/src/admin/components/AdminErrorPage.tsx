import type { ReactNode } from 'react';

type AdminErrorPageProps = {
  code: '403' | '404' | '410' | '500' | '503';
  title: string;
  description: string;
  icon: ReactNode;
  primaryAction?: ReactNode;
  secondaryAction?: ReactNode;
};

/** 純呈現元件：由 route、權限 guard 或 boundary 決定語意，避免把 HTTP 判斷綁死在版型。 */
export function AdminErrorPage({ code, title, description, icon, primaryAction, secondaryAction }: AdminErrorPageProps) {
  return (
    <section className="admin-error-page" aria-labelledby="admin-error-title">
      <div className="admin-error-page__card">
        <div className="admin-error-page__icon" aria-hidden="true">{icon}</div>
        <p className="admin-error-page__code">{code}</p>
        <h1 id="admin-error-title">{title}</h1>
        <p className="admin-error-page__description">{description}</p>
        {primaryAction || secondaryAction ? <div className="admin-error-page__actions">{primaryAction}{secondaryAction}</div> : null}
      </div>
    </section>
  );
}

export function InlineErrorState({ title, description, onRetry, retryLabel }: {
  title: string;
  description: string;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  return (
    <section className="admin-inline-error" role="alert" aria-live="polite">
      <ServerErrorIcon />
      <div><h2>{title}</h2><p>{description}</p></div>
      {onRetry && retryLabel ? <button className="secondary-button" type="button" onClick={onRetry}>{retryLabel}</button> : null}
    </section>
  );
}

export function NotFoundIcon() { return <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4M8.5 11h5M11 8.5v5" /></svg>; }
export function ShieldErrorIcon() { return <svg viewBox="0 0 24 24"><path d="M12 3.5 19 6v5.4c0 4.3-2.8 7.3-7 9.1-4.2-1.8-7-4.8-7-9.1V6l7-2.5Z" /><path d="M12 8.2v4.4M12 16h.01" /></svg>; }
export function ServerErrorIcon() { return <svg viewBox="0 0 24 24"><rect x="4" y="4" width="16" height="16" rx="3" /><path d="M8 9h.01M8 15h.01M11 9h5M11 15h5" /></svg>; }
