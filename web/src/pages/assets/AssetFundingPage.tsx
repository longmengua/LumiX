import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { QRCodeSVG } from 'qrcode.react';

import { AdminPageHero } from '../../admin/components/AdminPageHero';
import { AssetAdjustmentArtwork } from '../../admin/features/assets/AssetAdjustmentHeroArtwork';
import { AssetAdjustmentHeroIcon } from '../../admin/features/assets/AssetAdjustmentHeroIcon';
import { Card } from '../../components/base/Card';
import { CopyButton } from '../../components/base/CopyButton';
import { ErrorState } from '../../components/base/State';
import { fetchAccountProfile, type AccountProfileRecord } from '../../features/account/accountApi';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { AssetSymbolSelect } from '../../features/assets/AssetSymbolSelect';
import { useAssetProjectionSnapshot } from '../../features/assets/useAssetProjectionSnapshot';
import { useI18n } from '../../i18n';

type BarcodeDetectorResult = { rawValue?: string };
type BarcodeDetectorInstance = { detect: (source: HTMLVideoElement) => Promise<BarcodeDetectorResult[]> };
type BarcodeDetectorConstructor = new (options: { formats: string[] }) => BarcodeDetectorInstance;

declare global {
  interface Window { BarcodeDetector?: BarcodeDetectorConstructor; }
}

type FundingMode = 'deposit' | 'withdraw';
type FundingMethod = 'chain' | 'otc' | 'bank' | 'internal';

// 平台內部轉入／轉出是同一產品內最直接的資產方式，固定優先顯示；未接入的「其他」通道不建立無法操作的按鈕。
const methods: FundingMethod[] = ['internal', 'chain', 'otc', 'bank'];

/**
 * 充值／提幣方式入口。
 *
 * <p>頁面只建立使用者可理解的方式資訊架構；目前沒有已核准的鏈上、OTC、銀行或平台間轉帳 provider runtime，
 * 因此不產生地址、訂單、餘額變動或任何看似成功的送出結果。</p>
 */
export function AssetFundingPage({ mode }: { mode: FundingMode }) {
  const { t } = useI18n();
  const [selectedMethod, setSelectedMethod] = useState<FundingMethod>('internal');
  const isDeposit = mode === 'deposit';
  const methodPrefix = `assets.funding.${mode}.method.${selectedMethod}`;

  return (
    <div className="stack assets-page asset-funding-page">
      <AdminPageHero
        title={t(isDeposit ? 'assets.depositTitle' : 'assets.withdrawTitle')}
        description={t(isDeposit ? 'assets.depositDescription' : 'assets.withdrawDescription')}
        icon={<AssetAdjustmentHeroIcon />}
        chips={methods.slice(0, 3).map((method) => ({
          id: method,
          label: t(`assets.funding.${mode}.method.${method}`),
          icon: <FundingMethodIcon method={method} />,
        }))}
        illustration={<AssetAdjustmentArtwork className="admin-page-hero__artwork-image" />}
        slogan={t(isDeposit ? 'assets.funding.depositSlogan' : 'assets.funding.withdrawSlogan')}
        supportingText={t('assets.funding.supportingText')}
      />

      <AssetSectionNav className="asset-funding-workspace" />

      <Card className="asset-funding-card">
        <header className="asset-funding-card__header">
          <span className="asset-funding-card__icon" aria-hidden="true"><FundingIcon mode={mode} /></span>
          <div>
            <h2>{t(isDeposit ? 'assets.funding.depositMethodsTitle' : 'assets.funding.withdrawMethodsTitle')}</h2>
            <p>{t('assets.funding.methodsDescription')}</p>
          </div>
        </header>

        <div className="asset-funding-methods" aria-label={t('assets.funding.methodsDescription')}>
          {methods.map((method) => (
            <button
              className={`asset-funding-method${selectedMethod === method ? ' asset-funding-method--selected' : ''}`}
              key={method}
              type="button"
              aria-pressed={selectedMethod === method}
              onClick={() => setSelectedMethod(method)}
            >
              <span className="asset-funding-method__icon" aria-hidden="true"><FundingMethodIcon method={method} /></span>
              <span>{t(`assets.funding.${mode}.method.${method}`)}</span>
              <ChevronIcon />
            </button>
          ))}
        </div>

        {selectedMethod === 'internal'
          ? isDeposit ? <InternalDepositPanel /> : <InternalWithdrawalPanel />
          : <UnavailableMethodDetail icon={<FundingMethodIcon method={selectedMethod} />} title={t(methodPrefix)} description={t(`${methodPrefix}.description`)} />}
      </Card>
    </div>
  );
}

