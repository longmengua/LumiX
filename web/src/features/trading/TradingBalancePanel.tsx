import { useState } from 'react';

import { useI18n } from '../../i18n';
import { formatAmount, formatCurrency, formatPercent } from '../../utils/format';
import type { TradingBalance, TradingPosition } from './mockTradingService';

type TradingBalancePanelProps = {
  balances: TradingBalance[];
  positions: TradingPosition[];
  riskRatio: number;
};

export function TradingBalancePanel({ balances, positions, riskRatio }: TradingBalancePanelProps) {
  const { t } = useI18n();
  const [tab, setTab] = useState<'assets' | 'positions'>('assets');
  const quoteBalance = balances.find((balance) => balance.asset === 'USDT') ?? balances[0];
  const unrealizedPnl = positions.reduce((sum, position) => sum + position.pnl, 0);
  const maintenanceMargin = positions.reduce((sum, position) => sum + position.marginUsed * 0.005, 0);

  return (
    <section className="workspace-panel balance-panel">
      <div className="balance-panel__tabs" role="tablist" aria-label={t('trading.balances.title')}>
        <button className={tab === 'assets' ? 'balance-panel__tab balance-panel__tab--active' : 'balance-panel__tab'} type="button" onClick={() => setTab('assets')}>
          {t('trading.balance.tabs.assets')}
        </button>
        <button className={tab === 'positions' ? 'balance-panel__tab balance-panel__tab--active' : 'balance-panel__tab'} type="button" onClick={() => setTab('positions')}>
          {t('trading.balance.tabs.positions')}
        </button>
      </div>

      {tab === 'assets' ? (
        <>
          <div className="balance-panel__metrics">
            <span>{t('trading.balance.availableMargin')}<strong>{quoteBalance ? formatCurrency(quoteBalance.available) : formatCurrency(0)}</strong></span>
            <span>{t('trading.balance.walletBalance')}<strong>{quoteBalance ? formatCurrency(quoteBalance.total) : formatCurrency(0)}</strong></span>
            <span>{t('trading.balance.maintenanceMargin')}<strong>{formatCurrency(maintenanceMargin)}</strong></span>
            <span>{t('trading.balance.unrealizedPnl')}<strong className={unrealizedPnl >= 0 ? 'pnl--positive' : 'pnl--negative'}>{formatCurrency(unrealizedPnl)}</strong></span>
            <span>{t('trading.balance.marginRatio')}<strong>{formatPercent(riskRatio)}</strong></span>
          </div>
          <div className="balance-panel__assets">
            {balances.map((balance) => (
              <div key={balance.asset}>
                <span>{balance.asset}</span>
                <strong>{balance.asset === 'USDT' ? formatCurrency(balance.available) : formatAmount(balance.available)}</strong>
              </div>
            ))}
          </div>
        </>
      ) : (
        <p className="balance-panel__positions-summary">
          {t('trading.balance.positionCount', undefined, { count: positions.length })}
        </p>
      )}
    </section>
  );
}
