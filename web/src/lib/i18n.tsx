import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from 'react';
import { ConnStatus } from '../types/exchange';

type TranslationVars = Record<string, string | number>;

export type LanguageCode = 'zh-Hant' | 'en';

export type TFunction = (key: I18nKey, vars?: TranslationVars) => string;

export type I18nKey = keyof typeof zhDictionary;

export interface I18nState {
  languageCode: LanguageCode;
  setLanguageCode: (languageCode: LanguageCode) => void;
  t: TFunction;
}

type Dictionary = Record<I18nKey, string>;

const zhDictionary = {
  'app.title': 'Exchange Console',
  'app.symbolLabel': '標的',
  'conn.idle': '尚未連線',
  'conn.connecting': '連線中...',
  'conn.open': '即時更新',
  'conn.reconnecting': '重新連線中',
  'conn.closed': '連線已關閉',
  'conn.error': '連線異常',
  'conn.streamConnected': '串流已連線',
  'conn.subscribed': '已訂閱 {symbol}',
  'conn.parseStreamFailed': '推播資料解析失敗',
  'conn.serverError': '服務器回傳錯誤',
  'conn.serverErrorWithReason': '服務器回傳錯誤: {reason}',
  'conn.wsError': 'WebSocket 連線錯誤',
  'conn.closedRetry': 'Socket 關閉，嘗試重連...',
  'event.loadingSnapshot': '載入市場快照中',
  'event.snapshotLoaded': '市場快照載入完成',
  'event.snapshotLoadedDetail': 'K 線: {klineCount} 根 / 成交: {tradeCount} 筆',
  'event.wsConnected': 'WebSocket 已連線',
  'event.tradeReceived': '收到成交推播',
  'event.tickerUpdated': '收到行情更新',
  'event.subscribedMarket': '已訂閱行情',
  'event.heartbeat': '接收到 gateway 心跳',
  'event.serverError': '伺服器錯誤',
  'event.streamParseError': '推播訊息解析失敗',
  'event.wsError': 'WebSocket 錯誤',
  'event.connectionLost': '連線中斷',
  'event.autoReconnect': '將自動重連',
  'event.wsClosed': 'WebSocket 已關閉',
  'footer.streamLabel': 'Stream: {status} / {message}',
  'topbar.languageSelectorTitle': '切換語言',
  'topbar.languageLabel.zh': '中文',
  'topbar.languageLabel.en': 'EN',
  'topbar.languageOption.zh': '中文(繁體)',
  'topbar.languageOption.en': 'English',
  'topbar.profileTitle': '使用者資訊',
  'topbar.profileLabel': 'Profile',
  'topbar.profileNotLoggedIn': '未登入',
  'topbar.profileLogin': '登入',
  'topbar.profileRegister': '註冊',
  'topbar.profileTheme': '主題設定',
  'market.lastPrice': '最新價格',
  'market.change24h': '24h 漲跌',
  'market.high24h': '24h 最高',
  'market.low24h': '24h 最低',
  'market.volume': '24h 成交量 ({symbol})',
  'kline.title': 'K 線 (1m)',
  'kline.liveUpdates': '即時更新',
  'kline.empty': '尚未載入 K 線',
  'kline.chartAria': 'K 線圖',
  'history.title': '成交紀錄',
  'history.heading.time': '時間',
  'history.heading.side': '方向',
  'history.heading.price': '價格',
  'history.heading.qty': '數量',
  'history.heading.notional': '名目',
  'history.empty': '尚未收到成交資料',
  'history.loading': '載入中...',
  'history.end': '已載入全部成交紀錄',
  'positions.title': '持倉',
  'positions.heading.market': '市場',
  'positions.heading.side': '方向',
  'positions.heading.qty': '數量',
  'positions.heading.entry': '建倉價',
  'positions.heading.margin': '保證金',
  'positions.heading.pnl': '未實現盈虧',
  'order.title': '委託下單',
  'order.label.side': '方向',
  'order.label.type': '類型',
  'order.label.price': '價格',
  'order.label.qty': '數量',
  'order.submitBuy': '送出買單',
  'order.submitSell': '送出賣單',
  'order.current': '目前: {side} | {type} | {price} | {qty}',
  'message.title': '訊息中心',
  'message.empty': '尚無訊息'
} satisfies Record<string, string>;

