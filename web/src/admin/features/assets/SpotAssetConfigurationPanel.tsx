import { useEffect, useState, type FormEvent } from 'react';

import { useI18n } from '../../../i18n';
import {
  createAdminSpotAssetConfiguration,
  fetchAdminSpotAssetConfigurations,
  updateAdminSpotAssetConfigurationStatus,
  type AdminSpotAssetConfiguration,
} from '../../api/adminAssetsApi';

type CreateForm = { assetSymbol: string; internalName: string; precisionScale: string; };
const EMPTY_FORM: CreateForm = { assetSymbol: '', internalName: '', precisionScale: '6' };

/**
 * 現貨幣種設定只呈現後端主檔，所有建立與狀態變更皆回送 API；不以本地假資料模擬幣種可用性。
 */
export function SpotAssetConfigurationPanel() {
  const { t } = useI18n();
  const [assets, setAssets] = useState<AdminSpotAssetConfiguration[]>([]);
  const [form, setForm] = useState<CreateForm>(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [updatingSymbol, setUpdatingSymbol] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      setAssets(await fetchAdminSpotAssetConfigurations());
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : t('admin.spotAssetConfigLoadFailed'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void refresh(); }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const created = await createAdminSpotAssetConfiguration({
        assetSymbol: form.assetSymbol.trim().toUpperCase(), internalName: form.internalName.trim(), precisionScale: Number(form.precisionScale),
      });
      setAssets((current) => [...current, created].sort((left, right) => left.assetSymbol.localeCompare(right.assetSymbol)));
      setForm(EMPTY_FORM);
      setMessage(t('admin.spotAssetConfigCreated', undefined, { assetSymbol: created.assetSymbol }));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : t('admin.spotAssetConfigSaveFailed'));
    } finally {
      setSubmitting(false);
    }
  }

  async function changeStatus(asset: AdminSpotAssetConfiguration) {
    if (updatingSymbol) return;
    const nextStatus = asset.status === 'ACTIVE' ? 'HALTED' : 'ACTIVE';
    setUpdatingSymbol(asset.assetSymbol);
    setError(null);
    setMessage(null);
    try {
      const updated = await updateAdminSpotAssetConfigurationStatus(asset.assetSymbol, nextStatus);
      setAssets((current) => current.map((value) => value.assetSymbol === updated.assetSymbol ? updated : value));
      setMessage(t('admin.spotAssetConfigStatusSaved', undefined, { assetSymbol: updated.assetSymbol }));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : t('admin.spotAssetConfigSaveFailed'));
    } finally {
      setUpdatingSymbol(null);
    }
  }

  return <div className="admin-spot-asset-config stack">
    <section className="card">
      <h2 className="card__title">{t('admin.spotAssetConfigTitle')}</h2>
      <p className="assets-metric__hint">{t('admin.spotAssetConfigDescription')}</p>
      <form className="admin-spot-asset-config__form" onSubmit={(event) => void submit(event)}>
        <label className="field"><span className="field__label">{t('admin.spotAssetConfigSymbol')}</span><input className="input" required maxLength={32} value={form.assetSymbol} placeholder="USDT" onChange={(event) => setForm((current) => ({ ...current, assetSymbol: event.target.value.toUpperCase() }))} /></label>
        <label className="field"><span className="field__label">{t('admin.spotAssetConfigInternalName')}</span><input className="input" required maxLength={128} value={form.internalName} placeholder="USDT" onChange={(event) => setForm((current) => ({ ...current, internalName: event.target.value }))} /></label>
        <label className="field"><span className="field__label">{t('admin.spotAssetConfigPrecision')}</span><input className="input" required type="number" min="0" max="18" value={form.precisionScale} onChange={(event) => setForm((current) => ({ ...current, precisionScale: event.target.value }))} /></label>
        <button className="primary-button" type="submit" disabled={submitting}>{submitting ? t('admin.spotAssetConfigSaving') : t('admin.spotAssetConfigCreate')}</button>
      </form>
      <p className="admin-spot-asset-config__notice">{t('admin.spotAssetConfigNotice')}</p>
      {message ? <p className="form-message form-message--success" role="status">{message}</p> : null}
      {error ? <p className="form-message form-message--error" role="alert">{error}</p> : null}
    </section>
    <section className="card">
      <div className="admin-spot-asset-config__list-header"><div><h2 className="card__title">{t('admin.spotAssetConfigListTitle')}</h2><p className="assets-metric__hint">{t('admin.spotAssetConfigListHint')}</p></div><button className="admin-spot-asset-config__refresh" type="button" disabled={loading} aria-label={t('common.retry')} title={t('common.retry')} onClick={() => void refresh()}><RefreshIcon /></button></div>
      {loading ? <p className="assets-metric__hint">{t('admin.spotAssetConfigLoading')}</p> : null}
      {!loading && assets.length === 0 ? <p className="assets-metric__hint">{t('admin.spotAssetConfigEmpty')}</p> : null}
      {!loading && assets.length > 0 ? <div className="admin-spot-asset-config__list">{assets.map((asset) => <article className="admin-spot-asset-config__item" key={asset.assetSymbol}><div><strong>{asset.internalName}</strong><small>{t('admin.spotAssetConfigPrecisionValue', undefined, { precisionScale: asset.precisionScale })}</small></div><div className="admin-spot-asset-config__actions"><span className={`admin-spot-asset-config__status admin-spot-asset-config__status--${asset.status.toLowerCase()}`}>{t(`admin.spotAssetConfigStatus.${asset.status}`)}</span>{asset.status !== 'DELISTED' ? <button className="secondary-button" type="button" disabled={updatingSymbol !== null} onClick={() => void changeStatus(asset)}>{updatingSymbol === asset.assetSymbol ? t('admin.spotAssetConfigSaving') : asset.status === 'ACTIVE' ? t('admin.spotAssetConfigHalt') : t('admin.spotAssetConfigActivate')}</button> : null}</div></article>)}</div> : null}
    </section>
  </div>;
}

/** 重新讀取只更新唯讀設定快照，採低調 icon control 避免與新增幣種的主要操作競爭。 */
function RefreshIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 11a8 8 0 1 0 2 5.5M20 4v7h-7" /></svg>;
}
