import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

import { Card } from '../base/Card';

type AuthPageShellProps = {
  eyebrow: string;
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
};

export function AuthPageShell({ eyebrow, title, description, children, footer }: AuthPageShellProps) {
  return (
    <main className="auth-page">
      <section className="auth-page__hero">
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="lead">{description}</p>

        <div className="auth-page__points">
          <div className="auth-page__point">Root React + TypeScript + Vite</div>
          <div className="auth-page__point">Java 21 + Spring Boot 3 後端接入</div>
          <div className="auth-page__point">交易核心正式目標為 C++ Core</div>
        </div>

        <NavLink className="auth-page__home-link" to="/">
          Back to home
        </NavLink>
      </section>

      <Card title="LumiX Access">
        <div className="auth-page__panel">
          {children}
          {footer ? <div className="auth-page__footer">{footer}</div> : null}
        </div>
      </Card>
    </main>
  );
}
