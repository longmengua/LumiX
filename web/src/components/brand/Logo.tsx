import { useId } from 'react';

type LogoSize = 'sm' | 'md' | 'lg';
type LogoVariant = 'full' | 'mark';

type LogoProps = {
  variant?: LogoVariant;
  size?: LogoSize;
  className?: string;
  title?: string;
};

export function Logo({ variant = 'full', size = 'md', className, title = 'LumiX' }: LogoProps) {
  const gradientId = useId();
  const rootClassName = ['logo', `logo--${variant}`, `logo--${size}`, className].filter(Boolean).join(' ');

  return (
    <span className={rootClassName} aria-label={title} role="img">
      <svg aria-hidden="true" className="logo__mark" viewBox="0 0 40 40">
        <defs>
          <linearGradient id={gradientId} x1="0%" x2="100%" y1="0%" y2="100%">
            <stop offset="0%" stopColor="#60a5fa" />
            <stop offset="100%" stopColor="#818cf8" />
          </linearGradient>
        </defs>
        <rect x="6" y="6" width="28" height="28" rx="10" fill="rgba(2, 6, 23, 0.72)" stroke={`url(#${gradientId})`} strokeWidth="1.5" />
        <path d="M14 12V28H24" fill="none" stroke={`url(#${gradientId})`} strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.8" />
        <path d="M24 12L31 19" fill="none" stroke={`url(#${gradientId})`} strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.8" />
        <path d="M31 12L21 22" fill="none" stroke={`url(#${gradientId})`} strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.8" />
        <path d="M21 22L31 28" fill="none" stroke={`url(#${gradientId})`} strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.8" />
      </svg>
      {variant === 'full' ? <span className="logo__wordmark">LumiX</span> : null}
    </span>
  );
}
