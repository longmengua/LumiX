import { useRef, useState, type FormEvent } from 'react';

import { Card } from '../../components/base/Card';
import { ErrorState } from '../../components/base/State';
import { AdminPageHero } from '../../admin/components/AdminPageHero';
import { AssetAdjustmentArtwork } from '../../admin/features/assets/AssetAdjustmentHeroArtwork';
import { AssetAdjustmentHeroIcon } from '../../admin/features/assets/AssetAdjustmentHeroIcon';
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
  return <div className="stack assets-page asset-transfer-page">
    <AdminPageHero
      title={t('assets.transferTitle')}
      description={t('assets.transferRuntimeDescription')}
      icon={<AssetAdjustmentHeroIcon />}
      chips={[
        { id: 'transfer', label: t('assets.transferChipTransfer'), icon: <TransferIcon /> },
        { id: 'balance', label: t('assets.transferChipBalance'), icon: <CheckIcon /> },
        { id: 'safe', label: t('assets.transferChipSafe'), icon: <ShieldIcon /> },
      ]}
      illustration={<AssetAdjustmentArtwork className="admin-page-hero__artwork-image" />}
      slogan={t('assets.transferSlogan')}
      supportingText={t('assets.transferSloganDescription')}
    />
    <AssetSectionNav className="asset-transfer-workspace" />
    <Card className="asset-transfer-card"><header className="asset-transfer-card__header"><span className="asset-transfer-card__icon" aria-hidden="true"><TransferIcon /></span><div><h2>{t('assets.transferFormTitle')}</h2><p>{t('assets.transferFormDescription')}</p></div></header><form className="asset-transfer-form" onSubmit={(event) => void submit(event)}>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeFrom')}<RequiredMark /></span><TransferAccountSelect value={form.sourceAccountType} onChange={(value) => update('sourceAccountType', value)} labels={{ spot: t('account.spotAccount'), futures: t('account.futuresAccount') }} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeTo')}<RequiredMark /></span><TransferAccountSelect value={form.destinationAccountType} onChange={(value) => update('destinationAccountType', value)} labels={{ spot: t('account.spotAccount'), futures: t('account.futuresAccount') }} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeAsset')}<RequiredMark /></span><input className="input" required maxLength={32} value={form.assetSymbol} onChange={(event) => update('assetSymbol', event.target.value.toUpperCase())} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeAmount')}<RequiredMark /></span><input className="input" required inputMode="decimal" pattern="[0-9]+([.][0-9]+)?" value={form.amount} onChange={(event) => update('amount', event.target.value)} /></label>
      <div className="asset-transfer-form__actions"><button className="primary-button" disabled={submitting} type="submit"><TransferIcon />{submitting ? t('assets.transferRuntimeSubmitting') : t('assets.transferRuntimeSubmit')}</button></div>
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

function RequiredMark() { return <span className="asset-transfer-form__required" aria-hidden="true">*</span>; }
function ChevronIcon() { return <svg className="admin-form-select__chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5" /></svg>; }
function CheckIcon() { return <svg className="admin-form-select__check" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>; }
function TransferIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h12m-4-4 4 4-4 4M19 17H7m4 4-4-4 4-4" /></svg>; }
function ShieldIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5 6v5c0 4.6 2.9 8.5 7 10 4.1-1.5 7-5.4 7-10V6l-7-3Zm-3.2 9.1 2.1 2.1 4.4-4.4" /></svg>; }
