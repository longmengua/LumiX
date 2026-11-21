import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { formatMarketDecimal, isMarketDecimal } from './marketDecimal';

type Kind = 'spot' | 'futures';
type Order = { orderId: string; marketSymbol: string; side: string; status: string; price: string; quantity: string; filledQuantity: string; remainingQuantity: string };
type Balance = { asset: string; total: string | null; available: string | null; locked: string | null };
type Fill = { tradeId: string; side: string; price: string; quantity: string; fee: string; feeAsset: string };
type Level = { price: string; quantity: string };
type Workspace = { balances: Balance[]; fills: Fill[]; bids: Level[]; asks: Level[] };
type Configuration = { ordersEnabled: boolean; reason: string; makerFeeRate: string | null; takerFeeRate: string | null };
type Snapshot = { workspace: Workspace; orders: Order[]; configuration: Configuration; received: number };
type Command = { marketSymbol: string; side: string; orderType: string; timeInForce: string; price: string | null; quantity: string };
class RequestError extends Error { constructor(public status: number) { super(`HTTP ${status}`); } }

/** 所有帳戶資料及命令只經同源 LumiX API；不向第三方傳送 session 或交易意圖。 */
async function request<T>(path: string, signal: AbortSignal, method = 'GET', body?: unknown, key?: string): Promise<T> {
  const abort = new AbortController();
  const cancel = () => abort.abort();
  signal.addEventListener('abort', cancel, { once: true });
  if (signal.aborted) abort.abort();
  // 命令超時仍保留原冪等鍵，不能把服務端可能已提交的交易當作未送出。
  const deadline = window.setTimeout(cancel, 10000);
  try {
    const response = await fetch(path, { method, signal: abort.signal, credentials: 'same-origin', cache: 'no-store',
      headers: method === 'GET' ? {} : { 'Content-Type': 'application/json', 'X-LumiX-Intent': 'trading-command', 'Idempotency-Key': key! },
      body: body === undefined ? undefined : JSON.stringify(body) });
    if (!response.ok) throw new RequestError(response.status);
    return await response.json() as T;
  } finally { window.clearTimeout(deadline); signal.removeEventListener('abort', cancel); }
}
const amount = (value: string | null) => value !== null && isMarketDecimal(value) ? formatMarketDecimal(value) : '—';

