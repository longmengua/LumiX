import type { CSSProperties } from 'react';

import { Logo } from '../brand/Logo';
import { useI18n } from '../../i18n';

export type AuthVisualVariant = 'login' | 'register' | 'forgot' | 'twoFactor' | 'reset';

type AuthVisualPanelProps = {
  variant: AuthVisualVariant;
};

type Tone = {
  kickerKey: string;
  titleKey: string;
  subtitleKey: string;
  accent: string;
  gradientId: string;
};

const tones: Record<AuthVisualVariant, Tone> = {
  login: {
    kickerKey: 'auth.visual.login.kicker',
    titleKey: 'auth.visual.login.title',
    subtitleKey: 'auth.visual.login.subtitle',
    accent: '#60a5fa',
    gradientId: 'auth-visual-login-gradient',
  },
  register: {
    kickerKey: 'auth.visual.register.kicker',
    titleKey: 'auth.visual.register.title',
    subtitleKey: 'auth.visual.register.subtitle',
    accent: '#818cf8',
    gradientId: 'auth-visual-register-gradient',
  },
  forgot: {
    kickerKey: 'auth.visual.forgot.kicker',
    titleKey: 'auth.visual.forgot.title',
    subtitleKey: 'auth.visual.forgot.subtitle',
    accent: '#22d3ee',
    gradientId: 'auth-visual-forgot-gradient',
  },
  twoFactor: {
    kickerKey: 'auth.visual.twoFactor.kicker',
    titleKey: 'auth.visual.twoFactor.title',
    subtitleKey: 'auth.visual.twoFactor.subtitle',
    accent: '#38bdf8',
    gradientId: 'auth-visual-twofactor-gradient',
  },
  reset: {
    kickerKey: 'auth.visual.reset.kicker',
    titleKey: 'auth.visual.reset.title',
    subtitleKey: 'auth.visual.reset.subtitle',
    accent: '#a855f7',
    gradientId: 'auth-visual-reset-gradient',
  },
};

function LoginMotif({ gradientId }: { gradientId: string }) {
  return (
    <svg aria-hidden="true" className="auth-visual-panel__svg" viewBox="0 0 560 560">
      <defs>
        <linearGradient id={gradientId} x1="0%" x2="100%" y1="0%" y2="100%">
          <stop offset="0%" stopColor="#60a5fa" />
          <stop offset="55%" stopColor="#818cf8" />
          <stop offset="100%" stopColor="#c084fc" />
        </linearGradient>
      </defs>
      <circle className="auth-visual-panel__orbit auth-visual-panel__orbit--large" cx="280" cy="280" r="190" />
      <circle className="auth-visual-panel__orbit auth-visual-panel__orbit--medium" cx="280" cy="280" r="128" />
      <path className="auth-visual-panel__ring auth-visual-panel__ring--outer" d="M280 72a208 208 0 1 1 0 416 208 208 0 0 1 0-416Z" />
      <path className="auth-visual-panel__ring auth-visual-panel__ring--inner" d="M280 138a142 142 0 1 1 0 284 142 142 0 0 1 0-284Z" />
      <path className="auth-visual-panel__shield" d="M280 156 368 188v74c0 60-35 106-88 136-53-30-88-76-88-136v-74l88-32Z" />
      <path className="auth-visual-panel__shield-line" d="M280 198v112" />
      <path className="auth-visual-panel__shield-line" d="M244 234h72" />
      <path className="auth-visual-panel__shield-line" d="M252 286h56" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--one" cx="142" cy="180" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--two" cx="414" cy="164" r="6" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--three" cx="432" cy="332" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--four" cx="138" cy="352" r="6" />
      <g className="auth-visual-panel__motion auth-visual-panel__motion--login">
        <circle className="auth-visual-panel__dot" cx="280" cy="86" r="5" />
        <circle className="auth-visual-panel__dot" cx="454" cy="280" r="5" />
        <circle className="auth-visual-panel__dot" cx="280" cy="474" r="5" />
        <circle className="auth-visual-panel__dot" cx="106" cy="280" r="5" />
      </g>
    </svg>
  );
}

