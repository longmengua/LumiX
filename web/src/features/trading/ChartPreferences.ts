import { useSyncExternalStore } from 'react';

import { defaultChartAverages, type ChartAverage, type StockChartTimeframe } from './TradingChartControls';

const key = 'lumix.chart.preferences';
const frames: StockChartTimeframe[] = ['1d', '1w', '1mo', '1m', '5m', '15m', '1h'];
type Preferences = { timeframe: StockChartTimeframe; averages: ChartAverage[] };
const defaults = (): Preferences => ({ timeframe: '1d', averages: defaultChartAverages.map(item => ({...item})) });

/** 偏好只包含週期、均線根數與開關，不儲存行情、帳戶、憑證或交易狀態。未知內容一律回預設。 */
function read(): Preferences {
  try {
    const value = JSON.parse(localStorage.getItem(key) ?? 'null');
    if (!value || !frames.includes(value.timeframe) || !Array.isArray(value.averages) || value.averages.length !== 4) return defaults();
    const averages = defaultChartAverages.map((line, index) => {
      const item = value.averages[index];
      if (!item || !Number.isInteger(item.period) || item.period < 1 || item.period > 250 || typeof item.enabled !== 'boolean') throw new Error('invalid preference');
      // 顏色及識別值由共用模板決定，不能讓儲存內容注入任意樣式。
      return {...line, period: item.period, enabled: item.enabled};
    });
    return {timeframe: value.timeframe, averages};
  } catch { return defaults(); }
}
let current = read();
const listeners = new Set<() => void>();
function storageChanged(event: StorageEvent) {
  if (event.key === key || event.key === null) { current = read(); listeners.forEach(listener => listener()); }
}
function subscribe(listener: () => void) {
  if (listeners.size === 0) window.addEventListener('storage', storageChanged);
  listeners.add(listener);
  return () => { listeners.delete(listener); if (listeners.size === 0) window.removeEventListener('storage', storageChanged); };
}
function update(value: Preferences) {
  current = value;
  try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* 儲存受限時仍保留本次頁面階段偏好。 */ }
  listeners.forEach(listener => listener());
}

/** 現貨與合約訂閱相同偏好；頁面切換、重新整理及同源分頁會同步，不共享任何業務資料。 */
export function useTradingChartPreferences() {
  const preferences = useSyncExternalStore(subscribe, () => current);
  return {
    ...preferences,
    setTimeframe: (timeframe: StockChartTimeframe) => update({...current, timeframe}),
    setAverages: (averages: ChartAverage[]) => update({...current, averages}),
  };
}
