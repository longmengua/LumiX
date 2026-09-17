import { useState } from 'react';

export type AssetSymbolOption = {
  value: string;
  label: string;
};

type AssetSymbolSelectProps = {
  ariaLabel: string;
  disabled?: boolean;
  options: AssetSymbolOption[];
  value: string;
  onChange: (value: string) => void;
};

/**
 * 資產代號的共用暗色下拉控制項。
 *
 * 只接收呼叫端以真實資料來源組裝的 options，避免 control 自行寫死幣種或建立資產 fallback。
 */
export function AssetSymbolSelect({ ariaLabel, disabled = false, options, value, onChange }: AssetSymbolSelectProps) {
  const [open, setOpen] = useState(false);
  const selected = options.find((option) => option.value === value);

  return <span className={`admin-form-select admin-form-select--compact${open ? ' admin-form-select--open' : ''}`}>
    <button className="admin-form-select__trigger" type="button" disabled={disabled} aria-label={ariaLabel} aria-haspopup="listbox" aria-expanded={open} onClick={() => setOpen((current) => !current)}>
      <AssetIcon /><span>{selected?.label ?? '-'}</span><ChevronIcon />
    </button>
    {open ? <span className="admin-form-select__menu" role="listbox" aria-label={ariaLabel}>
      {options.map((option) => <button className={`admin-form-select__option${option.value === value ? ' admin-form-select__option--selected' : ''}`} key={option.value} role="option" type="button" aria-selected={option.value === value} onClick={() => { onChange(option.value); setOpen(false); }}>
        <AssetIcon /><span>{option.label}</span>{option.value === value ? <CheckIcon /> : null}
      </button>)}
    </span> : null}
  </span>;
}

function AssetIcon() { return <svg className="admin-form-select__icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 19 7v10l-7 4-7-4V7l7-4ZM5 7l7 4 7-4M12 11v10" /></svg>; }
function ChevronIcon() { return <svg className="admin-form-select__chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5" /></svg>; }
function CheckIcon() { return <svg className="admin-form-select__check" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>; }