function RegisterMotif({ gradientId }: { gradientId: string }) {
  return (
    <svg aria-hidden="true" className="auth-visual-panel__svg" viewBox="0 0 560 560">
      <defs>
        <linearGradient id={gradientId} x1="0%" x2="100%" y1="0%" y2="100%">
          <stop offset="0%" stopColor="#38bdf8" />
          <stop offset="50%" stopColor="#6366f1" />
          <stop offset="100%" stopColor="#a855f7" />
        </linearGradient>
      </defs>
      <path className="auth-visual-panel__gridline" d="M120 160h320M120 280h320M120 400h320" />
      <path className="auth-visual-panel__gridline" d="M180 110v340M280 110v340M380 110v340" />
      <circle className="auth-visual-panel__orbit auth-visual-panel__orbit--large" cx="280" cy="280" r="190" />
      <circle className="auth-visual-panel__orbit auth-visual-panel__orbit--medium" cx="280" cy="280" r="128" />
      <circle className="auth-visual-panel__profile" cx="230" cy="224" r="30" />
      <path className="auth-visual-panel__profile" d="M178 372c12-42 43-64 52-64s40 22 52 64" />
      <path className="auth-visual-panel__growth" d="M216 324 258 286 298 304 352 244" />
      <path className="auth-visual-panel__growth-arrow" d="M352 244v32h-32" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--one" cx="216" cy="324" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--two" cx="258" cy="286" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--three" cx="298" cy="304" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--four" cx="352" cy="244" r="7" />
      <g className="auth-visual-panel__motion auth-visual-panel__motion--register">
        <circle className="auth-visual-panel__dot" cx="150" cy="178" r="5" />
        <circle className="auth-visual-panel__dot" cx="410" cy="152" r="5" />
        <circle className="auth-visual-panel__dot" cx="426" cy="358" r="5" />
        <circle className="auth-visual-panel__dot" cx="146" cy="372" r="5" />
      </g>
    </svg>
  );
}

function ForgotMotif({ gradientId }: { gradientId: string }) {
  return (
    <svg aria-hidden="true" className="auth-visual-panel__svg" viewBox="0 0 560 560">
      <defs>
        <linearGradient id={gradientId} x1="0%" x2="100%" y1="0%" y2="100%">
          <stop offset="0%" stopColor="#22d3ee" />
          <stop offset="55%" stopColor="#60a5fa" />
          <stop offset="100%" stopColor="#818cf8" />
        </linearGradient>
      </defs>
      <circle className="auth-visual-panel__orbit auth-visual-panel__orbit--large" cx="280" cy="280" r="192" />
      <path className="auth-visual-panel__ring auth-visual-panel__ring--outer" d="M154 280a126 126 0 1 0 252 0" />
      <path className="auth-visual-panel__ring auth-visual-panel__ring--inner" d="M196 280a84 84 0 1 0 168 0" />
      <rect className="auth-visual-panel__lock" x="216" y="246" width="128" height="114" rx="24" />
      <path className="auth-visual-panel__lock-line" d="M246 246v-24c0-18 16-32 34-32s34 14 34 32v24" />
      <path className="auth-visual-panel__key" d="M350 332l28-28 18 18-28 28h-16v-18h-16v-16h14Z" />
      <path className="auth-visual-panel__recovery" d="M310 372c44-8 76-38 88-82" />
      <path className="auth-visual-panel__recovery-arrow" d="M390 282l10 12-14 4" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--one" cx="160" cy="186" r="6" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--two" cx="392" cy="178" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--three" cx="408" cy="338" r="5" />
      <g className="auth-visual-panel__motion auth-visual-panel__motion--forgot">
        <circle className="auth-visual-panel__dot" cx="280" cy="88" r="5" />
        <circle className="auth-visual-panel__dot" cx="456" cy="280" r="5" />
        <circle className="auth-visual-panel__dot" cx="280" cy="472" r="5" />
        <circle className="auth-visual-panel__dot" cx="104" cy="280" r="5" />
      </g>
    </svg>
  );
}

function TwoFactorMotif({ gradientId }: { gradientId: string }) {
  return (
    <svg aria-hidden="true" className="auth-visual-panel__svg" viewBox="0 0 560 560">
      <defs>
        <linearGradient id={gradientId} x1="0%" x2="100%" y1="0%" y2="100%">
          <stop offset="0%" stopColor="#60a5fa" />
          <stop offset="60%" stopColor="#22d3ee" />
          <stop offset="100%" stopColor="#8b5cf6" />
        </linearGradient>
      </defs>
      <circle className="auth-visual-panel__orbit auth-visual-panel__orbit--large" cx="280" cy="280" r="188" />
      <rect className="auth-visual-panel__device" x="182" y="126" width="196" height="308" rx="36" />
      <rect className="auth-visual-panel__device-screen" x="206" y="164" width="148" height="168" rx="24" />
      <path className="auth-visual-panel__device-line" d="M236 354h88" />
      <path className="auth-visual-panel__verify" d="M232 248l28 28 52-60" />
      <circle className="auth-visual-panel__dot auth-visual-panel__dot--strong" cx="232" cy="202" r="10" />
      <circle className="auth-visual-panel__dot auth-visual-panel__dot--strong" cx="280" cy="202" r="10" />
      <circle className="auth-visual-panel__dot auth-visual-panel__dot--strong" cx="328" cy="202" r="10" />
      <path className="auth-visual-panel__verify" d="M202 118c18-28 50-46 78-46s60 18 78 46" />
      <path className="auth-visual-panel__verify" d="M202 442c18 28 50 46 78 46s60-18 78-46" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--one" cx="136" cy="228" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--two" cx="426" cy="218" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--three" cx="142" cy="348" r="6" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--four" cx="416" cy="348" r="6" />
      <g className="auth-visual-panel__motion auth-visual-panel__motion--twofactor">
        <circle className="auth-visual-panel__dot" cx="280" cy="88" r="5" />
        <circle className="auth-visual-panel__dot" cx="456" cy="280" r="5" />
        <circle className="auth-visual-panel__dot" cx="280" cy="472" r="5" />
        <circle className="auth-visual-panel__dot" cx="104" cy="280" r="5" />
      </g>
    </svg>
  );
}

