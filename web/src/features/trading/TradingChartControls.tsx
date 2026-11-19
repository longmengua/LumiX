import { useId, useRef, useState, type FormEvent } from 'react';

import { useI18n } from '../../i18n';

export type StockChartTimeframe = '1d' | '1w' | '1mo' | '1m' | '5m' | '15m' | '1h';
export type ChartAverage = { id: number; period: number; color: string; enabled: boolean };

/** 沿用原圖表的四條均線與色彩；設定是顯示偏好，不涉及行情或交易狀態。 */
export const defaultChartAverages: ChartAverage[] = [
  { id: 1, period: 5, color: '#fbbf24', enabled: true },
  { id: 2, period: 20, color: '#38bdf8', enabled: true },
  { id: 3, period: 60, color: '#c084fc', enabled: true },
  { id: 4, period: 120, color: '#fb7185', enabled: true },
];

type Props = {
  timeframe: StockChartTimeframe;
  onTimeframeChange: (value: StockChartTimeframe) => void;
  averages: ChartAverage[];
  onAveragesChange: (value: ChartAverage[]) => void;
};

const calendarFrames = [
  { value: '1d', labelKey: 'trading.chart.timeframe.1d' },
  { value: '1w', labelKey: 'trading.chart.timeframe.1w' },
  { value: '1mo', labelKey: 'trading.chart.timeframe.1mo' },
] as const;
const minuteFrames = [
  { value: '1m', label: '1min' },
  { value: '5m', label: '5min' },
  { value: '15m', label: '15min' },
  { value: '1h', label: '60min' },
] as const;

/** 股票式週期列將日／週／月與分線分組；分線選取即切換圖表，沒有額外的確認動作。 */
export function TradingChartControls({ timeframe, onTimeframeChange, averages, onAveragesChange }: Props) {
  const { t } = useI18n();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const panelId = useId();
  const settingsTrigger = useRef<HTMLButtonElement>(null);
  const closeSettings = () => { setSettingsOpen(false); settingsTrigger.current?.focus(); };
  const minuteSelected = minuteFrames.some(item => item.value === timeframe);
  return (
    <div className="market-chart__controls">
      <div className="market-chart__timeframes" role="group" aria-label={t('trading.chart.timeframes')}>
        {calendarFrames.map(item => <button className={`market-chart__timeframe${timeframe === item.value ? ' market-chart__timeframe--active' : ''}`} type="button" key={item.value} aria-pressed={timeframe === item.value} onClick={() => onTimeframeChange(item.value)}>{t(item.labelKey)}</button>)}
        <select className={`market-chart__minute-select${minuteSelected ? ' market-chart__minute-select--active' : ''}`} value={minuteSelected ? timeframe : ''} aria-label={t('trading.chart.minuteLines')} onChange={event => {
          // 僅允許列出的週期，不讓 DOM 或未知選項帶入來源不支援的任意 interval。
          const selected = minuteFrames.find(item => item.value === event.target.value);
          if (selected) onTimeframeChange(selected.value);
        }}>
          <option value="" disabled>{t('trading.chart.minuteLines')}</option>
          {minuteFrames.map(item => <option value={item.value} key={item.value}>{item.label}</option>)}
        </select>
        <button className="market-chart__settings-toggle" type="button" ref={settingsTrigger} aria-expanded={settingsOpen} aria-controls={panelId} onClick={() => setSettingsOpen(value => !value)}>{t('trading.chart.averageSettings')}</button>
      </div>
      {settingsOpen && <AverageEditor id={panelId} averages={averages} onApply={value => { onAveragesChange(value); closeSettings(); }} onCancel={closeSettings} />}
    </div>
  );
}

/** 編輯草稿與目前圖線分離；只有完整有效的設定可套用，空白或小數週期不會進入均線運算。 */
function AverageEditor({ id, averages, onApply, onCancel }: { id: string; averages: ChartAverage[]; onApply: (value: ChartAverage[]) => void; onCancel: () => void }) {
  const { t } = useI18n();
  const [draft, setDraft] = useState(() => averages.map(item => ({ ...item, period: String(item.period) })));
  const [error, setError] = useState(false);
  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const valid = draft.every(item => /^\d{1,3}$/.test(item.period) && Number(item.period) >= 1 && Number(item.period) <= 250);
    if (!valid) { setError(true); return; }
    onApply(draft.map(item => ({ ...item, period: Number(item.period) })));
  }
  return (
    <form className="market-chart__settings-panel" id={id} onSubmit={apply}>
      <p>{t('trading.chart.averageSettingsHint')}</p>
      <div className="market-chart__average-fields">
        {draft.map((item, index) => <div className="market-chart__average-field" key={item.id}>
          <label><input type="checkbox" checked={item.enabled} onChange={event => setDraft(values => values.map(value => value.id === item.id ? { ...value, enabled: event.target.checked } : value))} /><span style={{color: item.color}}>{t('trading.chart.averageLine', undefined, { number: index + 1 })}</span></label>
          <label><span>{t('trading.chart.averagePeriod')}</span><input className="input" type="number" min="1" max="250" step="1" required value={item.period} aria-label={t('trading.chart.averagePeriodAria', undefined, { number: index + 1 })} onChange={event => { setError(false); setDraft(values => values.map(value => value.id === item.id ? { ...value, period: event.target.value } : value)); }} /></label>
        </div>)}
      </div>
      {error && <p className="form-message form-message--error" role="alert">{t('trading.chart.averagePeriodError')}</p>}
      <div className="market-chart__settings-actions">
        <button className="secondary-button" type="button" onClick={() => {setDraft(defaultChartAverages.map(item => ({ ...item, period: String(item.period) }))); setError(false);}}>{t('trading.chart.restoreAverages')}</button>
        <button className="secondary-button" type="button" onClick={onCancel}>{t('common.cancel')}</button>
        <button className="primary-button" type="submit">{t('trading.chart.applySettings')}</button>
      </div>
    </form>
  );
}
