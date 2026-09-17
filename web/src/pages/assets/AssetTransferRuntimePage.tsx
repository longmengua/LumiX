import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';

import { Card } from '../../components/base/Card';
import { ErrorState } from '../../components/base/State';
import { AdminPageHero } from '../../admin/components/AdminPageHero';
import { AssetAdjustmentArtwork } from '../../admin/features/assets/AssetAdjustmentHeroArtwork';
import { AssetAdjustmentHeroIcon } from '../../admin/features/assets/AssetAdjustmentHeroIcon';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { AssetSymbolSelect } from '../../features/assets/AssetSymbolSelect';
import { tabForAccountType, type AssetAccountType } from '../../features/assets/assetAccountTypes';
import { useAssetProjectionSnapshot } from '../../features/assets/useAssetProjectionSnapshot';
import { useI18n } from '../../i18n';

type TransferForm = { sourceAccountType: AssetAccountType; destinationAccountType: AssetAccountType; assetSymbol: string; amount: string; };
const initialForm: TransferForm = { sourceAccountType: 'SPOT', destinationAccountType: 'FUTURES', assetSymbol: '', amount: '' };

/** 真實劃轉表單；成功只由 server journal response 決定，不能用 local state 假裝資產已移動。 */
export function AssetTransferRuntimePage() {
  const { t } = useI18n();
  const [form, setForm] = useState<TransferForm>(initialForm);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ ledgerJournalId: string; replayed: boolean } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const idempotencyKey = useRef<string | null>(null);
  const { data, loading: assetsLoading, errorCode: assetLoadError } = useAssetProjectionSnapshot();
  const assetOptions = useMemo(() => (data?.accounts.find((account) => account.key === tabForAccountType(form.sourceAccountType))?.items ?? [])
    .filter((asset) => asset.accountStatus === 'ACTIVE' && asset.assetStatus === 'ACTIVE')
    .filter((asset, index, list) => list.findIndex((candidate) => candidate.assetSymbol === asset.assetSymbol) === index)
    .map((asset) => ({ value: asset.assetSymbol, label: asset.assetDisplayName })), [data, form.sourceAccountType]);
  const sourceAsset = data?.accounts.find((account) => account.key === tabForAccountType(form.sourceAccountType))?.items.find((asset) => asset.assetSymbol === form.assetSymbol);
  const destinationAsset = data?.accounts.find((account) => account.key === tabForAccountType(form.destinationAccountType))?.items.find((asset) => asset.assetSymbol === form.assetSymbol);

  useEffect(() => {
    if (!assetOptions.some((asset) => asset.value === form.assetSymbol)) {
      setForm((current) => ({ ...current, assetSymbol: assetOptions[0]?.value ?? '' }));
    }
  }, [assetOptions, form.assetSymbol]);

  function update<K extends keyof TransferForm>(key: K, value: TransferForm[K]) { idempotencyKey.current = null; setResult(null); setForm((current) => ({ ...current, [key]: value })); }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (form.sourceAccountType === form.destinationAccountType) { setError(t('assets.transferRuntimeSameAccount')); return; }
    if (!form.assetSymbol) { setError(t('assets.transferRuntimeAssetsEmpty')); return; }
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
      <label className="field"><span className="field__label">{t('assets.transferRuntimeFrom')}<RequiredMark /></span><TransferAccountSelect value={form.sourceAccountType} onChange={(value) => update('sourceAccountType', value)} labels={{ spot: t('account.spotAccount'), futures: t('account.futuresAccount') }} /><AccountAmountHint label={t('assets.transferRuntimeSourceAvailable')} amount={sourceAsset?.available} assetSymbol={form.assetSymbol} loading={assetsLoading} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeTo')}<RequiredMark /></span><TransferAccountSelect value={form.destinationAccountType} onChange={(value) => update('destinationAccountType', value)} labels={{ spot: t('account.spotAccount'), futures: t('account.futuresAccount') }} /><AccountAmountHint label={t('assets.transferRuntimeTargetTotal')} amount={destinationAsset?.total} assetSymbol={form.assetSymbol} loading={assetsLoading} /></label>
      <label className="field asset-transfer-form__amount"><span className="field__label">{t('assets.transferRuntimeAmount')}<RequiredMark /></span><span className="asset-transfer-form__amount-row"><input className="input" required inputMode="decimal" pattern="[0-9]+([.][0-9]+)?" placeholder={t('assets.funding.internalAmountPlaceholder')} value={form.amount} onChange={(event) => update('amount', event.target.value)} /><AssetSymbolSelect ariaLabel={t('assets.transferRuntimeAsset')} disabled={assetsLoading || Boolean(assetLoadError) || assetOptions.length === 0} options={assetOptions} value={form.assetSymbol} onChange={(value) => update('assetSymbol', value)} /></span></label>
      <div className="asset-transfer-form__actions"><button className="primary-button" disabled={submitting || assetsLoading || Boolean(assetLoadError) || assetOptions.length === 0} type="submit"><TransferIcon />{submitting ? t('assets.transferRuntimeSubmitting') : t('assets.transferRuntimeSubmit')}</button></div>
    </form>{assetLoadError ? <ErrorState title={t('assets.transferRuntimeAssetsFailed')} description={t('assets.transferRuntimeAssetsFailedDescription')} /> : null}{!assetsLoading && !assetLoadError && assetOptions.length === 0 ? <p className="asset-transfer-form__empty">{t('assets.transferRuntimeAssetsEmpty')}</p> : null}{result ? <p className="form-message form-message--success">{t('assets.transferRuntimeSucceeded', undefined, { journalId: result.ledgerJournalId })}</p> : null}{error ? <ErrorState title={t('assets.transferRuntimeFailed')} description={error} /> : null}</Card>
  </div>;
}

type TransferAccountSelectProps = {
  value: AssetAccountType;
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
function AccountAmountHint({ label, amount, assetSymbol, loading }: { label: string; amount?: string; assetSymbol: string; loading: boolean }) {
  return <span className="asset-transfer-form__balance-hint">{label}<strong>{loading ? '—' : amount && assetSymbol ? `${amount} ${assetSymbol}` : '—'}</strong></span>;
}
function ChevronIcon() { return <svg className="admin-form-select__chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5" /></svg>; }
function CheckIcon() { return <svg className="admin-form-select__check" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>; }
function TransferIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h12m-4-4 4 4-4 4M19 17H7m4 4-4-4 4-4" /></svg>; }
function ShieldIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5 6v5c0 4.6 2.9 8.5 7 10 4.1-1.5 7-5.4 7-10V6l-7-3Zm-3.2 9.1 2.1 2.1 4.4-4.4" /></svg>; }
