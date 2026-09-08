import { useState } from 'react';

type PasswordFieldProps = {
  label: string;
  value: string;
  onChange(value: string): void;
  autoComplete: 'current-password' | 'new-password';
  placeholder?: string;
  name?: string;
};

/**
 * 可切換遮罩的密碼欄位。
 *
 * 密碼仍只留在受控表單 state；切換按鈕只改變瀏覽器呈現方式，不會複製、記錄或持久化密碼。
 */
export function PasswordField({ label, value, onChange, autoComplete, placeholder, name }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <label className="field">
      <span className="field__label">{label}</span>
      <span className="password-field">
        <input
          className="input password-field__input"
          name={name}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
        />
        <button
          className="password-field__toggle"
          type="button"
          aria-label={visible ? `隱藏${label}` : `顯示${label}`}
          aria-pressed={visible}
          onClick={() => setVisible((current) => !current)}
        >
          {visible ? '隱藏' : '顯示'}
        </button>
      </span>
    </label>
  );
}