const enDictionary = {
  'app.title': 'Exchange Console',
  'app.symbolLabel': 'Symbol',
  'conn.idle': 'Not connected',
  'conn.connecting': 'Connecting...',
  'conn.open': 'Live',
  'conn.reconnecting': 'Reconnecting...',
  'conn.closed': 'Closed',
  'conn.error': 'Error',
  'conn.streamConnected': 'Stream connected',
  'conn.subscribed': 'Subscribed {symbol}',
  'conn.parseStreamFailed': 'Failed to parse stream payload',
  'conn.serverError': 'Server returned error',
  'conn.serverErrorWithReason': 'Server returned error: {reason}',
  'conn.wsError': 'WebSocket connection error',
  'conn.closedRetry': 'Socket closed, reconnecting...',
  'event.loadingSnapshot': 'Loading market snapshot',
  'event.snapshotLoaded': 'Market snapshot loaded',
  'event.snapshotLoadedDetail': 'K-lines: {klineCount}, Trades: {tradeCount}',
  'event.wsConnected': 'WebSocket connected',
  'event.tradeReceived': 'Trade push received',
  'event.tickerUpdated': 'Ticker updated',
  'event.subscribedMarket': 'Subscribed to market',
  'event.heartbeat': 'Gateway heartbeat received',
  'event.serverError': 'Server error',
  'event.streamParseError': 'Failed to parse stream event',
  'event.wsError': 'WebSocket error',
  'event.connectionLost': 'Connection lost',
  'event.autoReconnect': 'Auto reconnecting',
  'event.wsClosed': 'WebSocket closed',
  'footer.streamLabel': 'Stream: {status} / {message}',
  'topbar.languageSelectorTitle': 'Language switcher',
  'topbar.languageLabel.zh': '中文',
  'topbar.languageLabel.en': 'EN',
  'topbar.languageOption.zh': '中文(繁體)',
  'topbar.languageOption.en': 'English',
  'topbar.profileTitle': 'Account menu',
  'topbar.profileLabel': 'Profile',
  'topbar.profileNotLoggedIn': 'Not signed in',
  'topbar.profileLogin': 'Sign in',
  'topbar.profileRegister': 'Sign up',
  'topbar.profileTheme': 'Theme settings',
  'market.lastPrice': 'Last Price',
  'market.change24h': '24h Change',
  'market.high24h': '24h High',
  'market.low24h': '24h Low',
  'market.volume': '24h Volume ({symbol})',
  'kline.title': 'K Line (1m)',
  'kline.liveUpdates': 'Live updates',
  'kline.empty': 'No K-line loaded yet',
  'kline.chartAria': 'K-line chart',
  'history.title': 'Trade History',
  'history.heading.time': 'Time',
  'history.heading.side': 'Side',
  'history.heading.price': 'Price',
  'history.heading.qty': 'Qty',
  'history.heading.notional': 'Notional',
  'history.empty': 'No trade data yet',
  'history.loading': 'Loading...',
  'history.end': 'Loaded all trades',
  'positions.title': 'Positions',
  'positions.heading.market': 'Market',
  'positions.heading.side': 'Side',
  'positions.heading.qty': 'Qty',
  'positions.heading.entry': 'Entry',
  'positions.heading.margin': 'Margin',
  'positions.heading.pnl': 'PnL',
  'order.title': 'Order Entry',
  'order.label.side': 'Side',
  'order.label.type': 'Type',
  'order.label.price': 'Price',
  'order.label.qty': 'Qty',
  'order.submitBuy': 'Submit Buy',
  'order.submitSell': 'Submit Sell',
  'order.current': 'Current: {side} | {type} | {price} | {qty}',
  'message.title': 'Message Center',
  'message.empty': 'No message yet'
} satisfies Record<string, string>;

const dictionaries: Record<LanguageCode, Dictionary> = {
  'zh-Hant': zhDictionary,
  en: enDictionary
};

const LANGUAGE_STORAGE_KEY = 'exchange-language';

const defaultLanguage = (): LanguageCode => {
  if (typeof window === 'undefined') {
    return 'zh-Hant';
  }
  const raw = window.localStorage.getItem(LANGUAGE_STORAGE_KEY);
  return raw === 'en' ? 'en' : 'zh-Hant';
};

function interpolate(template: string, vars?: TranslationVars): string {
  if (!vars) {
    return template;
  }
  return template.replace(/{(\w+)}/g, (_, key) => {
    const value = vars[key];
    return value === undefined ? `{${key}}` : String(value);
  });
}

/** Create a fast local translator function for a fixed language map. */
export function createTranslator(languageCode: LanguageCode): TFunction {
  return (key, vars) => {
    const catalog = dictionaries[languageCode];
    const template = catalog[key];
    if (template === undefined) {
      return String(key);
    }
    return interpolate(template, vars);
  };
}

/** Keep language selection and translation function in one place for full app scope. */
export function connStatusLabel(status: ConnStatus, t: TFunction): string {
  const map: Record<ConnStatus, I18nKey> = {
    idle: 'conn.idle',
    connecting: 'conn.connecting',
    open: 'conn.open',
    reconnecting: 'conn.reconnecting',
    closed: 'conn.closed',
    error: 'conn.error'
  };
  return t(map[status]);
}

const I18nContext = createContext<I18nState | null>(null);

interface I18nProviderProps {
  children: ReactNode;
}

/** Provider writes selected language to localStorage and syncs document root language tag. */
export function I18nProvider({ children }: I18nProviderProps) {
  const [languageCode, setLanguageCode] = useState<LanguageCode>(defaultLanguage);

  useEffect(() => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, languageCode);
    document.documentElement.lang = languageCode === 'en' ? 'en' : 'zh-Hant';
  }, [languageCode]);

  const t = useMemo(() => createTranslator(languageCode), [languageCode]);

  const value = useMemo(
    () => ({
      languageCode,
      setLanguageCode,
      t
    }),
    [languageCode, t]
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

/** Read current language state in React trees that render labels/messages. */
export function useI18n(): I18nState {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within I18nProvider');
  }
  return context;
}