/** 內部轉入以登入者 UUID 為唯一收款識別；QR 不承載私鑰、地址或任何鏈上資料。 */
function InternalDepositPanel() {
  const { t } = useI18n();
  const [profile, setProfile] = useState<AccountProfileRecord | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    void fetchAccountProfile()
      .then((value) => { if (active) setProfile(value); })
      .catch(() => { if (active) setError('ACCOUNT_PROFILE_REQUEST_FAILED'); });
    return () => { active = false; };
  }, []);

  if (error) return <ErrorState title={t('assets.funding.internalReceiveFailed')} description={t('assets.funding.internalReceiveFailedDescription')} />;
  if (!profile) return <section className="asset-funding-method-detail" aria-live="polite"><span className="asset-funding-method-detail__icon" aria-hidden="true"><FundingMethodIcon method="internal" /></span><div><h3>{t('assets.funding.deposit.method.internal')}</h3><p>{t('common.loading')}</p></div></section>;

  return <section className="asset-funding-internal asset-funding-internal--deposit" aria-labelledby="internal-deposit-title">
    <div className="asset-funding-internal__copy">
      <span className="asset-funding-method-detail__icon" aria-hidden="true"><FundingMethodIcon method="internal" /></span>
      <div><h3 id="internal-deposit-title">{t('assets.funding.deposit.method.internal')}</h3><p>{t('assets.funding.internalReceiveDescription')}</p></div>
    </div>
    <div className="asset-funding-qr">
      <QRCodeSVG value={profile.userId} size={176} level="M" includeMargin aria-label={t('assets.funding.internalQrLabel')} />
    </div>
    <div className="asset-funding-identifier">
      <span>{t('assets.funding.internalRecipientId')}</span>
      <strong>{profile.userId}</strong>
      <CopyButton value={profile.userId} label={t('account.copyId')} copiedLabel={t('account.copied')} />
    </div>
  </section>;
}

