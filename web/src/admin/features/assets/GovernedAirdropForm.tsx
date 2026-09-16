import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from 'react';

import { useI18n } from '../../../i18n';
import { createAdminAirdrop, fetchAdminAirdropAssetOptions, type AdminAirdropAssetOption, type AdminAirdropRequest, type AdminAirdropResult } from '../../api/adminAssetsApi';

const INITIAL_FORM: AdminAirdropRequest = {
  targetUserId: '', assetSymbol: '', amount: '', activityId: 'REVERSAL', reason: '',
};

type FormErrors = Partial<Record<keyof AdminAirdropRequest, string>>;
type SelectOption = { value: string; label: string; icon?: 'activity' | 'airdrop' | 'asset' };

/**
 * 受治理空投僅負責呈現與收集既有管理命令；雙分錄、冪等與稽核證據仍完全由 server-side 邊界處理。
 */
export function GovernedAirdropForm() {
  const { t } = useI18n();
  const [form, setForm] = useState<AdminAirdropRequest>(INITIAL_FORM);
  const [assetOptions, setAssetOptions] = useState<AdminAirdropAssetOption[]>([]);
  const [assetsLoading, setAssetsLoading] = useState(true);
  const [assetLoadError, setAssetLoadError] = useState<string | null>(null);
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<AdminAirdropResult | null>(null);
  const [submissionError, setSubmissionError] = useState<string | null>(null);
  const activityOptions: SelectOption[] = [
    { value: 'REVERSAL', label: t('admin.assetsAdjustmentReversal'), icon: 'activity' },
    { value: 'AIRDROP', label: t('admin.assetsAirdropType'), icon: 'airdrop' },
  ];
  const assetSelectOptions: SelectOption[] = assetOptions.map((asset) => ({ value: asset.assetSymbol, label: asset.internalName, icon: 'asset' }));

  useEffect(() => {
    let cancelled = false;
    void fetchAdminAirdropAssetOptions()
      .then((options) => {
        if (cancelled) return;
        setAssetOptions(options);
        setForm((current) => current.assetSymbol ? current : { ...current, assetSymbol: options[0]?.assetSymbol ?? '' });
      })
      .catch((caught) => {
        if (!cancelled) setAssetLoadError(caught instanceof Error ? caught.message : t('admin.assetsAirdropAssetLoadFailed'));
      })
      .finally(() => { if (!cancelled) setAssetsLoading(false); });
    return () => { cancelled = true; };
  }, [t]);

  function update<K extends keyof AdminAirdropRequest>(key: K, value: AdminAirdropRequest[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  }

  /**
   * 這裡只把原生 required 與金額 pattern 明確呈現在欄位下方，不新增任何後端以外的資產業務判斷。
   */
  function validate(): FormErrors {
    const next: FormErrors = {};
    if (!form.targetUserId.trim()) next.targetUserId = t('admin.assetsAirdropRequired');
    if (!form.assetSymbol.trim()) next.assetSymbol = t('admin.assetsAirdropRequired');
    if (!form.amount.trim()) {
      next.amount = t('admin.assetsAirdropRequired');
    } else if (!/^\d+(?:\.\d+)?$/.test(form.amount)) {
      next.amount = t('admin.assetsAirdropAmountInvalid');
    }
    if (!form.reason.trim()) next.reason = t('admin.assetsAirdropRequired');
    return next;
  }

  function reset() {
    setForm({ ...INITIAL_FORM, assetSymbol: assetOptions[0]?.assetSymbol ?? '' });
    setErrors({});
    setResult(null);
    setSubmissionError(null);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    // 異常沖銷必須綁定原始 journal 與 append-only reversal evidence；現有 API 僅能安全執行空投，絕不可借用空投命令處理沖銷。
    if (form.activityId !== 'AIRDROP') {
      setSubmissionError(t('admin.assetsReversalUnavailable'));
      return;
    }
    if (assetsLoading || assetLoadError || assetOptions.length === 0) {
      setSubmissionError(t('admin.assetsAirdropAssetUnavailable'));
      return;
    }

    const nextErrors = validate();
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    setSubmissionError(null);
    setResult(null);
    try {
      setResult(await createAdminAirdrop(form));
    } catch (caught) {
      setSubmissionError(caught instanceof Error ? caught.message : t('admin.assetsAirdropFailed'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="admin-airdrop-form" aria-label={t('admin.assetsAirdropType')}>
      <header className="admin-airdrop-form__header">
        <div className="admin-airdrop-form__header-icon" aria-hidden="true"><AdjustmentIcon /></div>
        <h2>{t('admin.assetsAdjustmentTitle')}</h2>
      </header>
      <form className="admin-airdrop-form__body" noValidate onSubmit={(event) => void submit(event)}>
        <section className="admin-airdrop-form__section" aria-label={t('admin.assetsAirdropType')}>
          <div className="admin-airdrop-form__grid">
            <FieldError error={errors.targetUserId}>
              <label className="field"><span className="field__label">{t('admin.assetsAirdropUserId')}<RequiredMark /></span><input className="input" aria-invalid={Boolean(errors.targetUserId)} required maxLength={64} placeholder={t('admin.assetsAirdropUserIdPlaceholder')} value={form.targetUserId} onChange={(event) => update('targetUserId', event.target.value)} /></label>
            </FieldError>
            <FieldError error={errors.activityId}>
              <label className="field"><span className="field__label">{t('admin.assetsAirdropActivity')}<RequiredMark /></span><FormSelect ariaLabel={t('admin.assetsAirdropActivity')} options={activityOptions} placeholder={t('admin.assetsAirdropRequired')} value={form.activityId} onChange={(value) => update('activityId', value)} /></label>
              {form.activityId === 'REVERSAL' ? <p className="admin-airdrop-form__activity-hint">{t('admin.assetsReversalUnavailable')}</p> : null}
            </FieldError>
            <FieldError className="admin-airdrop-form__field--amount" error={errors.amount ?? errors.assetSymbol ?? assetLoadError ?? undefined}>
              <label className="field"><span className="field__label">{t('admin.assetsAirdropAmount')}<RequiredMark /></span><span className="admin-airdrop-form__amount-row"><input className="input" aria-invalid={Boolean(errors.amount)} required inputMode="decimal" pattern="[0-9]+([.][0-9]+)?" placeholder={t('admin.assetsAirdropAmountPlaceholder')} value={form.amount} onChange={(event) => update('amount', event.target.value)} /><FormSelect ariaLabel={t('admin.assetsAirdropAsset')} compact disabled={assetsLoading || Boolean(assetLoadError) || assetOptions.length === 0} options={assetSelectOptions} placeholder="-" value={form.assetSymbol} onChange={(value) => update('assetSymbol', value)} /></span></label>
            </FieldError>
          </div>
        </section>

        <section className="admin-airdrop-form__section admin-airdrop-form__remarks" aria-labelledby="airdrop-remarks">
          <div className="admin-airdrop-form__remarks-heading">
            <div className="admin-airdrop-form__remarks-icon" aria-hidden="true"><NoteIcon /></div>
            <div><h3 id="airdrop-remarks">{t('admin.assetsAirdropRemarksTitle')}<RequiredMark /></h3><p>{t('admin.assetsAirdropRemarksHint')}</p></div>
          </div>
          <FieldError error={errors.reason}>
            <label className="field"><span className="sr-only">{t('admin.assetsAirdropReason')}</span><span className="admin-airdrop-form__textarea-wrap"><textarea className="input" aria-invalid={Boolean(errors.reason)} required maxLength={256} placeholder={t('admin.assetsAirdropReasonPlaceholder')} value={form.reason} onChange={(event) => update('reason', event.target.value)} /><span className="admin-airdrop-form__counter">{form.reason.length}/256</span></span></label>
          </FieldError>
        </section>

        <div className="admin-airdrop-form__footer admin-airdrop-form__footer--actions-only">
          <div className="admin-airdrop-form__actions">
            <button className="secondary-button admin-airdrop-form__reset" type="button" disabled={submitting} onClick={reset}><ResetIcon />{t('admin.assetsAirdropReset')}</button>
            <button className="primary-button admin-airdrop-form__submit" type="submit" disabled={submitting || form.activityId !== 'AIRDROP'}><SendIcon />{submitting ? t('admin.assetsAirdropSubmitting') : t('admin.assetsAirdropSubmit')}</button>
          </div>
        </div>
        {result ? <p className="admin-airdrop-form__feedback admin-airdrop-form__feedback--success" role="status">{t('admin.assetsAirdropSucceeded', undefined, { journalId: result.ledgerJournalId, replay: result.replayed ? t('admin.assetsAirdropReplay') : t('admin.assetsAirdropNew') })}</p> : null}
        {submissionError ? <p className="admin-airdrop-form__feedback admin-airdrop-form__feedback--error" role="alert">{submissionError}</p> : null}
      </form>
    </section>
  );
}

function FieldError({ children, error, className }: { children: ReactNode; error?: string; className?: string }) {
  return <div className={['admin-airdrop-form__field-wrap', className].filter(Boolean).join(' ')}>{children}{error ? <p className="admin-airdrop-form__field-error" role="alert">{error}</p> : null}</div>;
}

/** 必填標記只補足視覺辨識；原有的 HTML required 與 submit validation 仍是實際驗證來源。 */
function RequiredMark() { return <span className="admin-airdrop-form__required" aria-hidden="true">*</span>; }

/** 輕量選單保留鍵盤操作與選取狀態，避免管理介面退回作業系統原生灰色選單。 */
function FormSelect({ ariaLabel, compact = false, disabled = false, options, placeholder, value, onChange }: { ariaLabel: string; compact?: boolean; disabled?: boolean; options: SelectOption[]; placeholder: string; value: string; onChange: (value: string) => void }) {
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const selected = options.find((option) => option.value === value);
  function onKeyDown(event: KeyboardEvent<HTMLButtonElement>) { if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') { event.preventDefault(); setOpen(true); } }
  return <span className={`admin-form-select${compact ? ' admin-form-select--compact' : ''}${open ? ' admin-form-select--open' : ''}`}><button ref={buttonRef} className="admin-form-select__trigger" type="button" disabled={disabled} aria-label={ariaLabel} aria-haspopup="listbox" aria-expanded={open} onKeyDown={onKeyDown} onClick={() => setOpen((current) => !current)}><SelectIcon kind={selected?.icon} /><span>{selected?.label ?? placeholder}</span><ChevronIcon /></button>{open ? <span className="admin-form-select__menu" role="listbox" aria-label={ariaLabel} onKeyDown={(event) => { if (event.key === 'Escape') { setOpen(false); buttonRef.current?.focus(); } }}>{options.map((option) => <button className={`admin-form-select__option${option.value === value ? ' admin-form-select__option--selected' : ''}`} key={option.value} role="option" type="button" aria-selected={option.value === value} onClick={() => { onChange(option.value); setOpen(false); buttonRef.current?.focus(); }}><SelectIcon kind={option.icon} /><span>{option.label}</span>{option.value === value ? <CheckIcon /> : null}</button>)}</span> : null}</span>;
}

function NoteIcon() { return <svg viewBox="0 0 24 24" focusable="false"><path d="M14 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8M14 4l6 6M14 4v6h6M8 15h8M8 11h3" /></svg>; }
function AdjustmentIcon() { return <svg viewBox="0 0 24 24" focusable="false"><path d="M5 7h10M12 3l4 4-4 4M19 17H9M12 21l-4-4 4-4" /></svg>; }
function SelectIcon({ kind }: { kind?: SelectOption['icon'] }) { return <svg className="admin-form-select__icon" viewBox="0 0 24 24" aria-hidden="true"><path d={kind === 'airdrop' ? 'm21 3-7.7 18-3.9-7.1L3 10.7 21 3Z' : kind === 'asset' ? 'M12 3 19 7v10l-7 4-7-4V7l7-4ZM5 7l7 4 7-4M12 11v10' : 'M7 7h10M14 3l4 4-4 4M17 17H7M10 21l-4-4 4-4'} /></svg>; }
function ChevronIcon() { return <svg className="admin-form-select__chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5" /></svg>; }
function CheckIcon() { return <svg className="admin-form-select__check" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>; }
function ResetIcon() { return <svg viewBox="0 0 24 24" focusable="false"><path d="M20 11a8 8 0 1 0 2 5.5M20 4v7h-7" /></svg>; }
function SendIcon() { return <svg viewBox="0 0 24 24" focusable="false"><path d="m21 3-7.7 18-3.9-7.1L3 10.7 21 3ZM9.4 13.9 14 10" /></svg>; }
