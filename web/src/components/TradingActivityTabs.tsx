import { PositionPanel } from './PositionPanel';
import { TradeHistoryPanel } from './TradeHistoryPanel';
import { PositionRow, TradeRow } from '../types/exchange';
import { useI18n } from '../lib/i18n';

/**
 * Bottom trading activity area: executions, positions, and future account/order tabs share one list surface.
 */
interface Props {
  activeTab: 'trades' | 'positions';
  onTabChange: (tab: 'trades' | 'positions') => void;
  trades: readonly TradeRow[];
  positions: readonly PositionRow[];
  loading: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
}

export function TradingActivityTabs({
  activeTab,
  onTabChange,
  trades,
  positions,
  loading,
  hasMore,
  onLoadMore
}: Props) {
  const { t } = useI18n();

  return (
    <section className="panel trading-tabs-panel">
      <div className="trading-tabs">
        <button type="button" className={activeTab === 'trades' ? 'active' : ''} onClick={() => onTabChange('trades')}>
          {t('history.title')}
        </button>
        <button type="button" className={activeTab === 'positions' ? 'active' : ''} onClick={() => onTabChange('positions')}>
          {t('positions.title')}
        </button>
      </div>
      {activeTab === 'trades' ? (
        <TradeHistoryPanel
          trades={trades}
          loading={loading}
          hasMore={hasMore}
          onLoadMore={onLoadMore}
          embedded
        />
      ) : (
        <PositionPanel rows={positions} embedded />
      )}
    </section>
  );
}
