import { useEffect, useState } from 'react';

type VerificationMethod = 'email' | 'sms' | 'totp';

type SecurityVerifyModalProps = {
  open: boolean;
  title: string;
  description: string;
  methods?: VerificationMethod[];
  confirmLabel?: string;
  onClose: () => void;
  onConfirm: (input: { method: VerificationMethod; code: string }) => void;
};

const methodLabels: Record<VerificationMethod, string> = {
  email: 'Email code',
  sms: 'SMS code',
  totp: 'Google Authenticator',
};

const defaultMethods: VerificationMethod[] = ['email', 'sms', 'totp'];

export function SecurityVerifyModal({
  open,
  title,
  description,
  methods = defaultMethods,
  confirmLabel = 'Verify',
  onClose,
  onConfirm,
}: SecurityVerifyModalProps) {
  const [method, setMethod] = useState<VerificationMethod>(methods[0] ?? 'email');
  const [code, setCode] = useState('');

  useEffect(() => {
    if (open) {
      setMethod(methods[0] ?? 'email');
      setCode('');
    }
  }, [methods, open]);

  if (!open) {
    return null;
  }

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <section
        aria-modal="true"
        aria-labelledby="security-verify-title"
        className="modal-card"
        role="dialog"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-card__header">
          <div>
            <p className="eyebrow">Security verify</p>
            <h2 id="security-verify-title">{title}</h2>
            <p>{description}</p>
          </div>
          <button className="ghost-button" type="button" onClick={onClose}>
            Close
          </button>
        </div>

        <div className="modal-card__body">
          <label className="field">
            <span className="field__label">Verification method</span>
            <select className="input" value={method} onChange={(event) => setMethod(event.target.value as VerificationMethod)}>
              {methods.map((item) => (
                <option key={item} value={item}>
                  {methodLabels[item]}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span className="field__label">Verification code</span>
            <input
              className="input"
              inputMode="numeric"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              placeholder="123456"
            />
          </label>
        </div>

        <div className="modal-card__actions">
          <button className="secondary-button" type="button" onClick={onClose}>
            Cancel
          </button>
          <button className="primary-button" type="button" onClick={() => onConfirm({ method, code })}>
            {confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}

export type { VerificationMethod };
