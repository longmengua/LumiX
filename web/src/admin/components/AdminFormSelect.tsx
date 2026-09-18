import { useRef, useState, type KeyboardEvent } from "react";

type Option = { value: string; label: string };

type AdminFormSelectProps = {
  ariaLabel: string;
  options: Option[];
  value: string;
  destructive?: boolean;
  onChange(value: string): void;
};

/** 共用後台下拉，集中深色選單、ARIA 與 Escape 關閉／焦點返回行為。 */
export function AdminFormSelect({
  ariaLabel,
  options,
  value,
  destructive = false,
  onChange,
}: AdminFormSelectProps) {
  const [open, setOpen] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  const selected = options.find((option) => option.value === value);
  function close() {
    setOpen(false);
    trigger.current?.focus();
  }
  function onKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (["ArrowDown", "Enter", " "].includes(event.key)) {
      event.preventDefault();
      setOpen(true);
    }
    if (event.key === "Escape") close();
  }
  return (
    <span
      className={`admin-form-select${open ? " admin-form-select--open" : ""}${destructive ? " admin-form-select--destructive" : ""}`}
    >
      <button
        ref={trigger}
        className="admin-form-select__trigger"
        type="button"
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        onKeyDown={onKeyDown}
      >
        <span>{selected?.label ?? "-"}</span>
        <Chevron />
      </button>
      {open ? (
        <span
          className="admin-form-select__menu"
          role="listbox"
          aria-label={ariaLabel}
        >
          {options.map((option) => (
            <button
              className={`admin-form-select__option${option.value === value ? " admin-form-select__option--selected" : ""}`}
              key={option.value}
              role="option"
              type="button"
              aria-selected={option.value === value}
              onClick={() => {
                onChange(option.value);
                close();
              }}
            >
              <span>{option.label}</span>
              {option.value === value ? <Check /> : null}
            </button>
          ))}
        </span>
      ) : null}
    </span>
  );
}
function Chevron() {
  return (
    <svg
      className="admin-form-select__chevron"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path d="m7 9 5 5 5-5" />
    </svg>
  );
}
function Check() {
  return (
    <svg
      className="admin-form-select__check"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path d="m5 12 4 4L19 6" />
    </svg>
  );
}
