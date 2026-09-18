import type { ReactNode } from "react";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel: string;
  confirmTone?: "default" | "danger";
  note?: string;
  onCancel: () => void;
  onConfirm: () => void;
  children?: ReactNode;
};

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  cancelLabel,
  confirmTone = "default",
  note,
  onCancel,
  onConfirm,
  children,
}: ConfirmDialogProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="modal-backdrop" role="presentation" onClick={onCancel}>
      <section
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        className="modal-card"
        role="dialog"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-card__header">
          <div>
            <h2 id="confirm-dialog-title">{title}</h2>
            <p>{description}</p>
          </div>
          {/* 標頭只保留無障礙關閉 icon；底部的取消按鈕才是唯一具名的取消操作，避免重複文案。 */}
          <button
            className="modal-card__dismiss"
            type="button"
            onClick={onCancel}
            aria-label={cancelLabel}
          >
            <CloseIcon />
          </button>
        </div>

        {children || note ? (
          <div className="modal-card__body">
            {children}
            {note ? <p className="modal-card__note">{note}</p> : null}
          </div>
        ) : null}

        <div className="modal-card__actions">
          <button className="secondary-button" type="button" onClick={onCancel}>
            {cancelLabel}
          </button>
          <button
            className={`primary-button${confirmTone === "danger" ? " primary-button--danger" : ""}`}
            type="button"
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m6 6 12 12M18 6 6 18" />
    </svg>
  );
}
