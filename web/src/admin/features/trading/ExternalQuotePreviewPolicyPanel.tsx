import { useEffect, useState, type FormEvent } from 'react';
import { useI18n } from '../../../i18n';
import { ADMIN_API_BASE_PATH } from '../../api/adminApi';
import { createAdminApiError, notifyAdminUnauthorized } from '../../api/adminError';

type Policy = { version: number; source: 'BINANCE'; platformRate: string; platformFixedUsdt: string;
  assetAdjustments: Record<string, string>; assetFixedAdjustmentsUsdt: Record<string, string> };
type Row = { asset: string; rate: string; fixed: string };
const path = `${ADMIN_API_BASE_PATH}/trading/external-quotes`;

function rowsOf(policy: Policy | null): Row[] {
  const assets = new Set(['BTC', 'ETH', ...Object.keys(policy?.assetAdjustments ?? {}),
    ...Object.keys(policy?.assetFixedAdjustmentsUsdt ?? {})]);
  return [...assets].sort().map(asset => ({ asset, rate: policy?.assetAdjustments[asset] ?? '',
    fixed: policy?.assetFixedAdjustmentsUsdt[asset] ?? '' }));
}

/** 後台保存純報價預覽設定；沒有交易憑證時不得把政策提交成功稱為 Binance 外部送單啟用。 */
export function ExternalQuotePreviewPolicyPanel() {
  const { locale } = useI18n();
  const english = locale === 'en-US';
  const text = (zh: string, en: string) => english ? en : zh;
  const [current, setCurrent] = useState<Policy | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [baseRate, setBaseRate] = useState('');
  const [baseFixed, setBaseFixed] = useState('');
  const [rows, setRows] = useState<Row[]>(() => rowsOf(null));
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    void fetch(`${path}/current`, { credentials: 'same-origin', cache: 'no-store' }).then(async response => {
      if (response.status === 404) return null;
      if (!response.ok) throw await createAdminApiError(response);
      return response.json() as Promise<Policy>;
    }).then(policy => {
      if (!mounted) return;
      setCurrent(policy);
      setBaseRate(policy?.platformRate ?? '');
      setBaseFixed(policy?.platformFixedUsdt ?? '');
      setRows(rowsOf(policy));
      setLoaded(true);
    }).catch(error => {
      if (!mounted) return;
      if (error?.kind === 'unauthorized') notifyAdminUnauthorized();
      setMessage(text('無法取得報價政策。', 'Could not load quote policy.'));
    });
    return () => { mounted = false; };
  }, [locale]);

  function edit(index: number, field: keyof Row, value: string) {
    setRows(previous => previous.map((row, position) => position === index
      ? { ...row, [field]: field === 'asset' ? value.toUpperCase() : value } : row));
  }

  async function publish(event: FormEvent) {
    event.preventDefault();
    if (busy || !loaded) return;
    const assets = rows.map(row => row.asset.trim().toUpperCase());
    if (new Set(assets).size !== assets.length || assets.some(asset => !/^[A-Z0-9]{2,16}$/.test(asset))) {
      setMessage(text('幣別不能重複，且須使用 2–16 碼大寫英文或數字。',
        'Asset symbols must be unique and use 2–16 uppercase letters or digits.'));
      return;
    }
    setBusy(true); setMessage('');
    try {
      const policy: Policy = { version: (current?.version ?? 0) + 1, source: 'BINANCE',
        platformRate: baseRate, platformFixedUsdt: baseFixed,
        assetAdjustments: Object.fromEntries(rows.map((row, i) => [assets[i], row.rate])),
        assetFixedAdjustmentsUsdt: Object.fromEntries(rows.map((row, i) => [assets[i], row.fixed])) };
      const response = await fetch(path, { method: 'POST', credentials: 'same-origin', cache: 'no-store',
        headers: { 'Content-Type': 'application/json', 'X-LumiX-Intent': 'trading-policy' }, body: JSON.stringify(policy) });
      if (!response.ok) throw await createAdminApiError(response);
      const saved = await response.json() as Policy;
      setCurrent(saved); setBaseRate(saved.platformRate); setBaseFixed(saved.platformFixedUsdt);
      setRows(rowsOf(saved));
      setMessage(text(`預覽政策 v${saved.version} 已保存；Binance 外部送單仍未啟用。`,
        `Preview policy v${saved.version} saved. Binance order routing remains disabled.`));
    } catch (error) {
      if (error && typeof error === 'object' && 'kind' in error && error.kind === 'unauthorized') notifyAdminUnauthorized();
      setMessage(text('政策未保存，請確認費率、版本及管理權限。', 'Policy was not saved. Check values, version and access.'));
    } finally { setBusy(false); }
  }

  return <section className="card stack">
    <h2 className="card__title">{text('Binance 路由價預覽', 'Binance routed price preview')}</h2>
    <p className="assets-metric__hint">{text('可設定百分比或每單位固定 USDT 價差；不使用的欄位填 0。百分比輸入小數，例如 0.1 代表 10%。此設定不會啟用 Binance 外部送單。',
      'Set a percentage or fixed USDT spread per unit. Enter 0 in unused fields. For example, 0.1 means 10%. This does not enable Binance order routing.')}</p>
    <p>{text('行情來源', 'Market source')}: <strong>Binance</strong> · {text('目前版本', 'Current version')}: <strong>{current?.version ?? (loaded ? '—' : '…')}</strong></p>
    <form className="stack" onSubmit={event => void publish(event)}>
      <label className="field"><span>{text('全站百分比基準', 'Global percentage base')}</span><input className="input" inputMode="decimal" required value={baseRate} onChange={event => setBaseRate(event.target.value)} placeholder="0" /></label>
      <label className="field"><span>{text('全站固定 USDT／每單位幣', 'Global fixed USDT per asset unit')}</span><input className="input" inputMode="decimal" required value={baseFixed} onChange={event => setBaseFixed(event.target.value)} placeholder="0" /></label>
      <h3>{text('幣別調整', 'Asset adjustments')}</h3>
      {rows.map((row, index) => <div className="trading-form__grid" key={index}>
        <label className="field"><span>{text('幣別', 'Asset')}</span><input className="input" required maxLength={16} value={row.asset} onChange={event => edit(index, 'asset', event.target.value)} /></label>
        <label className="field"><span>{text('百分比調整', 'Percentage adjustment')}</span><input className="input" inputMode="decimal" required value={row.rate} onChange={event => edit(index, 'rate', event.target.value)} placeholder="0" /></label>
        <label className="field"><span>{text('固定 USDT 調整', 'Fixed USDT adjustment')}</span><input className="input" inputMode="decimal" required value={row.fixed} onChange={event => edit(index, 'fixed', event.target.value)} placeholder="0" /></label>
        <button className="secondary-button" type="button" disabled={busy} onClick={() => setRows(previous => previous.filter((_, position) => position !== index))}>{text('移除幣別', 'Remove asset')}</button>
      </div>)}
      <button className="secondary-button" type="button" disabled={busy || rows.length >= 32} onClick={() => setRows(previous => [...previous, { asset: '', rate: '', fixed: '' }])}>{text('新增幣別', 'Add asset')}</button>
      <p className="assets-metric__hint">{text('幣別調整可為負，但合計百分比及固定價差都不可小於 0；兩種價差可並用。每次提交建立下一版本。',
        'Asset adjustments may be negative, but each combined spread must stay nonnegative. Both spread types may be used together. Each submission creates a new version.')}</p>
      <button className="primary-button" type="submit" disabled={!loaded || busy || !baseRate || !baseFixed || rows.some(row => !row.asset || !row.rate || !row.fixed)}>{busy ? text('保存中…', 'Saving…') : text('保存下一版預覽政策', 'Save next preview version')}</button>
    </form>
    <p role="status" aria-live="polite">{message}</p>
  </section>;
}