/** 共用真實交易面板：斷線清除舊資產，送單不樂觀成功；不確定結果使用原冪等鍵重試。 */
export function useLiveTradingPanels(kind: Kind, market: string, baseAsset: string, english: boolean) {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [side, setSide] = useState('BUY');
  const [orderType, setOrderType] = useState('LIMIT');
  const [price, setPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [pending, setPending] = useState<{ body: Command; key: string } | null>(null);
  const [cancelPending, setCancelPending] = useState<{ order: string; key: string } | null>(null);
  const control = useRef<AbortController | null>(null);
  const commandRunning = useRef(false);
  const queryVersion = useRef(0);
  const path = `/api/v1/trading/${kind}`;
  const text = (zh: string, en: string) => english ? en : zh;
  const failure = useCallback((error: unknown) => error instanceof RequestError && error.status === 401
    ? (english ? 'Sign in to access assets and orders.' : '請先登入以查詢資產與下單。')
    : (english ? 'Trading data unavailable. Refreshing…' : '交易資料暫時無法取得，正在重新整理…'), [english]);

  const refresh = useCallback(async (signal: AbortSignal) => {
    const version = ++queryVersion.current;
    try {
      const [workspace, orders, configuration] = await Promise.all([
        request<Workspace>(`${path}/workspace?market=${encodeURIComponent(market)}`, signal),
        request<{ items: Order[] }>(`${path}/orders?limit=100`, signal),
        request<Configuration>(`${path}/configuration?market=${encodeURIComponent(market)}`, signal),
      ]);
      // 只有完整成功的查詢才取代快照，金額永遠保留後端 decimal string。
      if (!Array.isArray(workspace.balances) || !Array.isArray(workspace.fills) || !Array.isArray(workspace.bids)
        || !Array.isArray(workspace.asks) || !Array.isArray(orders.items) || typeof configuration.ordersEnabled !== 'boolean') throw new Error('invalid workspace');
      if (!signal.aborted && version === queryVersion.current) setSnapshot({ workspace, orders: orders.items.filter(order => order.marketSymbol === market), configuration, received: Date.now() });
    } catch (error) {
      if (!signal.aborted && version === queryVersion.current) { setSnapshot(null); if (!commandRunning.current) setMessage(failure(error)); }
    }
  }, [path, market, failure]);

  useEffect(() => {
    const abort = new AbortController(); control.current = abort; setSnapshot(null); setMessage('');
    let stopped = false;
    let timer: number | undefined;
    // 不重疊輪詢；每次最多等待四秒，避免 hung request 讓畫面永久停留在舊餘額。
    async function poll() {
      const timeout = new AbortController();
      const cancel = () => timeout.abort(); abort.signal.addEventListener('abort', cancel, { once: true });
      const deadline = window.setTimeout(() => timeout.abort(), 4000);
      await refresh(timeout.signal);
      window.clearTimeout(deadline); abort.signal.removeEventListener('abort', cancel);
      if (!stopped) timer = window.setTimeout(poll, 1500);
    }
    void poll();
    const expiry = window.setInterval(() => setSnapshot(value => value && Date.now() - value.received > 5000 ? null : value), 1000);
    return () => { stopped = true; abort.abort(); window.clearInterval(expiry); if (timer !== undefined) window.clearTimeout(timer); };
  }, [refresh]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (commandRunning.current || (!pending && (!snapshot?.configuration.ordersEnabled || Date.now() - snapshot.received > 5000))) return;
    const command = pending ?? { body: { marketSymbol: market, side, orderType, timeInForce: orderType === 'MARKET' ? 'IOC' : 'GTC', price: orderType === 'LIMIT' ? price : null, quantity }, key: crypto.randomUUID() };
    setPending(command); setBusy(true); commandRunning.current = true; setMessage('');
    try {
      const order = await request<Order>(`${path}/orders`, control.current!.signal, 'POST', command.body, command.key);
      setPending(null); setMessage(text(`委託 ${order.orderId}：${order.status}，已成交 ${order.filledQuantity}。`, `Order ${order.orderId}: ${order.status}; filled ${order.filledQuantity}.`));
      setSnapshot(null); await refresh(control.current!.signal);
    } catch (error) {
      if (error instanceof RequestError && error.status < 500) {
        setPending(null); setMessage(error.status === 401 ? failure(error) : text('委託未受理，請確認餘額與商品規格。', 'Order rejected. Check balance and instrument rules.'));
      } else setMessage(text('送單結果尚未確認；重試會使用同一識別碼，避免重複下單。', 'Order outcome is unconfirmed. Retry uses the same idempotency key.'));
      setSnapshot(null);
    } finally { commandRunning.current = false; setBusy(false); }
  }

  async function cancel(order: Order) {
    if (commandRunning.current) return;
    const command = cancelPending ?? { order: order.orderId, key: crypto.randomUUID() };
    setCancelPending(command); setBusy(true); commandRunning.current = true;
    try {
      const result = await request<Order>(`${path}/orders/${encodeURIComponent(command.order)}`, control.current!.signal, 'DELETE', undefined, command.key);
      setCancelPending(null); setMessage(text(`委託 ${result.orderId}：${result.status}`, `Order ${result.orderId}: ${result.status}`));
      setSnapshot(null); await refresh(control.current!.signal);
    } catch (error) {
      if (error instanceof RequestError && error.status < 500) setCancelPending(null);
      setMessage(text('撤單尚未確認，請重試並以最新訂單狀態為準。', 'Cancellation unconfirmed. Retry and check the latest order state.'));setSnapshot(null);
    } finally { commandRunning.current = false; setBusy(false); }
  }

  const unavailable = snapshot?.configuration.reason === 'FUTURES_RISK_RUNTIME_UNAVAILABLE'
    ? text('合約保證金與風控尚未開放。', 'Futures margin and risk execution are unavailable.')
    : text('尚未設定完整交易政策或暫停接新單。', 'Complete trading policy is missing or new orders are paused.');
  const sourceAsset = side === 'BUY' ? 'USDT' : baseAsset;
  const available = snapshot?.workspace.balances.find(balance => balance.asset === sourceAsset)?.available ?? null;
  const orderPanel = <form className="trading-form workspace-panel trading-live-panel" onSubmit={submit}>
    <h2>{text(kind === 'spot' ? '現貨下單' : '合約下單', kind === 'spot' ? 'Spot order' : 'Futures order')}</h2>
    <div className="trading-form__toolbar">{['BUY', 'SELL'].map(value => <button className={`tab-button ${side === value ? 'tab-button--active' : ''}`} type="button" key={value} disabled={busy || !!pending || !!cancelPending} onClick={() => setSide(value)}>{value === 'BUY' ? text('買入', 'Buy') : text('賣出', 'Sell')}</button>)}</div>
    <fieldset className="trading-form__grid" disabled={busy || !!pending || !!cancelPending || !snapshot?.configuration.ordersEnabled}>
      <label className="field"><span>{text('委託類型', 'Order type')}</span><select className="input" value={orderType} onChange={event => setOrderType(event.target.value)}><option value="LIMIT">{text('限價 GTC', 'Limit GTC')}</option><option value="MARKET">{text('市價 IOC', 'Market IOC')}</option></select></label>
      <label className="field"><span>{text('可用', 'Available')} ({sourceAsset})</span><output>{amount(available)}</output></label>
      {orderType === 'LIMIT' && <label className="field"><span>{text('價格', 'Price')} (USDT)</span><input className="input" inputMode="decimal" value={price} onChange={event => setPrice(event.target.value)} required pattern="[0-9]+([.][0-9]+)?" /></label>}
      <label className="field"><span>{text('數量', 'Quantity')} ({baseAsset})</span><input className="input" inputMode="decimal" value={quantity} onChange={event => setQuantity(event.target.value)} required pattern="[0-9]+([.][0-9]+)?" /></label>
    </fieldset>
    {snapshot?.configuration.ordersEnabled ? <p>{text('費率', 'Fee rates')} maker {amount(snapshot.configuration.makerFeeRate)} / taker {amount(snapshot.configuration.takerFeeRate)} · {text('實收費用以成交結果為準', 'Actual fees follow settlement')}</p> : <p>{unavailable}</p>}
    <p className="trading-form__hint">{text('僅與 LumiX 真實委託成交；Binance 行情不保證可成交。', 'Matches real LumiX orders only. Binance reference liquidity is not executable.')}</p>
    <button className="primary-button trading-form__submit" type="submit" disabled={busy || !!cancelPending || (!pending && !snapshot?.configuration.ordersEnabled)}>{busy ? text('處理中…', 'Processing…') : pending ? text('重試確認原委託', 'Retry original order') : text('送出委託', 'Place order')}</button>
    <p role="status" aria-live="polite">{message}</p>
  </form>;
  const accountPanel = <section className="workspace-panel balance-panel trading-live-panel">
    <div className="workspace-panel__header"><h2>{text('資產與委託', 'Assets and orders')}</h2><span>LumiX</span></div>
    {!snapshot ? <p>{text('正在取得已登入帳戶資料；未同步前無法送出新單。', 'Loading authenticated account data. New orders require a current snapshot.')}</p> : <>
      <div className="trading-account-table" tabIndex={0}><table><thead><tr>{[text('資產', 'Asset'), text('總額', 'Total'), text('可用', 'Available'), text('鎖定', 'Locked')].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{snapshot.workspace.balances.map(balance => <tr key={balance.asset}><td>{balance.asset}</td><td>{amount(balance.total)}</td><td>{amount(balance.available)}</td><td>{amount(balance.locked)}</td></tr>)}</tbody></table></div>
      <h3>{text('最近委託', 'Recent orders')}</h3>
      <div className="trading-account-table" tabIndex={0}><table><thead><tr>{['ID', text('方向', 'Side'), text('價格', 'Price'), text('數量／已成交', 'Quantity / filled'), text('狀態', 'State'), text('操作', 'Action')].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{snapshot.orders.map(order => <tr key={order.orderId}><td>{order.orderId}</td><td>{order.side}</td><td>{amount(order.price)}</td><td>{amount(order.quantity)} / {amount(order.filledQuantity)}</td><td>{order.status}</td><td>{kind === 'spot' && ['ACCEPTED', 'OPEN', 'PARTIALLY_FILLED'].includes(order.status) && <button type="button" className="secondary-button" disabled={busy || !!pending || (!!cancelPending && cancelPending.order !== order.orderId)} onClick={() => void cancel(order)}>{text('撤單', 'Cancel')}</button>}</td></tr>)}</tbody></table></div>
      {snapshot.orders.length === 0 && <p>{text('尚無委託。', 'No orders.')}</p>}
      <h3>{text('最近成交', 'Recent fills')}</h3>
      {snapshot.workspace.fills.map(fill => <p key={fill.tradeId}>{fill.side} · {amount(fill.quantity)} @ {amount(fill.price)} · {text('費用', 'Fee')} {amount(fill.fee)} {fill.feeAsset}</p>)}
      {snapshot.workspace.fills.length === 0 && <p>{text('尚無成交。', 'No fills.')}</p>}
    </>}
    {cancelPending && <button type="button" disabled={busy} onClick={() => void cancel({ orderId: cancelPending.order } as Order)}>{text('重試原撤單', 'Retry cancellation')}</button>}
  </section>;
  const ownBook = <section className="workspace-panel trading-live-panel"><h2>{text('LumiX 可成交委託簿', 'LumiX executable order book')}</h2>
    {!snapshot ? <p>{text('委託簿尚未同步。', 'Order book unavailable.')}</p> : <div className="trading-form__grid">{(['asks', 'bids'] as const).map(sideName => <div key={sideName}><h3>{sideName === 'asks' ? text('賣盤', 'Asks') : text('買盤', 'Bids')}</h3>{snapshot.workspace[sideName].length === 0 ? <p>{text('尚無委託', 'No orders')}</p> : snapshot.workspace[sideName].map(level => <p key={level.price}>{amount(level.price)} · {amount(level.quantity)}</p>)}</div>)}</div>}
  </section>;
  return { orderPanel: <>{orderPanel}{ownBook}</>, accountPanel };
}
