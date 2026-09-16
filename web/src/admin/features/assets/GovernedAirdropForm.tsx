import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from 'react';

import { useI18n } from '../../../i18n';
import { createAdminAirdrop, fetchAdminAirdropAssetOptions, type AdminAirdropAssetOption, type AdminAirdropRequest, type AdminAirdropResult } from '../../api/adminAssetsApi';

const INITIAL_FORM: AdminAirdropRequest = {
  targetUserId: '', assetSymbol: '', amount: '', activityId: 'REVERSAL', reason: '',
};

type FormErrors = Partial<Record<keyof AdminAirdropRequest, string>>;
type SelectOption = { value: string; label: string; icon?: 'activity' | 'airdrop' | 'asset' };

/**
 * 資產調整表單僅呈現與收集既有管理命令；雙分錄、冪等與稽核證據仍完全由 server-side 邊界處理。
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
    } else if (!/^-?\d+(?:\.\d+)?$/.test(form.amount)) {
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
        <div className="admin-airdrop-form__header-main">
          <AssetAdjustmentHeroIcon />
          <div><h2>{t('admin.assetsAdjustmentTitle')}</h2><p>{t('admin.assetsAdjustmentSubtitle')}</p></div>
        </div>
        <div className="admin-airdrop-form__hero-aside">
          <AssetAdjustmentIllustration />
          <div><strong>{t('admin.assetsAdjustmentSlogan')}</strong><span>{t('admin.assetsAdjustmentSloganDescription')}</span></div>
        </div>
      </header>
      <form className="admin-airdrop-form__body" noValidate onSubmit={(event) => void submit(event)}>
        <section className="admin-airdrop-form__section" aria-label={t('admin.assetsAirdropType')}>
          <div className="admin-airdrop-form__grid">
            <FieldError error={errors.targetUserId}>
              <label className="field"><span className="field__label">{t('admin.assetsAirdropUserId')}<RequiredMark /></span><input className="input" aria-invalid={Boolean(errors.targetUserId)} required maxLength={64} placeholder={t('admin.assetsAirdropUserIdPlaceholder')} value={form.targetUserId} onChange={(event) => update('targetUserId', event.target.value)} /></label>
            </FieldError>
            <FieldError error={errors.activityId}>
              <label className="field"><span className="field__label">{t('admin.assetsAirdropActivity')}<RequiredMark /></span><FormSelect ariaLabel={t('admin.assetsAirdropActivity')} options={activityOptions} placeholder={t('admin.assetsAirdropRequired')} value={form.activityId} onChange={(value) => update('activityId', value)} /></label>
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
            <button className="primary-button admin-airdrop-form__submit" type="submit" disabled={submitting}><SendIcon />{submitting ? t('admin.assetsAirdropSubmitting') : t('admin.assetsAirdropSubmit')}</button>
          </div>
        </div>
        {result ? <p className="admin-airdrop-form__feedback admin-airdrop-form__feedback--success" role="status">{t('admin.assetsAdjustmentSucceeded', undefined, { journalId: result.ledgerJournalId, replay: result.replayed ? t('admin.assetsAirdropReplay') : t('admin.assetsAirdropNew') })}</p> : null}
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
/**
 * Hero icon 以獨立 SVG 建立完整景深，避免通用線框 icon 被誤認為可點擊的功能按鈕。
 */
