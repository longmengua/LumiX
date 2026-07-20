import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';
import { formatAmount, formatCurrency, formatPercent, formatPrice } from '../../utils/format';
import type { TradingWorkspaceData } from './mockTradingService';

type TradingMarketHeaderProps = {
  workspace: TradingWorkspaceData;
};

export function TradingMarketHeader({ workspace }: TradingMarketHeaderProps) {
  const { t } = useI18n();
  const { marketStats } = workspace;
  const metrics = [
    { label: t('trading.market.markPrice'), value: formatPrice(marketStats.markPrice, 2) },
    { label: t('trading.market.high24h'), value: formatPrice(marketStats.high24h, 2) },
    { label: t('trading.market.low24h'), value: formatPrice(marketStats.low24h, 2) },
    { label: t('trading.market.volume24h'), value: `${formatAmount(marketStats.volume24h, 2)} ${workspace.baseAsset}` },
    { label: t('trading.market.turnover24h'), value: formatCurrency(marketStats.turnover24h) },
    { label: t('trading.market.openInterest'), value: `${formatAmount(marketStats.openInterest, 2)} ${workspace.baseAsset}` },
    { label: t('trading.market.fundingRate'), value: `${formatPercent(marketStats.fundingRate, 3)} / ${marketStats.fundingCountdown}`, compact: true },
  ];

  return (
    <header className="market-header">
      <div className="market-header__identity">
        <span className="market-header__brand">LumiX</span>
        <div>
          <div className="market-header__instrument-row">
            <h1>{workspace.displaySymbol}</h1>
            <Badge tone="warning">{t('trading.market.mockBadge')}</Badge>
          </div>
          <p>{t(workspace.displayNameKey, undefined, workspace.displayNameValues)}</p>
        </div>
      </div>

      <div className="market-header__price">
        <span>{t('trading.market.lastPrice')}</span>
        <strong>{formatPrice(workspace.midPrice, 2)}</strong>
        <em className={workspace.change24h >= 0 ? 'pnl--positive' : 'pnl--negative'}>
          {formatPercent(workspace.change24h)}
        </em>
      </div>

      <div className="market-header__metrics">
        {metrics.map((metric) => (
          <div className={`market-header__metric${metric.compact ? ' market-header__metric--compact' : ''}`} key={metric.label}>
            <span>{metric.label}</span>
            <strong>{metric.value}</strong>
          </div>
        ))}
      </div>
    </header>
  );
}
