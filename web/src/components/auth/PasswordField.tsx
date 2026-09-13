import { useState, type KeyboardEvent, type ReactNode } from 'react';

type PasswordFieldProps = {
  label: string;
  value: string;
  onChange(value: string): void;
  autoComplete: 'current-password' | 'new-password';
  placeholder?: string;
  name?: string;
  className?: string;
  inputClassName?: string;
  leadingAdornment?: ReactNode;
  iconOnlyToggle?: boolean;
  showPasswordLabel?: string;
  hidePasswordLabel?: string;
  capsLockWarning?: string;
};

/**
 * 可切換遮罩的密碼欄位。
 *
 * 密碼仍只留在受控表單 state；切換按鈕只改變瀏覽器呈現方式，不會複製、記錄或持久化密碼。
 */
export function PasswordField({
  label,
  value,
  onChange,
  autoComplete,
  placeholder,
  name,
  className,
  inputClassName,
  leadingAdornment,
  iconOnlyToggle = false,
  showPasswordLabel,
  hidePasswordLabel,
  capsLockWarning,
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  const [capsLockOn, setCapsLockOn] = useState(false);
  const visibleLabel = hidePasswordLabel ?? `隱藏${label}`;
  const hiddenLabel = showPasswordLabel ?? `顯示${label}`;

  function updateCapsLockState(event: KeyboardEvent<HTMLInputElement>) {
    // Caps Lock 僅是輸入提醒，不會被保存或送到 API，避免在密碼流程增加任何敏感狀態。
    setCapsLockOn(event.getModifierState('CapsLock'));
  }

  return (
    <label className={['field', className].filter(Boolean).join(' ')}>
      <span className="field__label">{label}</span>
      <span className={['password-field', leadingAdornment ? 'password-field--has-leading-adornment' : ''].filter(Boolean).join(' ')}>
        {leadingAdornment ? <span className="password-field__leading-adornment" aria-hidden="true">{leadingAdornment}</span> : null}
        <input
          className={['input', 'password-field__input', inputClassName].filter(Boolean).join(' ')}
          name={name}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={updateCapsLockState}
          onKeyUp={updateCapsLockState}
          onBlur={() => setCapsLockOn(false)}
          placeholder={placeholder}
          autoComplete={autoComplete}
        />
        <button
          className={['password-field__toggle', iconOnlyToggle ? 'password-field__toggle--icon-only' : ''].filter(Boolean).join(' ')}
          type="button"
          aria-label={visible ? visibleLabel : hiddenLabel}
          aria-pressed={visible}
          title={visible ? visibleLabel : hiddenLabel}
          onClick={() => setVisible((current) => !current)}
        >
          {iconOnlyToggle ? <PasswordVisibilityIcon visible={visible} /> : visible ? '隱藏' : '顯示'}
        </button>
      </span>
      {capsLockWarning && capsLockOn ? <span className="password-field__caps-lock" role="status">{capsLockWarning}</span> : null}
    </label>
  );
}

function PasswordVisibilityIcon({ visible }: { visible: boolean }) {
  return visible ? (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M2.7 12s3.4-5.5 9.3-5.5S21.3 12 21.3 12 17.9 17.5 12 17.5 2.7 12 2.7 12Z" />
      <circle cx="12" cy="12" r="2.6" />
    </svg>
  ) : (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="m3 3 18 18" />
      <path d="M10.6 6.7A9.8 9.8 0 0 1 12 6.5c5.9 0 9.3 5.5 9.3 5.5a17 17 0 0 1-2.4 3.1M6.5 6.6C4 8.2 2.7 12 2.7 12s3.4 5.5 9.3 5.5c1.2 0 2.3-.2 3.3-.6" />
      <path d="M9.7 9.7A3.3 3.3 0 0 0 14.3 14.3" />
    </svg>
  );
}
