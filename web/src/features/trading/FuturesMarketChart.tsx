import { useI18n } from '../../i18n';
import { useTradingChartPreferences } from './ChartPreferences';
import { getMockMarketCandles } from './mockTradingService';
import { TradingMarketChart } from './TradingMarketChart';

/** 只包裝合約頁既有的開發資料來源；共用模板不載入此 adapter，也不把現貨來源冒充合約成交。 */
export function FuturesMarketChart({ symbol, lastPrice }: { symbol: string; lastPrice: number }) {
  const { timeframe } = useTradingChartPreferences();
  const { t } = useI18n();
  const candles = getMockMarketCandles(symbol, timeframe, lastPrice).map(candle => ({
    openTime: candle.time, open: String(candle.open), high: String(candle.high), low: String(candle.low),
    close: String(candle.close), volume: String(candle.volume), closed: true,
  }));
  return <TradingMarketChart candles={candles} sourceLabel={t('trading.market.mockBadge')} />;
}