/** 平台內部轉出固定由登入者現貨帳戶出帳；可選幣種僅取自真實現貨 projection。 */
function InternalWithdrawalPanel() {
  const { t } = useI18n();
  const { data, loading, errorCode, reload } = useAssetProjectionSnapshot();
  const [recipientUserId, setRecipientUserId] = useState('');
  const [assetSymbol, setAssetSymbol] = useState('');
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [scannerOpen, setScannerOpen] = useState(false);
  const [result, setResult] = useState<{ ledgerJournalId: string; replayed: boolean } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const idempotencyKey = useRef<string | null>(null);
  const assets = useMemo(() => (data?.accounts.find((account) => account.key === 'spot')?.items ?? [])
    .filter((asset) => asset.accountStatus === 'ACTIVE' && asset.assetStatus === 'ACTIVE')
    .filter((asset, index, list) => list.findIndex((candidate) => candidate.assetSymbol === asset.assetSymbol) === index), [data]);
  const assetOptions = assets.map((asset) => ({ value: asset.assetSymbol, label: asset.assetDisplayName }));

  useEffect(() => {
    if (!assetOptions.some((asset) => asset.value === assetSymbol)) setAssetSymbol(assetOptions[0]?.value ?? '');
  }, [assetOptions, assetSymbol]);

  function updateRecipient(value: string) { idempotencyKey.current = null; setResult(null); setRecipientUserId(value); }
  function updateAmount(value: string) { idempotencyKey.current = null; setResult(null); setAmount(value); }
  function updateAsset(value: string) { idempotencyKey.current = null; setResult(null); setAssetSymbol(value); }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!recipientUserId.trim() || !assetSymbol || !amount.trim()) return;
    idempotencyKey.current ??= crypto.randomUUID();
    setSubmitting(true); setError(null); setResult(null);
    try {
      const response = await fetch('/api/v1/assets/internal-transfers', {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'Idempotency-Key': idempotencyKey.current },
        body: JSON.stringify({ recipientUserId: recipientUserId.trim(), assetSymbol, amount }),
      });
      if (!response.ok) throw new Error('INTERNAL_TRANSFER_REQUEST_FAILED');
      const body: unknown = await response.json();
      if (!isTransferResponse(body)) throw new Error('INTERNAL_TRANSFER_CONTRACT_ERROR');
      setResult(body); reload();
    } catch {
      setError(t('assets.funding.internalTransferFailed'));
    } finally {
      setSubmitting(false);
    }
  }

  return <section className="asset-funding-internal asset-funding-internal--withdraw" aria-labelledby="internal-withdraw-title">
    <div className="asset-funding-internal__copy"><span className="asset-funding-method-detail__icon" aria-hidden="true"><FundingMethodIcon method="internal" /></span><div><h3 id="internal-withdraw-title">{t('assets.funding.withdraw.method.internal')}</h3><p>{t('assets.funding.internalTransferDescription')}</p></div></div>
    <form className="asset-funding-transfer-form" onSubmit={(event) => void submit(event)}>
      <label className="field asset-funding-transfer-form__recipient"><span className="field__label asset-funding-transfer-form__label-row"><span>{t('assets.funding.internalRecipientId')}<RequiredMark /></span><button className="asset-funding-transfer-form__scan-button" type="button" aria-label={t('assets.funding.internalScanLabel')} title={t('assets.funding.internalScanLabel')} onClick={() => setScannerOpen(true)}><ScanIcon /></button></span><input className="input" required maxLength={64} autoComplete="off" placeholder={t('assets.funding.internalRecipientPlaceholder')} value={recipientUserId} onChange={(event) => updateRecipient(event.target.value)} /></label>
      <label className="field asset-funding-transfer-form__amount"><span className="field__label">{t('assets.transferRuntimeAmount')}<RequiredMark /></span><span className="asset-funding-transfer-form__amount-row"><input className="input" required inputMode="decimal" pattern="[0-9]+([.][0-9]+)?" placeholder={t('assets.funding.internalAmountPlaceholder')} value={amount} onChange={(event) => updateAmount(event.target.value)} /><AssetSymbolSelect ariaLabel={t('assets.transferRuntimeAsset')} disabled={loading || Boolean(errorCode) || assetOptions.length === 0} options={assetOptions} value={assetSymbol} onChange={updateAsset} /></span></label>
      <div className="asset-funding-transfer-form__actions"><button className="primary-button" disabled={submitting || loading || Boolean(errorCode) || assetOptions.length === 0} type="submit"><FundingMethodIcon method="internal" />{submitting ? t('assets.funding.internalTransferSubmitting') : t('assets.funding.internalTransferSubmit')}</button></div>
    </form>
    {errorCode ? <><ErrorState title={t('assets.funding.internalAssetsFailed')} description={t('assets.funding.internalAssetsFailedDescription')} /><button className="secondary-button asset-funding-transfer-form__retry" type="button" onClick={reload}>{t('common.retry')}</button></> : null}
    {!loading && !errorCode && assetOptions.length === 0 ? <p className="asset-funding-transfer-form__empty">{t('assets.funding.internalAssetsEmpty')}</p> : null}
    {result ? <p className="form-message form-message--success">{t('assets.funding.internalTransferSucceeded', undefined, { journalId: result.ledgerJournalId })}</p> : null}
    {error ? <ErrorState title={t('assets.funding.internalTransferFailed')} description={error} /> : null}
    {scannerOpen ? <RecipientQrScanner onClose={() => setScannerOpen(false)} onDetected={(userId) => { updateRecipient(userId); setScannerOpen(false); }} /> : null}
  </section>;
}

/**
 * 只掃描瀏覽器原生 BarcodeDetector 支援的 QR Code，取得 UUID 後立即停止 camera stream。
 * 不支援或未授權時維持 fail-closed，使用者仍可手動輸入而不會帶入任何猜測值。
 */
