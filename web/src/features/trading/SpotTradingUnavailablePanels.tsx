import { Badge } from '../../components/base/Badge';

/** 以合約表單的版型呈現現貨能力邊界；沒有送單 handler、預填餘額或本地假成功。 */
export function SpotUnavailableOrderPanel({ baseAsset, english }: { baseAsset: string; english: boolean }) {
  return (
    <section className="trading-form workspace-panel" aria-label={english ? 'Spot order entry unavailable' : '現貨下單尚未開放'}>
      <div className="trading-form__toolbar">
        <button className="tab-button tab-button--active" type="button" disabled>{english ? 'Buy' : '買入'}</button>
        <button className="tab-button" type="button" disabled>{english ? 'Sell' : '賣出'}</button>
        <Badge tone="warning">{english ? 'Read only' : '僅供瀏覽'}</Badge>
      </div>
      <div className="trading-form__grid">
        <label className="field"><span className="field__label">{english ? 'Order type' : '委託類型'}</span><select className="input" disabled><option>{english ? 'Limit' : '限價'}</option></select></label>
        <label className="field"><span className="field__label">{english ? 'Available' : '可用資產'} (USDT)</span><input className="input" value="—" disabled /></label>
        <label className="field"><span className="field__label">{english ? 'Price' : '價格'} (USDT)</span><input className="input" value="" placeholder="—" disabled /></label>
        <label className="field"><span className="field__label">{english ? 'Quantity' : '數量'} ({baseAsset})</span><input className="input" value="" placeholder="—" disabled /></label>
      </div>
      <div className="trading-form__summary">
        {[english ? 'Estimated value' : '預估名義值', english ? 'Estimated quantity' : '約略數量', english ? 'Order price' : '委託價格'].map(label => <div className="trading-form__summary-row" key={label}><span>{label}</span><strong>—</strong></div>)}
      </div>
      <p className="trading-form__hint">{english ? 'LumiX spot order execution is not available yet. Binance data is for reference only.' : 'LumiX 現貨下單尚未開放。Binance 行情僅供參考。'}</p>
      <button className="primary-button trading-form__submit" type="button" disabled>{english ? 'Spot trading unavailable' : '現貨交易尚未開放'}</button>
    </section>
  );
}

/** 帳戶區維持相同面板位置；缺少工作區資料時只顯示不可用，不推測零餘額或零訂單。 */
export function SpotUnavailableAccountPanel({ english }: { english: boolean }) {
  return (
    <section className="workspace-panel balance-panel">
      <div className="workspace-panel__header"><h2>{english ? 'Assets and orders' : '資產與委託'}</h2><span>{english ? 'Not available in this workspace' : '此工作區尚未提供'}</span></div>
      <div className="balance-panel__tabs"><span className="balance-panel__tab balance-panel__tab--active">{english ? 'Assets' : '資產'}</span><span className="balance-panel__tab">{english ? 'Open orders' : '未完成訂單'}</span></div>
      <p className="balance-panel__empty">{english ? 'Account assets and open orders are not connected to this workspace yet.' : '此工作區尚未提供帳戶資產與委託資料。'}</p>
    </section>
  );
}
