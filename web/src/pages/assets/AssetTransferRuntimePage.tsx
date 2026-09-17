import { useRef, useState, type FormEvent } from 'react';

import { Card } from '../../components/base/Card';
import { ErrorState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { useI18n } from '../../i18n';

type TransferForm = { sourceAccountType: string; destinationAccountType: string; assetSymbol: string; amount: string; };
const initialForm: TransferForm = { sourceAccountType: 'SPOT', destinationAccountType: 'FUTURES', assetSymbol: 'USDT', amount: '' };

/** 真實劃轉表單；成功只由 server journal response 決定，不能用 local state 假裝資產已移動。 */
export function AssetTransferRuntimePage() {
  const { t } = useI18n();
  const [form, setForm] = useState<TransferForm>(initialForm);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ ledgerJournalId: string; replayed: boolean } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const idempotencyKey = useRef<string | null>(null);

  function update<K extends keyof TransferForm>(key: K, value: TransferForm[K]) { idempotencyKey.current = null; setResult(null); setForm((current) => ({ ...current, [key]: value })); }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (form.sourceAccountType === form.destinationAccountType) { setError(t('assets.transferRuntimeSameAccount')); return; }
    idempotencyKey.current ??= crypto.randomUUID();
    setSubmitting(true); setError(null); setResult(null);
    try {
      const response = await fetch('/api/v1/assets/transfers', { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json', 'Idempotency-Key': idempotencyKey.current }, body: JSON.stringify(form) });
      if (!response.ok) throw new Error(t('assets.transferRuntimeFailed'));
      setResult(await response.json() as { ledgerJournalId: string; replayed: boolean });
    } catch (caught) { setError(caught instanceof Error ? caught.message : t('assets.transferRuntimeFailed')); }
    finally { setSubmitting(false); }
  }
  return <div className="stack assets-page"><PageHeader title={t('assets.transferTitle')} description={t('assets.transferRuntimeDescription')} /><AssetSectionNav />
    <Card title={t('assets.transferTitle')}><form className="admin-assets-transfer" onSubmit={(event) => void submit(event)}>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeFrom')}<span className="admin-assets-transfer__required" aria-hidden="true">*</span></span><TransferAccountSelect value={form.sourceAccountType} onChange={(value) => update('sourceAccountType', value)} labels={{ spot: t('account.spotAccount'), futures: t('account.futuresAccount') }} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeTo')}<span className="admin-assets-transfer__required" aria-hidden="true">*</span></span><TransferAccountSelect value={form.destinationAccountType} onChange={(value) => update('destinationAccountType', value)} labels={{ spot: t('account.spotAccount'), futures: t('account.futuresAccount') }} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeAsset')}<span className="admin-assets-transfer__required" aria-hidden="true">*</span></span><input className="input" required maxLength={32} value={form.assetSymbol} onChange={(event) => update('assetSymbol', event.target.value.toUpperCase())} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeAmount')}<span className="admin-assets-transfer__required" aria-hidden="true">*</span></span><input className="input" required inputMode="decimal" pattern="[0-9]+([.][0-9]+)?" value={form.amount} onChange={(event) => update('amount', event.target.value)} /></label>
      <button className="primary-button" disabled={submitting} type="submit">{submitting ? t('assets.transferRuntimeSubmitting') : t('assets.transferRuntimeSubmit')}</button>
    </form>{result ? <p className="form-message form-message--success">{t('assets.transferRuntimeSucceeded', undefined, { journalId: result.ledgerJournalId })}</p> : null}{error ? <ErrorState title={t('assets.transferRuntimeFailed')} description={error} /> : null}</Card>
  </div>;
}

type TransferAccountSelectProps = {
  value: string;
  onChange: (value: 'SPOT' | 'FUTURES') => void;
  labels: { spot: string; futures: string };
};

function TransferAccountSelect({ value, onChange, labels }: TransferAccountSelectProps) {
  const [open, setOpen] = useState(false);
  const options = [{ value: 'SPOT' as const, label: labels.spot }, { value: 'FUTURES' as const, label: labels.futures }];
  const selected = options.find((option) => option.value === value) ?? options[0];
  return <span className={`admin-form-select${open ? ' admin-form-select--open' : ''}`}>
    <button className="admin-form-select__trigger" type="button" aria-haspopup="listbox" aria-expanded={open} onClick={() => setOpen((current) => !current)}>
      <span>{selected.label}</span><ChevronIcon />
    </button>
    {open ? <span className="admin-form-select__menu" role="listbox">
      {options.map((option) => <button className={`admin-form-select__option${option.value === value ? ' admin-form-select__option--selected' : ''}`} key={option.value} role="option" aria-selected={option.value === value} type="button" onClick={() => { onChange(option.value); setOpen(false); }}>
        <span>{option.label}</span>{option.value === value ? <CheckIcon /> : null}
      </button>)}
    </span> : null}
  </span>;
}

function ChevronIcon() { return <svg className="admin-form-select__chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5" /></svg>; }
function CheckIcon() { return <svg className="admin-form-select__check" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>; }
