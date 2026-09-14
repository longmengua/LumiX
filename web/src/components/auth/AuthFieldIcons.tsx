type IconProps = {
  className?: string;
};

/**
 * 前台與後台登入欄位共用相同的低對比線框圖示，避免兩個入口對相同帳密概念使用不同視覺語言。
 */
export function MailIcon({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <rect x="3.25" y="5.25" width="17.5" height="13.5" rx="2" />
      <path d="m4.5 7 7.5 5.8L19.5 7" />
    </svg>
  );
}

export function LockIcon({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <rect x="5.25" y="10.25" width="13.5" height="10" rx="2" />
      <path d="M8.5 10.25V7.8a3.5 3.5 0 0 1 7 0v2.45" />
      <path d="M12 14.2v2.3" />
    </svg>
  );
}

/** 顯示名稱屬於個人識別資訊，使用 user 輪廓而非帳號或權限圖示，避免誤導為登入憑證。 */
export function UserIcon({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="8.1" r="3.15" />
      <path d="M5.35 19.25c.72-3.15 3.15-5.05 6.65-5.05s5.93 1.9 6.65 5.05" />
    </svg>
  );
}

export function ArrowRightIcon({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M5 12h13.5" />
      <path d="m14 6.5 5.5 5.5-5.5 5.5" />
    </svg>
  );
}