function RecipientQrScanner({ onClose, onDetected }: { onClose: () => void; onDetected: (userId: string) => void }) {
  const { t } = useI18n();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [status, setStatus] = useState<'starting' | 'ready' | 'unavailable' | 'permission-error' | 'invalid-code'>('starting');

  useEffect(() => {
    let active = true;
    let frameId: number | null = null;
    let stream: MediaStream | null = null;
    const Detector = window.BarcodeDetector;
    if (!Detector || !navigator.mediaDevices?.getUserMedia) {
      setStatus('unavailable');
      return undefined;
    }

    const stop = () => {
      if (frameId !== null) cancelAnimationFrame(frameId);
      stream?.getTracks().forEach((track) => track.stop());
    };
    const start = async () => {
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false });
        const video = videoRef.current;
        if (!active || !video) { stop(); return; }
        video.srcObject = stream;
        await video.play();
        if (!active) { stop(); return; }
        const detector = new Detector({ formats: ['qr_code'] });
        setStatus('ready');
        const scan = async () => {
          if (!active || !videoRef.current) return;
          try {
            const value = (await detector.detect(videoRef.current)).find((code) => typeof code.rawValue === 'string')?.rawValue;
            if (value) {
              const userId = normalizeRecipientUuid(value);
              if (userId) { stop(); onDetected(userId); return; }
              setStatus('invalid-code');
            }
          } catch {
            // 單一辨識 frame 失敗不代表相機失效，持續下一個 frame 才能避免誤判中斷掃描。
          }
          frameId = requestAnimationFrame(() => { void scan(); });
        };
        void scan();
      } catch {
        if (active) setStatus('permission-error');
      }
    };
    void start();
    return () => { active = false; stop(); };
  }, [onDetected]);

  const message = status === 'unavailable' ? t('assets.funding.internalScanUnavailable')
    : status === 'permission-error' ? t('assets.funding.internalScanPermissionError')
      : status === 'invalid-code' ? t('assets.funding.internalScanInvalid') : t('assets.funding.internalScanHint');
  return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="modal-card asset-funding-scanner" role="dialog" aria-modal="true" aria-labelledby="internal-qr-scanner-title" onMouseDown={(event) => event.stopPropagation()}>
      <div className="modal-card__header"><div><h2 id="internal-qr-scanner-title">{t('assets.funding.internalScanTitle')}</h2><p>{message}</p></div><button className="asset-funding-scanner__close" type="button" aria-label={t('common.close')} onClick={onClose}><CloseIcon /></button></div>
      <div className="modal-card__body"><div className="asset-funding-scanner__video"><video ref={videoRef} autoPlay muted playsInline aria-label={t('assets.funding.internalScanPreview')} /></div></div>
    </section>
  </div>;
}

function normalizeRecipientUuid(value: string) {
  const candidate = value.trim();
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(candidate) ? candidate : null;
}

function UnavailableMethodDetail({ icon, title, description }: { icon: ReactNode; title: string; description: string }) {
  const { t } = useI18n();
  return <section className="asset-funding-method-detail" aria-live="polite"><span className="asset-funding-method-detail__icon" aria-hidden="true">{icon}</span><div><h3>{title}</h3><p>{description}</p><span className="asset-funding-method-detail__status">{t('assets.funding.methodNotAvailable')}</span></div></section>;
}

function isTransferResponse(value: unknown): value is { ledgerJournalId: string; replayed: boolean } {
  return typeof value === 'object' && value !== null && typeof (value as { ledgerJournalId?: unknown }).ledgerJournalId === 'string' && typeof (value as { replayed?: unknown }).replayed === 'boolean';
}

function RequiredMark() { return <span className="asset-funding-transfer-form__required" aria-hidden="true">*</span>; }
function ScanIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 8V5h3m8 0h3v3M19 16v3h-3M8 19H5v-3M8 12h8M12 8v8" /></svg>; }
function CloseIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>; }

function FundingIcon({ mode }: { mode: FundingMode }) {
  return mode === 'deposit'
    ? <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4v11m0-11-4 4m4-4 4 4M5 14.5v3A2.5 2.5 0 0 0 7.5 20h9a2.5 2.5 0 0 0 2.5-2.5v-3" /></svg>
    : <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20V9m0 11-4-4m4 4 4-4M5 9.5v-3A2.5 2.5 0 0 1 7.5 4h9A2.5 2.5 0 0 1 19 6.5v3" /></svg>;
}

function FundingMethodIcon({ method }: { method: FundingMethod }) {
  if (method === 'chain') return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="7" cy="12" r="3" /><circle cx="17" cy="7" r="3" /><circle cx="17" cy="17" r="3" /><path d="m9.7 10.7 4.6-2.4m0 7.4-4.6-2.4" /></svg>;
  if (method === 'otc') return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h14v11H5zM8 4v3m8-3v3M8 12h3m2 0h3m-8 3h8" /></svg>;
  if (method === 'bank') return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 9 8-5 8 5M5.5 10.5h13M6.5 10.5v7m4-7v7m4-7v7m3-7v7M4 20h16" /></svg>;
  if (method === 'internal') return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 8h11m-4-4 4 4-4 4M19 16H8m4 4-4-4 4-4" /></svg>;
  return null;
}

function ChevronIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>;
}
