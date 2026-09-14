import { useId } from 'react';

type HelpTooltipProps = {
  message: string;
  label: string;
  className?: string;
};

/**
 * 共用的補充規則提示。
 *
 * <p>僅供理解格式、時區或輸入規則的說明必須收在此元件，避免每個畫面另做常駐提示而破壞表單節奏；
 * button 與 aria-describedby 讓鍵盤及讀屏使用者也能取得 hover 同樣的內容。</p>
 */
export function HelpTooltip({ message, label, className = '' }: HelpTooltipProps) {
  const tooltipId = useId();

  return (
    <button
      className={['help-tooltip', className].filter(Boolean).join(' ')}
      type="button"
      aria-label={label}
      aria-describedby={tooltipId}
    >
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <circle cx="12" cy="12" r="8.5" />
        <path d="M9.8 9a2.35 2.35 0 1 1 3.9 1.76c-.95.78-1.7 1.26-1.7 2.64" />
        <path d="M12 16.9h.01" />
      </svg>
      <span className="help-tooltip__content" id={tooltipId} role="tooltip">{message}</span>
    </button>
  );
}