function ResetMotif({ gradientId }: { gradientId: string }) {
  return (
    <svg aria-hidden="true" className="auth-visual-panel__svg" viewBox="0 0 560 560">
      <defs>
        <linearGradient id={gradientId} x1="0%" x2="100%" y1="0%" y2="100%">
          <stop offset="0%" stopColor="#38bdf8" />
          <stop offset="60%" stopColor="#818cf8" />
          <stop offset="100%" stopColor="#f472b6" />
        </linearGradient>
      </defs>
      <circle className="auth-visual-panel__orbit auth-visual-panel__orbit--large" cx="280" cy="280" r="188" />
      <path className="auth-visual-panel__ring auth-visual-panel__ring--outer" d="M160 280a120 120 0 1 0 240 0" />
      <path className="auth-visual-panel__ring auth-visual-panel__ring--inner" d="M202 280a78 78 0 1 0 156 0" />
      <rect className="auth-visual-panel__lock" x="216" y="230" width="128" height="126" rx="24" />
      <path className="auth-visual-panel__lock-line" d="M246 230v-22c0-18 16-32 34-32s34 14 34 32v22" />
      <path className="auth-visual-panel__refresh" d="M368 232a116 116 0 0 0-20-30" />
      <path className="auth-visual-panel__refresh-arrow" d="M340 198l18 4-8 16" />
      <path className="auth-visual-panel__refresh" d="M192 304a118 118 0 0 0 22 46" />
      <path className="auth-visual-panel__refresh-arrow" d="M208 352l-2-18-16 6" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--one" cx="164" cy="178" r="6" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--two" cx="398" cy="176" r="6" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--three" cx="392" cy="362" r="7" />
      <circle className="auth-visual-panel__node auth-visual-panel__node--four" cx="158" cy="358" r="7" />
      <g className="auth-visual-panel__motion auth-visual-panel__motion--reset">
        <circle className="auth-visual-panel__dot" cx="280" cy="88" r="5" />
        <circle className="auth-visual-panel__dot" cx="456" cy="280" r="5" />
        <circle className="auth-visual-panel__dot" cx="280" cy="472" r="5" />
        <circle className="auth-visual-panel__dot" cx="104" cy="280" r="5" />
      </g>
    </svg>
  );
}

export function AuthVisualPanel({ variant }: AuthVisualPanelProps) {
  const { t } = useI18n();
  const tone = tones[variant];

  return (
    <section
      className={`auth-visual-panel auth-visual-panel--${variant}`}
      style={{ '--auth-visual-accent': tone.accent } as CSSProperties}
      aria-labelledby={`auth-visual-${variant}-title`}
    >
      <div className="auth-visual-panel__glow auth-visual-panel__glow--one" aria-hidden="true" />
      <div className="auth-visual-panel__glow auth-visual-panel__glow--two" aria-hidden="true" />
      <div className="auth-visual-panel__grid" aria-hidden="true" />

      <div className="auth-visual-panel__content">
        <div className="auth-visual-panel__brand">
          <Logo size="sm" title={t('nav.logo')} variant="mark" />
          <span className="auth-visual-panel__brand-text">{t('nav.logo')}</span>
        </div>
        <p className="auth-visual-panel__subtitle">{t(tone.subtitleKey)}</p>
      </div>

      <div className="auth-visual-panel__art" aria-hidden="true">
        <div className="auth-visual-panel__halo" />
        {variant === 'login' ? <LoginMotif gradientId={tone.gradientId} /> : null}
        {variant === 'register' ? <RegisterMotif gradientId={tone.gradientId} /> : null}
        {variant === 'forgot' ? <ForgotMotif gradientId={tone.gradientId} /> : null}
        {variant === 'twoFactor' ? <TwoFactorMotif gradientId={tone.gradientId} /> : null}
        {variant === 'reset' ? <ResetMotif gradientId={tone.gradientId} /> : null}
      </div>
    </section>
  );
}
