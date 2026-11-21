import { useEffect, useState, type FormEvent } from 'react';
import { useI18n } from '../../../i18n';
import { ADMIN_API_BASE_PATH } from '../../api/adminApi';
import { createAdminApiError, notifyAdminUnauthorized } from '../../api/adminError';

type Kind = 'spot' | 'futures';
type Current = { marketSymbol: string; status: string; policyVersion: number; policy: string };
const markets = ['BTC-USDT', 'ETH-USDT'];

async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T | null> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/trading/${path}`, {
    method, credentials: 'same-origin', cache: 'no-store',
    headers: method === 'GET' ? {} : { 'Content-Type': 'application/json', 'X-LumiX-Intent': 'trading-policy' },
    body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
  });
  if (response.status === 404 && method === 'GET') return null;
  if (!response.ok) {
    const error = await createAdminApiError(response);
    if (error.kind === 'unauthorized') notifyAdminUnauthorized();
    throw error;
  }
  return response.json() as Promise<T>;
}

/** 管理端只傳遞管理員明確輸入的完整政策，不預填費率、槓桿或抵押品折算率。 */
export function AdminTradingPolicyPanel({ kind }: { kind: Kind }) {
  const { locale } = useI18n();
  const english = locale === 'en-US';
  const text = (zh: string, en: string) => english ? en : zh;
  const [market, setMarket] = useState(markets[0]);
  const [current, setCurrent] = useState<Current | null>(null);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    setCurrent(null); setDraft(''); setMessage('');
    void request<Current>(`${kind}/policies/current?market=${encodeURIComponent(market)}`).then(value => {
      if (!mounted) return;
      setCurrent(value);
      setDraft(value?.policy ?? '');
    }).catch(() => { if (mounted) setMessage(text('無法取得目前政策。', 'Could not load the current policy.')); });
    return () => { mounted = false; };
  }, [kind, market, locale]);

  async function publish(event: FormEvent) {
    event.preventDefault();
    if (busy || !draft.trim()) return;
    setBusy(true); setMessage('');
    try {
      const policy: unknown = JSON.parse(draft);
      if (!policy || typeof policy !== 'object' || Array.isArray(policy)) throw new Error('invalid policy');
      const selected = kind === 'spot' ? (policy as { rules?: { marketSymbol?: unknown } }).rules?.marketSymbol
        : (policy as { execution?: { rules?: { marketSymbol?: unknown } } }).execution?.rules?.marketSymbol;
      if (selected !== market) throw new Error('policy market does not match selection');
      await request(`${kind}/policies`, 'POST', draft);
      const saved = await request<Current>(`${kind}/policies/current?market=${encodeURIComponent(market)}`);
      setCurrent(saved);
      setDraft(saved?.policy ?? '');
      setMessage(text('政策已保存；請確認商品狀態。', 'Policy saved. Check the instrument status.'));
    } catch {
      setMessage(text('政策未保存。請檢查完整 JSON、版本、帳戶及商品規格。', 'Policy was not saved. Check JSON, version, accounts and instrument rules.'));
    } finally { setBusy(false); }
  }

  async function changeStatus(target: 'ACTIVE' | 'HALTED') {
    if (busy) return;
    setBusy(true); setMessage('');
    try {
      await request('futures/instruments/status', 'POST', { marketSymbol: market, status: target });
      setCurrent(await request<Current>(`futures/policies/current?market=${encodeURIComponent(market)}`));
      setMessage(text(`商品狀態已改為 ${target}。`, `Instrument status is now ${target}.`));
    } catch {
      setMessage(text('狀態未變更；請確認即時行情、保險基金及未結持倉／委託。', 'Status unchanged. Check the reference feed, insurance fund, positions and orders.'));
    } finally { setBusy(false); }
  }

  return <div className="stack">
    <section className="card">
      <h2 className="card__title">{text(kind === 'spot' ? '現貨交易政策' : '合約風險政策', kind === 'spot' ? 'Spot trading policy' : 'Futures risk policy')}</h2>
      <p className="assets-metric__hint">{text('政策需填寫完整 JSON；數值、費率與帳戶由管理員核定。既有政策不可覆寫，變更須提交下一版本。', 'Enter the complete policy JSON. An administrator determines rates, limits and accounts. Existing versions are immutable.')}</p>
      <label className="field"><span className="field__label">{text('交易對', 'Market')}</span><select className="input" value={market} onChange={event => setMarket(event.target.value)}>{markets.map(value => <option key={value} value={value}>{value}</option>)}</select></label>
      <p>{text('目前狀態', 'Current status')}: <strong>{current?.status ?? '—'}</strong> · {text('政策版本', 'Policy version')}: {current?.policyVersion ?? '—'}</p>
      <form onSubmit={event => void publish(event)}>
        <label className="field"><span className="field__label">{text('完整政策 JSON', 'Complete policy JSON')}</span><textarea className="input" rows={18} required maxLength={16000} value={draft} onChange={event => setDraft(event.target.value)} spellCheck={false} /></label>
        <button className="primary-button" type="submit" disabled={busy || !draft.trim()}>{busy ? text('處理中…', 'Working…') : text('提交政策', 'Publish policy')}</button>
      </form>
      {kind === 'futures' && current && <div className="trading-form__toolbar">
        <button className="secondary-button" type="button" disabled={busy || current.status === 'ACTIVE'} onClick={() => void changeStatus('ACTIVE')}>{text('啟用／恢復', 'Activate / resume')}</button>
        <button className="secondary-button" type="button" disabled={busy || current.status === 'HALTED'} onClick={() => void changeStatus('HALTED')}>{text('暫停', 'Halt')}</button>
      </div>}
      {kind === 'futures' && <p className="assets-metric__hint">{text('非 USDT 保證金政策目前只能保存為 HALTED；啟用需已入帳 USDT 保險基金及新鮮永續行情。', 'Non USDT collateral policies remain HALTED. Activation requires funded USDT insurance and a fresh perpetual reference.')}</p>}
      {message && <p role="status" aria-live="polite">{message}</p>}
    </section>
  </div>;
}
