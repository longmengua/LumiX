import { useEffect, useRef, useState } from 'react';

type CopyButtonProps = {
  value: string;
  label: string;
  copiedLabel: string;
};

/**
 * 複製識別值的共用 icon button。
 *
 * <p>UUID、電子郵件等支援所需識別值不應靠使用者手動選取；此元件統一處理 clipboard 權限與短暫回饋，
 * 讓前後台使用相同且可及的複製行為。</p>
 */
export function CopyButton({ value, label, copiedLabel }: CopyButtonProps) {
  const [copied, setCopied] = useState(false);
  const resetTimer = useRef<number | null>(null);

  useEffect(() => () => {
    if (resetTimer.current !== null) window.clearTimeout(resetTimer.current);
  }, []);

  async function copy() {
    const copiedSuccessfully = await copyToClipboard(value);
    if (!copiedSuccessfully) return;

    setCopied(true);
    if (resetTimer.current !== null) window.clearTimeout(resetTimer.current);
    resetTimer.current = window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <button
      className="copy-button"
      type="button"
      aria-label={copied ? copiedLabel : label}
      title={copied ? copiedLabel : label}
      onClick={() => void copy()}
    >
      {copied ? <CheckIcon /> : <CopyIcon />}
      <span className="sr-only" aria-live="polite">{copied ? copiedLabel : ''}</span>
    </button>
  );
}

/** Clipboard API 在 HTTPS / localhost 可用；舊瀏覽器才退回到同步選取方式，讓識別值複製不受環境差異影響。 */
async function copyToClipboard(value: string) {
  try {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(value);
      return true;
    }
  } catch {
    // Clipboard 權限遭拒時仍嘗試相容 fallback，不把底層例外顯示成技術訊息。
  }

  const textArea = document.createElement('textarea');
  textArea.value = value;
  textArea.setAttribute('readonly', '');
  textArea.style.position = 'fixed';
  textArea.style.opacity = '0';
  document.body.append(textArea);
  textArea.select();
  const copied = document.execCommand('copy');
  textArea.remove();
  return copied;
}

function CopyIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <rect x="9" y="8" width="9" height="10" rx="1.5" />
      <path d="M15 8V6.5A1.5 1.5 0 0 0 13.5 5h-8A1.5 1.5 0 0 0 4 6.5v8A1.5 1.5 0 0 0 5.5 16H9" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="m5.5 12.5 4 4 9-9" />
    </svg>
  );
}
