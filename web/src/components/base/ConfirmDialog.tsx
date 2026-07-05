import type { ReactNode } from 'react';

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel: string;
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
            <p className="eyebrow">Confirmation</p>
            <h2 id="confirm-dialog-title">{title}</h2>
            <p>{description}</p>
          </div>
          <button className="ghost-button" type="button" onClick={onCancel}>
            {cancelLabel}
          </button>
        </div>

        {children || note ? <div className="modal-card__body">{children}{note ? <p className="modal-card__note">{note}</p> : null}</div> : null}

        <div className="modal-card__actions">
          <button className="secondary-button" type="button" onClick={onCancel}>
            {cancelLabel}
          </button>
          <button className="primary-button" type="button" onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
