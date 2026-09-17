import { useRef, useState, type FormEvent } from 'react';

import { Card } from '../../components/base/Card';
import { ErrorState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { useI18n } from '../../i18n';

type TransferForm = { sourceAccountType: string; destinationAccountType: string; assetSymbol: string; amount: string; };
const initialForm: TransferForm = { sourceAccountType: 'SPOT', destinationAccountType: 'FUTURES', assetSymbol: 'USDT', amount: '' };

/** 真實劃轉表單；成功 only 由 server journal response 決定，不能用 local state 假裝資產已移動。 */
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
    <Card title={t('assets.transferTitle')}><form className="admin-assets-airdrop" onSubmit={(event) => void submit(event)}>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeFrom')}</span><select className="input" value={form.sourceAccountType} onChange={(event) => update('sourceAccountType', event.target.value)}><option>SPOT</option><option>FUTURES</option></select></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeTo')}</span><select className="input" value={form.destinationAccountType} onChange={(event) => update('destinationAccountType', event.target.value)}><option>SPOT</option><option>FUTURES</option></select></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeAsset')}</span><input className="input" required maxLength={32} value={form.assetSymbol} onChange={(event) => update('assetSymbol', event.target.value.toUpperCase())} /></label>
      <label className="field"><span className="field__label">{t('assets.transferRuntimeAmount')}</span><input className="input" required inputMode="decimal" pattern="[0-9]+([.][0-9]+)?" value={form.amount} onChange={(event) => update('amount', event.target.value)} /></label>
      <button className="primary-button" disabled={submitting} type="submit">{submitting ? t('assets.transferRuntimeSubmitting') : t('assets.transferRuntimeSubmit')}</button>
    </form>{result ? <p className="form-message form-message--success">{t('assets.transferRuntimeSucceeded', undefined, { journalId: result.ledgerJournalId })}</p> : null}{error ? <ErrorState title={t('assets.transferRuntimeFailed')} description={error} /> : null}</Card>
  </div>;
}