function AssetAdjustmentHeroIcon() {
  return <div className="admin-airdrop-form__header-icon" aria-hidden="true">
    <svg viewBox="0 0 84 84" focusable="false">
      <defs>
        <linearGradient id="asset-adjustment-hero-tile" x1="8" y1="5" x2="77" y2="80" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="var(--asset-adjustment-icon-violet)" />
          <stop offset=".5" stopColor="var(--asset-adjustment-icon-indigo)" />
          <stop offset="1" stopColor="var(--asset-adjustment-icon-blue)" />
        </linearGradient>
        <linearGradient id="asset-adjustment-hero-edge" x1="42" y1="3" x2="42" y2="82" gradientUnits="userSpaceOnUse">
          <stop stopColor="var(--asset-adjustment-icon-edge-light)" />
          <stop offset=".58" stopColor="var(--asset-adjustment-icon-edge-mid)" />
          <stop offset="1" stopColor="var(--asset-adjustment-icon-edge-dark)" />
        </linearGradient>
        <linearGradient id="asset-adjustment-hero-glyph" x1="28" y1="26" x2="57" y2="59" gradientUnits="userSpaceOnUse">
          <stop stopColor="var(--asset-adjustment-icon-glyph-start)" />
          <stop offset="1" stopColor="var(--asset-adjustment-icon-glyph-end)" />
        </linearGradient>
        <radialGradient id="asset-adjustment-hero-highlight" cx="0" cy="0" r="1" gradientTransform="translate(25 17) rotate(48) scale(58)" gradientUnits="userSpaceOnUse">
          <stop stopColor="#fff" stopOpacity=".25" />
          <stop offset=".52" stopColor="#fff" stopOpacity=".045" />
          <stop offset="1" stopColor="#fff" stopOpacity="0" />
        </radialGradient>
        <filter id="asset-adjustment-hero-shadow" x="-35%" y="-30%" width="170%" height="185%">
          <feDropShadow dx="0" dy="7" stdDeviation="7" floodColor="var(--asset-adjustment-icon-shadow)" floodOpacity=".28" />
        </filter>
        <filter id="asset-adjustment-hero-glyph-glow" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="1.15" result="glyphBlur" />
          <feMerge><feMergeNode in="glyphBlur" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
      </defs>
      <g filter="url(#asset-adjustment-hero-shadow)">
        <rect x="2" y="4" width="80" height="78" rx="19" fill="var(--asset-adjustment-icon-depth)" opacity=".72" />
        <rect x="2" y="2" width="80" height="78" rx="19" fill="url(#asset-adjustment-hero-tile)" />
        <rect x="2.5" y="2.5" width="79" height="77" rx="18.5" fill="none" stroke="url(#asset-adjustment-hero-edge)" />
        <rect x="3" y="3" width="78" height="76" rx="18" fill="url(#asset-adjustment-hero-highlight)" />
      </g>
      <g fill="none" stroke="url(#asset-adjustment-hero-glyph)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" filter="url(#asset-adjustment-hero-glyph-glow)">
        <path d="M27 33h29m-8-8 8 8-8 8" />
        <path d="M57 51H28m8 8-8-8 8-8" />
      </g>
    </svg>
  </div>;
}
function AssetAdjustmentIllustration() {
  return <svg className="admin-airdrop-form__illustration" viewBox="0 0 260 130" aria-hidden="true">
    <defs>
      <linearGradient id="asset-adjustment-card" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#8d7bff" /><stop offset=".48" stopColor="#5976f2" /><stop offset="1" stopColor="#274db8" /></linearGradient>
      <linearGradient id="asset-adjustment-coin" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#91b8ff" /><stop offset=".48" stopColor="#577ff0" /><stop offset="1" stopColor="#284eb9" /></linearGradient>
      <radialGradient id="asset-adjustment-sphere" cx="30%" cy="22%"><stop offset="0" stopColor="#c8d9ff" /><stop offset=".3" stopColor="#8498ff" /><stop offset="1" stopColor="#4155c4" /></radialGradient>
      <radialGradient id="asset-adjustment-ground"><stop offset="0" stopColor="#456ff0" stopOpacity=".3" /><stop offset="1" stopColor="#233f9e" stopOpacity="0" /></radialGradient>
      <filter id="asset-adjustment-shadow" x="-40%" y="-40%" width="180%" height="200%"><feDropShadow dx="0" dy="8" stdDeviation="7" floodColor="#142c83" floodOpacity=".48" /></filter>
    </defs>
    <ellipse cx="124" cy="108" rx="104" ry="18" fill="url(#asset-adjustment-ground)" />
    <ellipse cx="116" cy="103" rx="87" ry="16" fill="none" stroke="#5683ff" strokeOpacity=".55" strokeWidth="1.5" />
    <g filter="url(#asset-adjustment-shadow)">
      <path d="M46 78v12c0 7 17 13 38 13s38-6 38-13V78Z" fill="#3158c6" />
      <ellipse cx="84" cy="78" rx="38" ry="13" fill="url(#asset-adjustment-coin)" />
      <path d="M50 64v12c0 7 15 12 34 12s34-5 34-12V64Z" fill="#355fce" />
      <ellipse cx="84" cy="64" rx="34" ry="12" fill="url(#asset-adjustment-coin)" />
      <path d="M55 51v11c0 6 13 11 29 11s29-5 29-11V51Z" fill="#3a67d7" />
      <ellipse cx="84" cy="51" rx="29" ry="11" fill="url(#asset-adjustment-coin)" />
      <ellipse cx="77" cy="48" rx="13" ry="4" fill="#c5d7ff" fillOpacity=".26" />
    </g>
    <g transform="rotate(13 161 57)" filter="url(#asset-adjustment-shadow)">
      <rect x="128" y="21" width="76" height="72" rx="17" fill="url(#asset-adjustment-card)" />
      <path d="M145 50h33m-9-9 9 9-9 9M185 65h-33m9-9-9 9 9 9" fill="none" stroke="#d7e5ff" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M140 30c14-8 37-8 52 1" fill="none" stroke="#fff" strokeOpacity=".2" strokeWidth="3" strokeLinecap="round" />
    </g>
    <circle cx="206" cy="29" r="9" fill="url(#asset-adjustment-sphere)" filter="url(#asset-adjustment-shadow)" />
    <circle cx="229" cy="91" r="12" fill="url(#asset-adjustment-sphere)" filter="url(#asset-adjustment-shadow)" />
    <circle cx="34" cy="47" r="5" fill="url(#asset-adjustment-sphere)" opacity=".75" />
  </svg>;
}
function SelectIcon({ kind }: { kind?: SelectOption['icon'] }) { return <svg className="admin-form-select__icon" viewBox="0 0 24 24" aria-hidden="true"><path d={kind === 'airdrop' ? 'm21 3-7.7 18-3.9-7.1L3 10.7 21 3Z' : kind === 'asset' ? 'M12 3 19 7v10l-7 4-7-4V7l7-4ZM5 7l7 4 7-4M12 11v10' : 'M7 7h10M14 3l4 4-4 4M17 17H7M10 21l-4-4 4-4'} /></svg>; }
function ChevronIcon() { return <svg className="admin-form-select__chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 9 5 5 5-5" /></svg>; }
function CheckIcon() { return <svg className="admin-form-select__check" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>; }
function ResetIcon() { return <svg viewBox="0 0 24 24" focusable="false"><path d="M20 11a8 8 0 1 0 2 5.5M20 4v7h-7" /></svg>; }
function SendIcon() { return <svg viewBox="0 0 24 24" focusable="false"><path d="m21 3-7.7 18-3.9-7.1L3 10.7 21 3ZM9.4 13.9 14 10" /></svg>; }
