import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ExchangeTopBar } from './components/ExchangeTopBar';
import { KLinePanel } from './components/KLinePanel';
import { MarketRibbon } from './components/MarketRibbon';
import { OrderPanel } from './components/OrderPanel';
import { PositionPanel } from './components/PositionPanel';
import { TradeHistoryPanel } from './components/TradeHistoryPanel';
import {
  buildWsUrl,
  fetchJson,
  normalizeKline,
  normalizeTicker,
  normalizeTrade,
  upsertKlineWithTrade,
  wsCommandSubscribe
} from './lib/exchange';
import { connStatusLabel } from './lib/i18n';
import { useI18n } from './lib/i18n';
import {
  type ConnStatus,
  type KlineRow,
  type MarketTickerState,
  type PositionRow,
  type RawKline,
  type RawTicker,
  type RawTrade,
  type TradeFormState,
  type TradeRow,
  DEFAULT_ORDER_FORM,
  DEFAULT_SYMBOL,
  MAX_CHART_CANDLES,
  MAX_TRADE_ROWS,
  SAMPLE_POSITIONS,
  TRADE_PAGE_SIZE
} from './types/exchange';

type MessageCategory =
  | 'SYSTEM'
  | 'ANNOUNCEMENT'
  | 'ORDER'
  | 'TRADE'
  | 'DEPOSIT'
  | 'WITHDRAW'
  | 'ACCOUNT'
  | 'SECURITY'
  | 'PROMOTION'
  | 'COMPLIANCE';

type MessageSeverity = 'INFO' | 'SUCCESS' | 'WARNING' | 'CRITICAL';

type MessageTab = 'all' | 'unread' | 'archived';

type MessageWsStatus = 'idle' | 'connecting' | 'open' | 'reconnecting' | 'closed' | 'error';

type ReadAllScope = 'ALL' | 'CATEGORY';

interface ApiErrorPayload {
  code: string;
  message: string;
  traceId?: string | null;
}

interface ApiEnvelope<T> {
  ok: boolean;
  data: T | null;
  error?: ApiErrorPayload | string | null;
}

interface ApiSuccess<T> {
  ok: true;
  status: number;
  data: T;
}

interface ApiFailure {
  ok: false;
  status: number;
  code: string;
  message: string;
  traceId?: string;
  retryable: boolean;
}

type ApiResult<T> = ApiSuccess<T> | ApiFailure;

interface MessageSummary {
  messageId: string;
  title: string;
  summary: string;
  category: MessageCategory;
  severity: MessageSeverity;
  createdAt: string;
  isRead: boolean;
  isDeleted: boolean;
  isArchived: boolean;
  isPinned: boolean;
  isExpired: boolean;
  isScheduled: boolean;
  actionUrl: string | null;
  actionLabel: string | null;
}

interface MessageDetail extends MessageSummary {
  body: string;
  readAt: string | null;
  effectiveAt: string | null;
  expireAt: string | null;
  metadata: Record<string, unknown>;
}

interface UnreadCountResponse {
  unreadCount: number;
  byCategory: Record<string, number>;
}

interface MessageApiListResponse {
  nextCursor: string | null;
  hasMore: boolean;
  items: MessageApiListResponseItem[];
}

interface MessageApiListResponseItem {
  messageId: string;
  title: string;
  summary: string;
  category: string;
  severity: string;
  createdAt: string;
  isRead: boolean;
  isDeleted: boolean;
  isArchived: boolean;
  isPinned: boolean;
  isExpired: boolean;
  isScheduled: boolean;
  actionUrl?: string | null;
  actionLabel?: string | null;
}

interface MessageApiDetailResponse {
  messageId: string;
  title: string;
  summary: string;
  body: string;
  category: string;
  severity: string;
  createdAt: string;
  effectiveAt: string | null;
  expireAt: string | null;
  isRead: boolean;
  readAt: string | null;
  isDeleted: boolean;
  isArchived: boolean;
  isPinned: boolean;
  isExpired: boolean;
  isScheduled: boolean;
  actionUrl: string | null;
  actionLabel: string | null;
  metadata?: Record<string, unknown> | null;
}

interface MessageReadResponse {
  messageId: string;
  isRead: boolean;
  readAt: string;
}

interface MessageReadAllResponse {
  updatedCount: number;
}

interface MessageArchiveResponse {
  messageId: string;
  isArchived: boolean;
}

interface MessagePinResponse {
  messageId: string;
  isPinned: boolean;
}

interface MessageDeleteResponse {
  messageId: string;
  deleted: boolean;
}

interface MessagePreferencePayload {
  category: string;
  inAppEnabled: boolean;
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  locked: boolean;
}

interface MessagePreferenceState {
  inAppEnabled: boolean;
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  locked: boolean;
}

interface MessagePreferencesResponse {
  preferences: MessagePreferencePayload[];
}

interface MessageApiPreferencesUpdateResponse {
  updated: number;
  preferences: MessagePreferencePayload[];
}

interface WsEnvelope {
  event?: string;
  type?: string;
  data?: unknown;
}

interface WsMessageNewPayload {
  messageId?: string;
  title?: string;
  summary?: string;
  category?: string;
  severity?: string;
  createdAt?: string;
  isRead?: boolean;
  isDeleted?: boolean;
  isArchived?: boolean;
  isPinned?: boolean;
  isExpired?: boolean;
  isScheduled?: boolean;
  actionUrl?: string | null;
  actionLabel?: string | null;
}

interface WsUnreadPayload {
  unreadCount?: number;
  byCategory?: Record<string, number>;
}

const PAGE_SIZE = 30;
const SEARCH_DEBOUNCE_MS = 300;
const MESSAGE_RETRY_DELAYS_MS = [500, 1000, 2000];
const EXCHANGE_TRADE_HISTORY_LIMIT = TRADE_PAGE_SIZE;

const CATEGORY_ALIAS: Record<string, MessageCategory> = {
  WITHDRAWAL: 'WITHDRAW',
  CAMPAIGN: 'PROMOTION'
};

const DEFAULT_PREFERENCE_VALUES: Record<MessageCategory, MessagePreferenceState> = {
  SYSTEM: { inAppEnabled: true, emailEnabled: true, smsEnabled: false, pushEnabled: false, locked: true },
  ANNOUNCEMENT: { inAppEnabled: true, emailEnabled: true, smsEnabled: false, pushEnabled: false, locked: false },
  ORDER: { inAppEnabled: true, emailEnabled: true, smsEnabled: true, pushEnabled: true, locked: false },
  TRADE: { inAppEnabled: true, emailEnabled: true, smsEnabled: true, pushEnabled: true, locked: false },
  DEPOSIT: { inAppEnabled: true, emailEnabled: true, smsEnabled: false, pushEnabled: true, locked: false },
  WITHDRAW: { inAppEnabled: true, emailEnabled: true, smsEnabled: false, pushEnabled: true, locked: false },
  ACCOUNT: { inAppEnabled: true, emailEnabled: true, smsEnabled: false, pushEnabled: false, locked: false },
  SECURITY: { inAppEnabled: true, emailEnabled: true, smsEnabled: true, pushEnabled: true, locked: true },
  PROMOTION: { inAppEnabled: true, emailEnabled: true, smsEnabled: false, pushEnabled: true, locked: false },
  COMPLIANCE: { inAppEnabled: true, emailEnabled: true, smsEnabled: true, pushEnabled: false, locked: true }
};

const CATEGORY_OPTIONS: { value: MessageCategory | 'ALL'; label: string }[] = [
  { value: 'ALL', label: '全部分類' },
  { value: 'SYSTEM', label: '系統' },
  { value: 'ANNOUNCEMENT', label: '公告' },
  { value: 'ORDER', label: '訂單' },
  { value: 'TRADE', label: '交易' },
  { value: 'DEPOSIT', label: '入金' },
  { value: 'WITHDRAW', label: '提領' },
  { value: 'ACCOUNT', label: '帳戶' },
  { value: 'SECURITY', label: '安全' },
  { value: 'PROMOTION', label: '活動' },
  { value: 'COMPLIANCE', label: '法遵' }
];

const CATEGORY_LABELS: Record<MessageCategory, string> = {
  SYSTEM: '系統',
  ANNOUNCEMENT: '公告',
  ORDER: '訂單',
  TRADE: '交易',
  DEPOSIT: '入金',
  WITHDRAW: '提領',
  ACCOUNT: '帳戶',
  SECURITY: '安全',
  PROMOTION: '活動',
  COMPLIANCE: '法遵'
};

const CATEGORY_ICONS: Record<MessageCategory, string> = {
  SYSTEM: '🛰',
  ANNOUNCEMENT: '📢',
  ORDER: '📋',
  TRADE: '💱',
  DEPOSIT: '💰',
  WITHDRAW: '🔐',
  ACCOUNT: '👤',
  SECURITY: '🛡',
  PROMOTION: '🎯',
  COMPLIANCE: '⚖️'
};

const SEVERITY_META: Record<MessageSeverity, { label: string; icon: string; tone: MessageSeverity }> = {
  INFO: { label: '一般', icon: 'ℹ', tone: 'INFO' },
  SUCCESS: { label: '成功', icon: '✓', tone: 'SUCCESS' },
  WARNING: { label: '警告', icon: '!', tone: 'WARNING' },
  CRITICAL: { label: '重要', icon: '⚠', tone: 'CRITICAL' }
};

function clonePreferenceMap(seed: Record<MessageCategory, MessagePreferenceState>): Record<MessageCategory, MessagePreferenceState> {
  const copied = {} as Record<MessageCategory, MessagePreferenceState>;
  (Object.keys(seed) as MessageCategory[]).forEach((category) => {
    copied[category] = { ...seed[category] };
  });
  return copied;
}

function createEmptyUnreadMap(): Record<MessageCategory, number> {
  return {
    SYSTEM: 0,
    ANNOUNCEMENT: 0,
    ORDER: 0,
    TRADE: 0,
    DEPOSIT: 0,
    WITHDRAW: 0,
    ACCOUNT: 0,
    SECURITY: 0,
    PROMOTION: 0,
    COMPLIANCE: 0
  };
}

function normalizeCategory(raw: unknown): MessageCategory | null {
  if (typeof raw !== 'string') {
    return null;
  }
  const normalized = raw.trim().toUpperCase();
  if (Object.prototype.hasOwnProperty.call(CATEGORY_ALIAS, normalized)) {
    return CATEGORY_ALIAS[normalized];
  }
  const supported: MessageCategory[] = [
    'SYSTEM',
    'ANNOUNCEMENT',
    'ORDER',
    'TRADE',
    'DEPOSIT',
    'WITHDRAW',
    'ACCOUNT',
    'SECURITY',
    'PROMOTION',
    'COMPLIANCE'
  ];
  return supported.includes(normalized as MessageCategory) ? (normalized as MessageCategory) : null;
}

function normalizeSeverity(raw: unknown): MessageSeverity {
  const value = typeof raw === 'string' ? raw.trim().toUpperCase() : 'INFO';
  if (value === 'SUCCESS' || value === 'WARNING' || value === 'CRITICAL' || value === 'INFO') {
    return value;
  }
  return 'INFO';
}

function normalizeBoolean(value: unknown, fallback = false): boolean {
  return typeof value === 'boolean' ? value : fallback;
}

function normalizeString(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback;
}

function resolveApiBase(): string {
  const injected = window.__EXCHANGE_API_BASE__?.trim();
  if (injected) {
    return injected.replace(/\/+$/, '');
  }
  return `${window.location.protocol}//${window.location.host}`.replace(/\/+$/, '');
}

function readAuthToken(): string | null {
  const candidates = [
    localStorage.getItem('accessToken'),
    localStorage.getItem('authToken'),
    localStorage.getItem('token'),
    localStorage.getItem('Authorization'),
    sessionStorage.getItem('accessToken'),
    sessionStorage.getItem('token'),
    new URLSearchParams(window.location.search).get('token'),
    new URLSearchParams(window.location.search).get('access_token')
  ];
  for (const raw of candidates) {
    if (!raw) {
      continue;
    }
    const trimmed = raw.trim();
    if (!trimmed) {
      continue;
    }
    const normalized = trimmed.startsWith('Bearer ') ? trimmed.slice(7).trim() : trimmed;
    if (normalized) {
      return normalized;
    }
  }
  return null;
}

function formatDate(time: string): string {
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) {
    return '--';
  }
  return new Intl.DateTimeFormat('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date);
}

function normalizeMessageSummary(raw: MessageApiListResponseItem | WsMessageNewPayload | MessageApiDetailResponse): MessageSummary | null {
  const messageId = normalizeString((raw as { messageId?: unknown }).messageId);
  const category = normalizeCategory((raw as { category?: unknown }).category);
  if (!messageId || !category) {
    return null;
  }
  const actionUrlRaw = (raw as { actionUrl?: unknown }).actionUrl;
  const actionLabelRaw = (raw as { actionLabel?: unknown }).actionLabel;

  return {
    messageId,
    title: normalizeString((raw as { title?: unknown }).title, '--'),
    summary: normalizeString((raw as { summary?: unknown }).summary, ''),
    category,
    severity: normalizeSeverity((raw as { severity?: unknown }).severity),
    createdAt: normalizeString((raw as { createdAt?: unknown }).createdAt),
    isRead: normalizeBoolean((raw as { isRead?: unknown })),
    isDeleted: normalizeBoolean((raw as { isDeleted?: unknown })),
    isArchived: normalizeBoolean((raw as { isArchived?: unknown })),
    isPinned: normalizeBoolean((raw as { isPinned?: unknown })),
    isExpired: normalizeBoolean((raw as { isExpired?: unknown })),
    isScheduled: normalizeBoolean((raw as { isScheduled?: unknown })),
    actionUrl: typeof actionUrlRaw === 'string' ? actionUrlRaw : null,
    actionLabel: typeof actionLabelRaw === 'string' ? actionLabelRaw : null
  };
}

function normalizeMessageDetail(raw: MessageApiDetailResponse): MessageDetail {
  const base = normalizeMessageSummary(raw);
  if (!base) {
    return {
      messageId: '',
      title: '--',
      summary: '--',
      category: 'SYSTEM',
      severity: 'INFO',
      createdAt: new Date().toISOString(),
      isRead: false,
      isDeleted: false,
      isArchived: false,
      isPinned: false,
      isExpired: false,
      isScheduled: false,
      actionUrl: null,
      actionLabel: null,
      body: '--',
      readAt: null,
      effectiveAt: null,
      expireAt: null,
      metadata: {}
    };
  }
  return {
    ...base,
    body: normalizeString((raw as { body?: unknown }).body, '--'),
    readAt: normalizeString((raw as { readAt?: unknown }).readAt) || null,
    effectiveAt: normalizeString((raw as { effectiveAt?: unknown }).effectiveAt) || null,
    expireAt: normalizeString((raw as { expireAt?: unknown }).expireAt) || null,
    metadata:
      (raw as { metadata?: unknown }).metadata &&
      typeof (raw as { metadata?: unknown }).metadata === 'object'
        ? ((raw as { metadata?: Record<string, unknown> }).metadata as Record<string, unknown>)
        : {}
  };
}

function normalizeUnreadMap(raw: unknown): Record<MessageCategory, number> {
  const map = createEmptyUnreadMap();
  if (!raw || typeof raw !== 'object') {
    return map;
  }

  Object.entries(raw as Record<string, unknown>).forEach(([key, value]) => {
    const category = normalizeCategory(key);
    const count = typeof value === 'number' && Number.isFinite(value) ? value : 0;
    if (category) {
      map[category] = count;
    }
  });
  return map;
}

function normalizePreferencePayload(payload: MessagePreferencePayload[]): Record<MessageCategory, MessagePreferenceState> {
  const next = clonePreferenceMap(DEFAULT_PREFERENCE_VALUES);
  payload.forEach((item) => {
    const category = normalizeCategory(item.category);
    if (!category) {
      return;
    }
    next[category] = {
      inAppEnabled: true,
      emailEnabled: normalizeBoolean(item.emailEnabled, next[category].emailEnabled),
      smsEnabled: normalizeBoolean(item.smsEnabled, next[category].smsEnabled),
      pushEnabled: normalizeBoolean(item.pushEnabled, next[category].pushEnabled),
      locked: normalizeBoolean(item.locked, next[category].locked)
    };
  });
  return next;
}

function toRecordArray(values: Record<MessageCategory, MessagePreferenceState>): MessagePreferencePayload[] {
  return (Object.keys(values) as MessageCategory[]).map((category) => ({
    category,
    inAppEnabled: true,
    emailEnabled: values[category].emailEnabled,
    smsEnabled: values[category].smsEnabled,
    pushEnabled: values[category].pushEnabled,
    locked: values[category].locked
  }));
}

function parseApiError(raw: unknown, fallbackStatus = 500): ApiFailure {
  if (!raw || typeof raw !== 'object') {
    return {
      ok: false,
      status: fallbackStatus,
      code: `HTTP_${fallbackStatus}`,
      message: `Request failed (${fallbackStatus})`,
      retryable: fallbackStatus >= 500
    };
  }

  const payload = raw as ApiEnvelope<unknown> & { status?: number };
  if (typeof payload.ok === 'boolean' && payload.ok === false) {
    const err = payload.error;
    if (typeof err === 'string') {
      return {
        ok: false,
        status: payload.status ?? fallbackStatus,
        code: `API_${err.toUpperCase()}`,
        message: err,
        retryable: false
      };
    }
    if (err && typeof err === 'object') {
      return {
        ok: false,
        status: payload.status ?? fallbackStatus,
        code: normalizeString(err.code || `HTTP_${fallbackStatus}`).toUpperCase(),
        message: normalizeString(err.message, `Request failed (${fallbackStatus})`),
        traceId: normalizeString(err.traceId),
        retryable: (payload.status ?? fallbackStatus) >= 500
      };
    }
  }

  return {
    ok: false,
    status: payload.status ?? fallbackStatus,
    code: `HTTP_${fallbackStatus}`,
    message: `Request failed (${fallbackStatus})`,
    retryable: fallbackStatus >= 500
  };
}

function resolveErrorMessage(failure: ApiFailure): string {
  const code = failure.code;
  if (code === 'UNAUTHORIZED' || code === 'HTTP_401') {
    return '未授權，請先登入後再試';
  }
  if (code === 'FORBIDDEN' || code === 'HTTP_403') {
    return '無權限操作此功能';
  }
  if (code === 'VALIDATION_ERROR') {
    return '輸入參數不正確';
  }
  if (code === 'MESSAGE_NOT_FOUND') {
    return '訊息不存在或已刪除';
  }
  if (code === 'PREFERENCE_LOCKED') {
    return '偏好項目被系統鎖定，不可修改';
  }
  if (failure.status >= 500) {
    return '後端錯誤，已自動重試';
  }
  return failure.message || '操作失敗，請稍後再試';
}

async function requestMessageApi<T>(
  path: string,
  options: { method?: 'GET' | 'POST' | 'PUT' | 'DELETE'; body?: unknown; query?: URLSearchParams } = {}
): Promise<ApiResult<T>> {
  const method = options.method ?? 'GET';
  const headers: Record<string, string> = {};
  const token = readAuthToken();

  if (method !== 'GET' && options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const base = resolveApiBase();
  const url = new URL(path.startsWith('http') ? path : `${base}${path}`);
  if (options.query) {
    options.query.forEach((value, key) => {
      url.searchParams.set(key, value);
    });
  }

  for (let attempt = 0; attempt < 2; attempt += 1) {
    try {
      const res = await fetch(url.toString(), {
        method,
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
        cache: 'no-store'
      });

      const parsed = await res.json().catch(async () => null);
      if (parsed && typeof parsed === 'object' && 'ok' in parsed) {
        const envelope = parsed as ApiEnvelope<T>;
        if (envelope.ok) {
          return {
            ok: true,
            status: res.status,
            data: (envelope.data as T) ?? ({} as T)
          };
        }

        const failure = parseApiError(parsed, res.status);
        if (attempt < 1 && failure.retryable) {
          await new Promise((resolve) => setTimeout(resolve, MESSAGE_RETRY_DELAYS_MS[Math.min(attempt, 1)]));
          continue;
        }
        return failure;
      }

      if (!res.ok) {
        const statusFailure = parseApiError({}, res.status);
        if (attempt < 1 && statusFailure.retryable) {
          await new Promise((resolve) => setTimeout(resolve, MESSAGE_RETRY_DELAYS_MS[Math.min(attempt, 1)]));
          continue;
        }
        return statusFailure;
      }

      return {
        ok: true,
        status: res.status,
        data: (parsed as T) ?? ({} as T)
      };
    } catch {
      const networkFailure: ApiFailure = {
        ok: false,
        status: 0,
        code: 'NETWORK_ERROR',
        message: '網路不穩，請稍後再試',
        retryable: attempt < 1
      };
      if (attempt < 1) {
        await new Promise((resolve) => setTimeout(resolve, MESSAGE_RETRY_DELAYS_MS[attempt]));
        continue;
      }
      return networkFailure;
    }
  }

  return {
    ok: false,
    status: 0,
    code: 'UNKNOWN_ERROR',
    message: '無法完成請求',
    retryable: false
  };
}

function buildMessageWsUrl(): string {
  const base = buildWsUrl();
  const token = readAuthToken();
  if (!token) {
    return base;
  }
  const separator = base.includes('?') ? '&' : '?';
  return `${base}${separator}token=${encodeURIComponent(token)}`;
}

function normalizeMetadataEntries(metadata: Record<string, unknown> = {}): Array<[string, string]> {
  return Object.entries(metadata).map(([key, raw]) => {
    if (typeof raw === 'string') {
      return [key, raw];
    }
    if (raw === null || raw === undefined) {
      return [key, '--'];
    }
    return [key, JSON.stringify(raw)];
  });
}

function messageMatchesFilter(
  item: MessageSummary,
  tab: MessageTab,
  category: MessageCategory | 'ALL',
  search: string
): boolean {
  if (item.isDeleted) {
    return false;
  }

  if (tab === 'unread' && (item.isRead || item.isArchived)) {
    return false;
  }

  if (tab === 'archived' && !item.isArchived) {
    return false;
  }

  if (tab !== 'archived' && item.isArchived) {
    return false;
  }

  if (category !== 'ALL' && item.category !== category) {
    return false;
  }

  const keyword = search.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return `${item.title} ${item.summary}`.toLowerCase().includes(keyword);
}

function isCursorInvalidError(result: ApiFailure): boolean {
  return result.code === 'INVALID_CURSOR' || /cursor/i.test(result.message);
}

export function ExchangeConsole() {
  const { t } = useI18n();
  const [activePage, setActivePage] = useState<'exchange' | 'message-center'>(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('view') === 'messages' ? 'message-center' : 'exchange';
  });

  const [exchangeSymbol] = useState(DEFAULT_SYMBOL);
  const [exchangeConnStatus, setExchangeConnStatus] = useState<ConnStatus>('idle');
  const [exchangeTicker, setExchangeTicker] = useState<MarketTickerState | null>(null);
  const [exchangeCandles, setExchangeCandles] = useState<KlineRow[]>([]);
  const [exchangeTrades, setExchangeTrades] = useState<TradeRow[]>([]);
  const [exchangeTradeLoading, setExchangeTradeLoading] = useState(false);
  const [exchangeTradeHasMore, setExchangeTradeHasMore] = useState(false);
  const [exchangeOrderForm, setExchangeOrderForm] = useState<TradeFormState>(DEFAULT_ORDER_FORM);
  const exchangePositions: PositionRow[] = SAMPLE_POSITIONS;
  const exchangeWsRef = useRef<WebSocket | null>(null);
  const exchangeWsRetryRef = useRef(0);
  const exchangeWsTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const stopExchangeWsRef = useRef(false);
  const exchangeTradeRequestId = useRef(0);

  const exchangeConnText = useMemo(() => connStatusLabel(exchangeConnStatus, t), [exchangeConnStatus, t]);

  const exchangeToMessageView = useCallback(() => {
    const next = new URL(window.location.href);
    next.searchParams.set('view', 'messages');
    window.history.replaceState({}, '', `${next.pathname}${next.search}`);
    setActivePage('message-center');
  }, []);

  const exchangeToTradingView = useCallback(() => {
    const next = new URL(window.location.href);
    next.searchParams.delete('view');
    window.history.replaceState({}, '', `${next.pathname}${next.search}`);
    setActivePage('exchange');
  }, []);

  const [viewMode, setViewMode] = useState<'list' | 'settings'>('list');
  const [activeTab, setActiveTab] = useState<MessageTab>('all');
  const [categoryFilter, setCategoryFilter] = useState<MessageCategory | 'ALL'>('ALL');
  const [searchText, setSearchText] = useState('');
  const [searchTextDebounced, setSearchTextDebounced] = useState('');
  const [messages, setMessages] = useState<MessageSummary[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [isListLoading, setIsListLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [newMessageHints, setNewMessageHints] = useState(0);

  const [selectedMessageId, setSelectedMessageId] = useState<string | null>(null);
  const [selectedMessage, setSelectedMessage] = useState<MessageDetail | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [actionFeedback, setActionFeedback] = useState('');

  const [unreadTotal, setUnreadTotal] = useState(0);
  const [unreadByCategory, setUnreadByCategory] = useState<Record<MessageCategory, number>>(createEmptyUnreadMap());
  const [isFirstPage, setIsFirstPage] = useState(true);

  const [preferencesLoaded, setPreferencesLoaded] = useState(false);
  const [activePreferences, setActivePreferences] = useState<Record<MessageCategory, MessagePreferenceState>>(
    DEFAULT_PREFERENCE_VALUES
  );
  const [draftPreferences, setDraftPreferences] = useState<Record<MessageCategory, MessagePreferenceState>>(
    DEFAULT_PREFERENCE_VALUES
  );
  const [preferenceLoading, setPreferenceLoading] = useState(false);
  const [preferenceSaving, setPreferenceSaving] = useState(false);
  const [preferenceError, setPreferenceError] = useState<string | null>(null);
  const [preferenceFeedback, setPreferenceFeedback] = useState('');

  const [wsStatus, setWsStatus] = useState<MessageWsStatus>('idle');

  const messageListRequestId = useRef(0);
  const detailRequestId = useRef(0);
  const unreadRequestId = useRef(0);

  const wsRef = useRef<WebSocket | null>(null);
  const wsRetryRef = useRef(0);
  const wsTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const connectWsRef = useRef<() => void>(() => {});
  const stopWsRef = useRef(false);

  const filtersRef = useRef({
    activeTab: 'all' as MessageTab,
    categoryFilter: 'ALL' as MessageCategory | 'ALL',
    searchText: '',
    viewMode: 'list' as 'list' | 'settings',
    isFirstPage: true
  });

  const normalizeExchangeTradeRows = useCallback((rawTrades: unknown): TradeRow[] => {
    if (!Array.isArray(rawTrades)) {
      return [];
    }

    return rawTrades
      .map((item) => normalizeTrade(item as RawTrade))
      .filter((row): row is TradeRow => row !== null);
  }, []);

  const normalizeExchangeCandles = useCallback((rawCandles: unknown): KlineRow[] => {
    if (!Array.isArray(rawCandles)) {
      return [];
    }

    return rawCandles
      .map((item) => normalizeKline(item as RawKline))
      .filter((item): item is KlineRow => item !== null)
      .sort((a, b) => Date.parse(a.openTime) - Date.parse(b.openTime));
  }, []);

  const applyExchangeTrade = useCallback((incoming: TradeRow) => {
    setExchangeTrades((prev) => {
      const exists = prev.some((item) => item.matchId === incoming.matchId);
      if (exists) {
        return prev
          .map((item) => (item.matchId === incoming.matchId ? { ...item, ...incoming } : item))
          .sort((a, b) => b.tsKey - a.tsKey)
          .slice(0, MAX_TRADE_ROWS);
      }

      return [incoming, ...prev].sort((a, b) => b.tsKey - a.tsKey).slice(0, MAX_TRADE_ROWS);
    });
  }, []);

  const loadExchangeSnapshot = useCallback(async () => {
    const requestId = ++exchangeTradeRequestId.current;
    setExchangeTradeLoading(true);

    const [rawTicker, rawKlines, rawTrades] = await Promise.all([
      fetchJson<RawTicker>(`/api/market-data/${exchangeSymbol}/ticker`),
      fetchJson<RawKline[]>(`/api/market-data/${exchangeSymbol}/klines?limit=${MAX_CHART_CANDLES}`),
      fetchJson<RawTrade[]>(`/api/market-data/${exchangeSymbol}/trades?limit=${EXCHANGE_TRADE_HISTORY_LIMIT}`)
    ]);

    if (requestId !== exchangeTradeRequestId.current) {
      return;
    }

    setExchangeTicker((rawTicker && normalizeTicker(rawTicker)) || null);

    const normalizedCandles = normalizeExchangeCandles(rawKlines).slice(-MAX_CHART_CANDLES);
    setExchangeCandles(normalizedCandles);

    const normalizedTrades = normalizeExchangeTradeRows(rawTrades);
    setExchangeTrades(normalizedTrades);
    setExchangeTradeHasMore(Array.isArray(rawTrades) && rawTrades.length === EXCHANGE_TRADE_HISTORY_LIMIT);
    setExchangeTradeLoading(false);

  }, [exchangeSymbol, normalizeExchangeCandles, normalizeExchangeTradeRows]);

  const loadMoreExchangeTrades = useCallback(async () => {
    if (!exchangeTradeHasMore || exchangeTradeLoading || exchangeTrades.length === 0) {
      return;
    }

    const cursor = exchangeTrades[exchangeTrades.length - 1];
    const earliestTs = new Date(cursor.tsKey).toISOString();
    if (Number.isNaN(Date.parse(earliestTs))) {
      return;
    }

    const query = new URLSearchParams();
    query.set('limit', String(EXCHANGE_TRADE_HISTORY_LIMIT));
    query.set('beforeTs', earliestTs);

    setExchangeTradeLoading(true);
    const rawTrades = await fetchJson<RawTrade[]>(`/api/market-data/${exchangeSymbol}/trades?${query.toString()}`);
    if (!Array.isArray(rawTrades)) {
      setExchangeTradeLoading(false);
      setExchangeTradeHasMore(false);
      return;
    }

    const incoming = normalizeExchangeTradeRows(rawTrades);
    setExchangeTrades((prev) => {
      const exists = new Set(prev.map((trade) => trade.matchId));
      const next = [...prev, ...incoming.filter((trade) => !exists.has(trade.matchId))];
      return next.sort((a, b) => b.tsKey - a.tsKey).slice(0, MAX_TRADE_ROWS);
    });
    setExchangeTradeHasMore(rawTrades.length === EXCHANGE_TRADE_HISTORY_LIMIT);
    setExchangeTradeLoading(false);
  }, [exchangeSymbol, exchangeTradeHasMore, exchangeTradeLoading, exchangeTrades, normalizeExchangeTradeRows]);

  const connectExchangeWebSocket = useCallback(() => {
    const connect = () => {
      if (stopExchangeWsRef.current) {
        return;
      }

      if (exchangeWsTimerRef.current) {
        clearTimeout(exchangeWsTimerRef.current);
        exchangeWsTimerRef.current = null;
      }

      const socket = new WebSocket(buildWsUrl());
      exchangeWsRef.current = socket;
      setExchangeConnStatus('connecting');

      socket.onopen = () => {
        if (stopExchangeWsRef.current) {
          return;
        }

        exchangeWsRetryRef.current = 0;
        setExchangeConnStatus('open');
        socket.send(JSON.stringify(wsCommandSubscribe(exchangeSymbol)));
        void loadExchangeSnapshot();
      };

      socket.onmessage = (event) => {
        let payload: { event?: string; type?: string; data?: unknown };
        try {
          payload = JSON.parse(event.data);
        } catch {
          return;
        }

        if (!payload || (typeof payload.event !== 'string' && typeof payload.type !== 'string')) {
          return;
        }

        const eventName = (payload.event || payload.type || '').toLowerCase();
        if (eventName === 'ticker') {
          const normalized = normalizeTicker(payload.data as RawTicker);
          if (normalized) {
            setExchangeTicker(normalized);
          }
          return;
        }

        if (eventName === 'trade') {
          const rawTrade = payload.data as RawTrade;
          const trade = normalizeTrade(rawTrade);
          if (trade) {
            applyExchangeTrade(trade);
            setExchangeCandles((prev) => upsertKlineWithTrade(prev, rawTrade, MAX_CHART_CANDLES).slice(-MAX_CHART_CANDLES));
          }
          return;
        }

        if (eventName === 'subscribed.market') {
          return;
        }

        if (eventName === 'gateway.heartbeat' || eventName === 'heartbeat') {
          setExchangeConnStatus('open');
          return;
        }

        if (eventName === 'error') {
          setExchangeConnStatus('error');
        }
      };

      socket.onclose = () => {
        if (stopExchangeWsRef.current) {
          return;
        }

        const retryRound = exchangeWsRetryRef.current;
        if (retryRound < MESSAGE_RETRY_DELAYS_MS.length) {
          setExchangeConnStatus('reconnecting');
          exchangeWsRetryRef.current += 1;
          exchangeWsTimerRef.current = window.setTimeout(() => {
            connect();
          }, MESSAGE_RETRY_DELAYS_MS[retryRound]);
          return;
        }

        setExchangeConnStatus('error');
      };

      socket.onerror = () => {
        if (stopExchangeWsRef.current) {
          return;
        }
        setExchangeConnStatus('error');
      };
    };

    connect();
  }, [applyExchangeTrade, exchangeSymbol, loadExchangeSnapshot]);

  const stopExchangeSocket = useCallback(() => {
    stopExchangeWsRef.current = true;
    if (exchangeWsTimerRef.current) {
      clearTimeout(exchangeWsTimerRef.current);
      exchangeWsTimerRef.current = null;
    }
    exchangeWsRef.current?.close();
  }, []);

  // Kick off exchange stream bootstrap and keep websocket in sync with lifecycle.
  useEffect(() => {
    void loadExchangeSnapshot();
    stopExchangeWsRef.current = false;
    connectExchangeWebSocket();

    return () => {
      stopExchangeSocket();
    };
  }, [connectExchangeWebSocket, loadExchangeSnapshot, stopExchangeSocket]);

  // Keep latest filter/state snapshot for websocket handlers and interval callbacks.
  useEffect(() => {
    filtersRef.current = {
      activeTab,
      categoryFilter,
      searchText: searchTextDebounced,
      viewMode,
      isFirstPage
    };
  }, [activeTab, categoryFilter, searchTextDebounced, viewMode, isFirstPage]);

  // Debounce search input to avoid frequent API calls while typing.
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setSearchTextDebounced(searchText.trim());
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [searchText]);

  const listFilterErrorHint = useMemo(() => {
    const list = [activeTab, categoryFilter, searchTextDebounced].filter(Boolean).join(' / ');
    return list ? `目前條件：${list}` : '';
  }, [activeTab, categoryFilter, searchTextDebounced]);

  const loadUnreadCount = useCallback(async () => {
    const requestId = ++unreadRequestId.current;
    const query = new URLSearchParams();
    query.set('excludeArchived', 'true');

    const result = await requestMessageApi<UnreadCountResponse>('/api/messages/unread-count', { query });
    if (requestId !== unreadRequestId.current) {
      return;
    }

    if (!result.ok) {
      setUnreadByCategory((prev) => prev);
      return;
    }

    setUnreadTotal(Math.max(0, Number(result.data.unreadCount) || 0));
    setUnreadByCategory(normalizeUnreadMap(result.data.byCategory));
  }, []);

  const applyMessagesToList = useCallback((items: MessageSummary[]) => {
    setMessages((prev) => {
      const map = new Map(prev.map((item) => [item.messageId, item]));
      const next = [...prev];

      items.forEach((incoming) => {
        const existing = map.get(incoming.messageId);
        if (!existing) {
          next.push(incoming);
          return;
        }

        const index = next.findIndex((item) => item.messageId === incoming.messageId);
        if (index >= 0) {
          next[index] = { ...existing, ...incoming };
        }
      });

      return next;
    });
  }, []);

  const replaceSingleMessageInList = useCallback((incoming: MessageSummary, prepend = false) => {
    setMessages((prev) => {
      const next = [...prev];
      const index = next.findIndex((item) => item.messageId === incoming.messageId);
      if (index >= 0) {
        next[index] = { ...next[index], ...incoming };
        if (prepend) {
          const [updated] = next.splice(index, 1);
          next.unshift(updated);
        }
        return next;
      }

      if (prepend) {
        next.unshift(incoming);
      } else {
        next.push(incoming);
      }
      return next.slice(0, 160);
    });
  }, []);

  const patchSingleMessage = useCallback((messageId: string, patch: Partial<MessageSummary>) => {
    setMessages((prev) =>
      prev.map((item) => (item.messageId === messageId ? { ...item, ...patch } : item))
    );
    setSelectedMessage((current) =>
      current && current.messageId === messageId ? ({ ...current, ...patch } as MessageDetail) : current
    );
  }, []);

  const loadMessagePage = useCallback(
    async (cursor: string | null, reset: boolean) => {
      const requestId = ++messageListRequestId.current;
      const { activeTab: targetTab, categoryFilter: targetCategory, searchText: targetSearch } = filtersRef.current;
      const query = new URLSearchParams();
      query.set('limit', String(PAGE_SIZE));
      query.set('status', targetTab === 'unread' ? 'UNREAD' : 'ALL');
      query.set('archived', targetTab === 'archived' ? 'true' : 'false');
      if (targetSearch) {
        query.set('search', targetSearch);
      }
      if (targetCategory !== 'ALL') {
        query.append('category', targetCategory);
      }
      query.set('pinnedFirst', 'true');
      query.set('excludeDeleted', 'true');
      if (cursor) {
        query.set('cursor', cursor);
      }

      if (reset) {
        setIsListLoading(true);
        setListError(null);
        setIsLoadingMore(false);
      } else {
        setIsLoadingMore(true);
      }

      const result = await requestMessageApi<MessageApiListResponse>('/api/messages', { query });
      if (requestId !== messageListRequestId.current) {
        return;
      }

      if (!result.ok) {
        const errorMessage = resolveErrorMessage(result);
        setListError(errorMessage);
        setIsListLoading(false);
        setIsLoadingMore(false);

        if (cursor && isCursorInvalidError(result)) {
          setListError('資料位移，已自動重載');
          setNewMessageHints(0);
          await loadMessagePage(null, true);
        }
        return;
      }

      const incoming = (result.data?.items ?? [])
        .map((item) => normalizeMessageSummary(item))
        .filter((item): item is MessageSummary => !!item);

      setListError(null);
      setNextCursor(result.data?.nextCursor ?? null);
      setHasMore(Boolean(result.data?.hasMore));

      if (reset) {
        setMessages(incoming);
        setIsFirstPage(true);
      } else {
        applyMessagesToList(incoming);
        setIsFirstPage(false);
      }

      setIsListLoading(false);
      setIsLoadingMore(false);
    },
    [applyMessagesToList]
  );

  const syncCurrentView = useCallback(async () => {
    await Promise.all([loadMessagePage(null, true), loadUnreadCount()]);
  }, [loadMessagePage, loadUnreadCount]);

  const loadMoreMessages = useCallback(() => {
    if (!hasMore || !nextCursor || isLoadingMore || isListLoading) {
      return;
    }
    void loadMessagePage(nextCursor, false);
  }, [hasMore, nextCursor, isLoadingMore, isListLoading, loadMessagePage]);

  const readMessage = useCallback(
    async (messageId: string) => {
      const result = await requestMessageApi<MessageReadResponse>(`/api/messages/${encodeURIComponent(messageId)}/read`, {
        method: 'POST'
      });
      if (!result.ok) {
        return;
      }
      patchSingleMessage(messageId, {
        isRead: true
      });
      void loadUnreadCount();
    },
    [patchSingleMessage, loadUnreadCount]
  );

  const markMessageArchived = useCallback(
    async (messageId: string, archived: boolean) => {
      const endpoint = archived ? '/archive' : '/unarchive';
      const result = await requestMessageApi<MessageArchiveResponse>(
        `/api/messages/${encodeURIComponent(messageId)}${endpoint}`,
        { method: 'POST' }
      );

      if (!result.ok) {
        return resolveErrorMessage(result);
      }

      await syncCurrentView();
      if (selectedMessageId === messageId && archived) {
        setSelectedMessageId(null);
        setSelectedMessage(null);
      }
      return null;
    },
    [syncCurrentView, selectedMessageId]
  );

  const toggleMessagePinned = useCallback(
    async (messageId: string, pinned: boolean) => {
      const method = pinned ? 'DELETE' : 'POST';
      const result = await requestMessageApi<MessagePinResponse>(
        `/api/messages/${encodeURIComponent(messageId)}/pin`,
        { method }
      );

      if (!result.ok) {
        return resolveErrorMessage(result);
      }

      patchSingleMessage(messageId, {
        isPinned: pinned
      });
      return null;
    },
    [patchSingleMessage]
  );

  const deleteMessage = useCallback(
    async (messageId: string) => {
      const result = await requestMessageApi<MessageDeleteResponse>(`/api/messages/${encodeURIComponent(messageId)}`, {
        method: 'DELETE'
      });
      if (!result.ok) {
        return resolveErrorMessage(result);
      }

      setMessages((prev) => prev.filter((item) => item.messageId !== messageId));
      if (selectedMessageId === messageId) {
        setSelectedMessageId(null);
        setSelectedMessage(null);
      }
      void loadUnreadCount();
      return null;
    },
    [selectedMessageId, loadUnreadCount]
  );

  const readAllMessage = useCallback(async () => {
    const body: { scope: ReadAllScope; category?: MessageCategory } = {
      scope: categoryFilter === 'ALL' ? 'ALL' : 'CATEGORY'
    };
    if (body.scope === 'CATEGORY') {
      body.category = categoryFilter as MessageCategory;
    }

    const result = await requestMessageApi<MessageReadAllResponse>('/api/messages/read-all', {
      method: 'POST',
      body
    });

    if (!result.ok) {
      return resolveErrorMessage(result);
    }
    setActionFeedback(`已標為已讀 ${result.data.updatedCount} 則`);
    await syncCurrentView();
    return null;
  }, [categoryFilter, syncCurrentView]);

  const selectMessage = useCallback(
    async (messageId: string) => {
      const requestId = ++detailRequestId.current;
      setSelectedMessageId(messageId);
      setIsDetailLoading(true);
      setDetailError(null);

      const result = await requestMessageApi<MessageApiDetailResponse>(`/api/messages/${encodeURIComponent(messageId)}`, {
        method: 'GET'
      });
      if (requestId !== detailRequestId.current) {
        return;
      }
      if (!result.ok) {
        setSelectedMessage(null);
        setDetailError(resolveErrorMessage(result));
        setIsDetailLoading(false);
        if (result.code === 'MESSAGE_NOT_FOUND') {
          setSelectedMessageId(null);
          setMessages((prev) => prev.filter((item) => item.messageId !== messageId));
        }
        return;
      }

      const normalized = normalizeMessageDetail(result.data);
      setSelectedMessage(normalized);
      setIsDetailLoading(false);

      await readMessage(messageId);
    },
    [readMessage]
  );

  const loadMessagePreferences = useCallback(async () => {
    if (preferenceLoading) {
      return;
    }

    setPreferenceLoading(true);
    setPreferenceError(null);
    setPreferenceFeedback('');

    const result = await requestMessageApi<MessagePreferencesResponse>('/api/message-preferences', { method: 'GET' });
    if (!result.ok) {
      setPreferenceError(resolveErrorMessage(result));
      setPreferenceLoading(false);
      setPreferencesLoaded(false);
      return;
    }

    const next = normalizePreferencePayload(result.data.preferences ?? []);
    setActivePreferences(clonePreferenceMap(next));
    setDraftPreferences(clonePreferenceMap(next));
    setPreferencesLoaded(true);
    setPreferenceLoading(false);
  }, [preferenceLoading]);

  const saveMessagePreferences = useCallback(async () => {
    setPreferenceSaving(true);
    setPreferenceError(null);
    setPreferenceFeedback('');

    const result = await requestMessageApi<MessageApiPreferencesUpdateResponse>('/api/message-preferences', {
      method: 'PUT',
      body: { preferences: toRecordArray(draftPreferences) }
    });
    if (!result.ok) {
      setPreferenceError(resolveErrorMessage(result));
      setPreferenceSaving(false);
      return;
    }

    const next = normalizePreferencePayload(result.data.preferences ?? []);
    setActivePreferences(clonePreferenceMap(next));
    setDraftPreferences(clonePreferenceMap(next));
    setPreferenceFeedback(`更新完成，已更新 ${result.data.updated} 項`);
    setPreferenceSaving(false);
  }, [draftPreferences]);

  const discardPreferenceChanges = useCallback(() => {
    setDraftPreferences(clonePreferenceMap(activePreferences));
    setPreferenceError(null);
    setPreferenceFeedback('');
  }, [activePreferences]);

  const togglePreferenceChannel = useCallback(
    (category: MessageCategory, channel: 'emailEnabled' | 'smsEnabled' | 'pushEnabled') => {
      setPreferenceFeedback('');
      setPreferenceError(null);
      setDraftPreferences((prev) => {
        const current = prev[category];
        if (!current || current.locked) {
          return prev;
        }
        return {
          ...prev,
          [category]: { ...current, [channel]: !current[channel] }
        };
      });
    },
    []
  );

  const updateWsMessage = useCallback(
    (payload: unknown) => {
      const summary = normalizeMessageSummary((payload || {}) as WsMessageNewPayload);
      if (!summary) {
        return;
      }

      if (!summary.messageId) {
        return;
      }

      void loadUnreadCount();

      const { activeTab: currentTab, categoryFilter: currentCategory, searchText: currentSearch, viewMode: currentViewMode, isFirstPage } =
        filtersRef.current;

      if (currentViewMode !== 'list') {
        setNewMessageHints((prev) => prev + 1);
        return;
      }

      if (currentTab === 'archived') {
        setNewMessageHints((prev) => prev + 1);
        return;
      }

      if (!messageMatchesFilter(summary, currentTab, currentCategory, currentSearch)) {
        setNewMessageHints((prev) => prev + 1);
        return;
      }

      if (!isFirstPage) {
        setNewMessageHints((prev) => prev + 1);
        return;
      }

      replaceSingleMessageInList(summary, true);
      setIsFirstPage(true);
      setNewMessageHints(0);
    },
    [loadUnreadCount, replaceSingleMessageInList]
  );

  const updateWsUnreadCount = useCallback(
    (payload: unknown) => {
      const data = payload as WsUnreadPayload;
      if (!data || typeof data !== 'object') {
        void loadUnreadCount();
        return;
      }
      if (typeof data.unreadCount === 'number' || data.unreadCount === 0) {
        setUnreadTotal(Math.max(0, Number(data.unreadCount) || 0));
      }
      if (data.byCategory && typeof data.byCategory === 'object') {
        setUnreadByCategory(normalizeUnreadMap(data.byCategory));
        return;
      }
      void loadUnreadCount();
    },
    [loadUnreadCount]
  );

  const wsStatusLabel = useMemo(() => {
    if (wsStatus === 'open') {
      return 'WS 連線中';
    }
    if (wsStatus === 'connecting') {
      return 'WS 連線中...';
    }
    if (wsStatus === 'reconnecting') {
      return 'WS 重連中';
    }
    if (wsStatus === 'error') {
      return 'WS 連線中斷';
    }
    return 'WS 未啟動';
  }, [wsStatus]);

  // Initial bootstrap + reload list when filter changes.
  useEffect(() => {
    void syncCurrentView();
  }, []);

  useEffect(() => {
    void loadMessagePage(null, true);
  }, [activeTab, categoryFilter, searchTextDebounced, loadMessagePage]);

  // Load preference data when entering preference page.
  useEffect(() => {
    if (viewMode === 'settings' && !preferencesLoaded) {
      void loadMessagePreferences();
    }
  }, [viewMode, preferencesLoaded, loadMessagePreferences]);

  useEffect(() => {
    let isUnmounted = false;

    const connect = () => {
      if (isUnmounted) {
        return;
      }

      if (wsTimerRef.current) {
        clearTimeout(wsTimerRef.current);
        wsTimerRef.current = null;
      }

      if (stopWsRef.current) {
        return;
      }

      const socket = new WebSocket(buildMessageWsUrl());
      wsRef.current = socket;
      setWsStatus('connecting');

      socket.onopen = () => {
        if (isUnmounted || stopWsRef.current) {
          return;
        }
        wsRetryRef.current = 0;
        setWsStatus('open');
        try {
          socket.send(JSON.stringify({ type: 'subscribe.user' }));
        } catch {
          // Some backend gateway may ignore subscribe for private user channel.
        }
        void syncCurrentView();
      };

      socket.onmessage = (event) => {
        if (isUnmounted || stopWsRef.current) {
          return;
        }
        let payload: WsEnvelope;
        try {
          payload = JSON.parse(event.data) as WsEnvelope;
        } catch {
          return;
        }

        const eventName = payload.event || payload.type || '';
        if (eventName === 'message.new') {
          updateWsMessage(payload.data ?? payload);
        } else if (eventName === 'message.unreadCount') {
          updateWsUnreadCount(payload.data);
        }
      };

      socket.onclose = () => {
        if (isUnmounted || stopWsRef.current) {
          return;
        }
        const retryRound = wsRetryRef.current;
        if (retryRound < MESSAGE_RETRY_DELAYS_MS.length) {
          setWsStatus('reconnecting');
          wsRetryRef.current += 1;
          wsTimerRef.current = window.setTimeout(() => {
            connect();
          }, MESSAGE_RETRY_DELAYS_MS[retryRound]);
          return;
        }
        setWsStatus('error');
      };

      socket.onerror = () => {
        if (isUnmounted || stopWsRef.current) {
          return;
        }
        setWsStatus('error');
      };
    };

    const reconnectNow = () => {
      if (isUnmounted || stopWsRef.current) {
        return;
      }
      wsRetryRef.current = 0;
      wsRef.current?.close();
      connect();
    };

    connectWsRef.current = reconnectNow;
    connect();

    return () => {
      isUnmounted = true;
      stopWsRef.current = true;
      if (wsTimerRef.current) {
        clearTimeout(wsTimerRef.current);
      }
      wsRef.current?.close();
    };
  }, [syncCurrentView, updateWsMessage, updateWsUnreadCount]);

  useEffect(() => {
    if (selectedMessageId && messages.length > 0 && !messages.some((item) => item.messageId === selectedMessageId)) {
      setSelectedMessageId(null);
      setSelectedMessage(null);
    }
  }, [selectedMessageId, messages]);

  const listErrorBanner = useMemo(() => {
    if (!listError) {
      return null;
    }
    return (
      <div className="message-empty error">
        {listError}
      </div>
    );
  }, [listError]);

  const topbarConnStatus: ConnStatus = activePage === 'exchange' ? exchangeConnStatus : wsStatus;
  const topbarConnText = activePage === 'exchange' ? exchangeConnText : wsStatusLabel;

  if (activePage === 'exchange') {
    return (
      <div className="exchange-page">
        <ExchangeTopBar
          symbol={exchangeSymbol}
          connText={topbarConnText}
          status={topbarConnStatus}
          unreadCount={unreadTotal}
          onMessageCenterOpen={exchangeToMessageView}
          onBackToExchange={exchangeToTradingView}
          showBackButton={activePage === 'message-center'}
        />

        <MarketRibbon ticker={exchangeTicker} symbol={exchangeSymbol} candles={exchangeCandles} />

        <div className="exchange-grid">
          <div className="top-row">
            <div className="stack-column">
              <KLinePanel candles={exchangeCandles} />
              <TradeHistoryPanel
                trades={exchangeTrades}
                loading={exchangeTradeLoading}
                hasMore={exchangeTradeHasMore}
                onLoadMore={loadMoreExchangeTrades}
              />
            </div>

            <div className="stack-column">
              <OrderPanel
                side={exchangeOrderForm.side}
                type={exchangeOrderForm.type}
                price={exchangeOrderForm.price}
                qty={exchangeOrderForm.qty}
                onStateChange={setExchangeOrderForm}
              />
              <PositionPanel rows={exchangePositions} />
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="exchange-page">
      <ExchangeTopBar
        symbol={exchangeSymbol}
        connText={topbarConnText}
        status={topbarConnStatus}
        unreadCount={unreadTotal}
        onMessageCenterOpen={exchangeToMessageView}
        onBackToExchange={exchangeToTradingView}
        showBackButton={activePage === 'message-center'}
      />
      <header className="message-center-header">
        <div>
          <h1>訊息中心</h1>
          <p>集中管理公告、交易、資產與安全訊息</p>
        </div>
        <div>
          <div className="message-center-badge">
            未讀：
            <strong>{unreadTotal}</strong>
            <span className="badge-dot">{unreadTotal === 0 ? '全數已讀' : '未讀中'}</span>
          </div>
          <div className="message-center-badge">
            {wsStatusLabel}
            {wsStatus === 'error' ? (
              <button
                type="button"
                onClick={() => {
                  stopWsRef.current = false;
                  wsRetryRef.current = 0;
                  connectWsRef.current();
                }}
                style={{ marginLeft: 8 }}
              >
                點我重試
              </button>
            ) : null}
          </div>
        </div>
      </header>

      <div className="message-center-layout">
        <aside className="message-center-nav panel">
          <button
            type="button"
            className={`nav-button ${viewMode === 'list' ? 'active' : ''}`}
            onClick={() => setViewMode('list')}
          >
            通知列表
          </button>
          <button
            type="button"
            className={`nav-button ${viewMode === 'settings' ? 'active' : ''}`}
            onClick={() => setViewMode('settings')}
          >
            通知偏好
          </button>
        </aside>

        {viewMode === 'list' ? (
          <section className="message-center-shell">
            <section className="panel message-controls">
              <div className="message-controls__tabs">
                <button
                  type="button"
                  className={activeTab === 'all' ? 'active' : ''}
                  onClick={() => setActiveTab('all')}
                >
                  全部
                </button>
                <button
                  type="button"
                  className={activeTab === 'unread' ? 'active' : ''}
                  onClick={() => setActiveTab('unread')}
                >
                  未讀
                </button>
                <button
                  type="button"
                  className={activeTab === 'archived' ? 'active' : ''}
                  onClick={() => setActiveTab('archived')}
                >
                  已封存
                </button>
              </div>

              <div className="message-controls__filters">
                <div className="search-box">
                  <span>🔍</span>
                  <input
                    value={searchText}
                    onChange={(event) => setSearchText(event.currentTarget.value)}
                    placeholder="搜尋標題或正文"
                  />
                </div>

                <div className="message-category-grid">
                  {CATEGORY_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      className={`category-chip ${categoryFilter === option.value ? 'active' : ''}`}
                      onClick={() => setCategoryFilter(option.value)}
                    >
                      {option.label}
                      <span>
                        {option.value === 'ALL'
                          ? unreadTotal
                          : `${unreadByCategory[option.value as MessageCategory] ?? 0} 未讀`}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="message-toolbar">
                <button
                  type="button"
                  onClick={() => {
                    void readAllMessage().then((errorMessage) => {
                      if (errorMessage) {
                        setActionFeedback(errorMessage);
                        window.setTimeout(() => setActionFeedback(''), 2200);
                      }
                    });
                  }}
                >
                  全部已讀
                </button>
                <button
                  type="button"
                  onClick={() => {
                    void syncCurrentView();
                    setActionFeedback('重新整理完成');
                    window.setTimeout(() => setActionFeedback(''), 1200);
                  }}
                >
                  重新整理
                </button>
              </div>

              <p className="muted">篩選條件：{listFilterErrorHint}</p>
            </section>

            <div className="message-center-grid">
              <section className="panel">
                <div className="panel-head">
                  <h2>
                    訊息列表
                    <span className="message-list-sub"> {messages.length} 筆</span>
                  </h2>
                </div>

                {newMessageHints > 0 ? (
                  <div className="message-empty">
                    你有 {newMessageHints} 則新訊息，點擊「載入最新」以更新列表
                    <div className="detail-actions">
                      <button
                        type="button"
                        onClick={() => {
                          setNewMessageHints(0);
                          void syncCurrentView();
                        }}
                      >
                        載入最新
                      </button>
                    </div>
                  </div>
                ) : null}

                <div className="message-list-shell">
                  {isListLoading ? (
                    <div className="message-skeleton">
                      <div className="skeleton-line" />
                      <div className="skeleton-line short" />
                      <div className="skeleton-line" />
                    </div>
                  ) : null}

                  {listErrorBanner}

                  {!isListLoading && !listError ? (
                    messages.length === 0 ? (
                      <div className="message-empty">目前沒有符合條件的訊息</div>
                    ) : (
                      messages
                        .filter((message) => messageMatchesFilter(message, activeTab, categoryFilter, searchTextDebounced))
                        .map((message) => {
                          const severity = SEVERITY_META[message.severity];
                          return (
                            <article
                              key={message.messageId}
                              className={`message-row ${message.isRead ? '' : 'unread'} ${selectedMessageId === message.messageId ? 'selected' : ''} tone-${severity.tone.toLowerCase()}`}
                              onClick={() => {
                                void selectMessage(message.messageId);
                              }}
                            >
                              <div className="message-row__head">
                                <div className="message-row__icon">{CATEGORY_ICONS[message.category]}</div>
                                <div className="message-row__main">
                                  <div className="message-row__title">
                                    <span>{message.title}</span>
                                    {message.isPinned ? <span className="pin-badge">已釘選</span> : null}
                                    {message.category === 'SECURITY' || message.category === 'COMPLIANCE' ? (
                                      <span className="pin-badge critical">高優先</span>
                                    ) : null}
                                  </div>
                                  <div className="message-row__summary">{message.summary}</div>
                                  <div className="message-row__meta">
                                    <span>{CATEGORY_LABELS[message.category]}</span>
                                    <span className={`severity-badge ${severity.tone.toLowerCase()}`}>
                                      {severity.icon} {severity.label}
                                    </span>
                                    <span>{formatDate(message.createdAt)}</span>
                                  </div>
                                </div>
                              </div>

                              <div className="message-row__actions">
                                <button
                                  type="button"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    void readMessage(message.messageId);
                                  }}
                                >
                                  {message.isRead ? '已讀' : '標為已讀'}
                                </button>
                                <button
                                  type="button"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    void toggleMessagePinned(message.messageId, !message.isPinned).then((errorMessage) => {
                                      if (errorMessage) {
                                        setActionFeedback(errorMessage);
                                        window.setTimeout(() => setActionFeedback(''), 2200);
                                      }
                                    });
                                  }}
                                >
                                  {message.isPinned ? '取消釘選' : '釘選'}
                                </button>
                                <button
                                  type="button"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    void markMessageArchived(message.messageId, !message.isArchived).then((errorMessage) => {
                                      if (errorMessage) {
                                        setActionFeedback(errorMessage);
                                        window.setTimeout(() => setActionFeedback(''), 2200);
                                      }
                                    });
                                  }}
                                >
                                  {message.isArchived ? '取消封存' : '封存'}
                                </button>
                                <button
                                  type="button"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    void deleteMessage(message.messageId).then((errorMessage) => {
                                      if (errorMessage) {
                                        setActionFeedback(errorMessage);
                                        window.setTimeout(() => setActionFeedback(''), 2200);
                                      }
                                    });
                                  }}
                                >
                                  刪除
                                </button>
                              </div>
                            </article>
                          );
                        })
                    )
                  ) : null}

                  {isListLoading ? null : (
                    <div className="message-pagination">
                      {!isLoadingMore && hasMore ? (
                        <button type="button" className="load-more" onClick={loadMoreMessages}>
                          載入更多
                        </button>
                      ) : null}
                      {isLoadingMore ? <div className="message-empty">載入更多中...</div> : null}
                    </div>
                  )}
                </div>

                {actionFeedback ? <div className="save-feedback">{actionFeedback}</div> : null}
              </section>

              <section className="panel message-detail-panel">
                <div className="panel-head">
                  <h2>訊息詳情</h2>
                </div>
                {selectedMessageId ? (
                  <article className="message-detail">
                    {isDetailLoading ? (
                      <div className="message-skeleton">
                        <div className="skeleton-line" />
                        <div className="skeleton-line short" />
                        <div className="skeleton-line" />
                      </div>
                    ) : null}

                    {detailError ? <div className="message-empty error">{detailError}</div> : null}

                    {selectedMessage ? (
                      <>
                        <h3>{selectedMessage.title}</h3>
                        <p className="muted">{selectedMessage.summary}</p>
                        <div className="message-meta-grid">
                          <span>分類</span>
                          <strong>{CATEGORY_LABELS[selectedMessage.category]}</strong>
                          <span>重要度</span>
                          <strong>{SEVERITY_META[selectedMessage.severity].label}</strong>
                          <span>狀態</span>
                          <strong>{selectedMessage.isRead ? '已讀' : '未讀'}</strong>
                          <span>釘選</span>
                          <strong>{selectedMessage.isPinned ? '是' : '否'}</strong>
                          <span>封存</span>
                          <strong>{selectedMessage.isArchived ? '是' : '否'}</strong>
                          <span>已排程</span>
                          <strong>{selectedMessage.isScheduled ? '是' : '否'}</strong>
                          <span>時間</span>
                          <strong>{formatDate(selectedMessage.createdAt)}</strong>
                          <span>到期</span>
                          <strong>{selectedMessage.expireAt ? formatDate(selectedMessage.expireAt) : '無'}</strong>
                        </div>
                        <p className="message-detail-content">{selectedMessage.body}</p>
                        {normalizeMetadataEntries(selectedMessage.metadata).length > 0 ? (
                          <ul>
                            {normalizeMetadataEntries(selectedMessage.metadata).map(([key, value]) => (
                              <li key={key}>
                                <strong>{key}</strong>
                                <span>{value}</span>
                              </li>
                            ))}
                          </ul>
                        ) : null}
                        {selectedMessage.actionUrl ? (
                          <a className="message-link" href={selectedMessage.actionUrl}>
                            {selectedMessage.actionLabel || '查看詳情'}
                          </a>
                        ) : null}
                        <div className="detail-actions">
                          <button
                            type="button"
                            onClick={() => {
                              void readMessage(selectedMessage.messageId);
                            }}
                          >
                            已讀
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              void toggleMessagePinned(selectedMessage.messageId, !selectedMessage.isPinned).then(
                                (errorMessage) => {
                                  if (errorMessage) {
                                    setActionFeedback(errorMessage);
                                    window.setTimeout(() => setActionFeedback(''), 2200);
                                  }
                                }
                              );
                            }}
                          >
                            {selectedMessage.isPinned ? '取消釘選' : '釘選'}
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              void markMessageArchived(selectedMessage.messageId, !selectedMessage.isArchived).then(
                                (errorMessage) => {
                                  if (errorMessage) {
                                    setActionFeedback(errorMessage);
                                    window.setTimeout(() => setActionFeedback(''), 2200);
                                  }
                                }
                              );
                            }}
                          >
                            {selectedMessage.isArchived ? '取消封存' : '封存'}
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              void deleteMessage(selectedMessage.messageId).then((errorMessage) => {
                                if (errorMessage) {
                                  setActionFeedback(errorMessage);
                                  window.setTimeout(() => setActionFeedback(''), 2200);
                                }
                              });
                            }}
                          >
                            刪除
                          </button>
                        </div>
                      </>
                    ) : null}
                  </article>
                ) : (
                  <div className="message-empty">請先選擇一則訊息</div>
                )}
              </section>
            </div>
          </section>
        ) : (
          <section className="panel message-settings-panel">
            <div className="panel-head">
              <h2>通知偏好</h2>
              <span>站內通知固定開啟；安全/法遵無法關閉電子郵件與 SMS / Push。</span>
            </div>

            {preferenceLoading ? (
              <div className="message-skeleton">
                <div className="skeleton-line" />
                <div className="skeleton-line" />
                <div className="skeleton-line short" />
              </div>
            ) : null}

            {!preferenceLoading ? (
              <>
                <p className="message-empty error">{preferenceError}</p>

                <div className="message-settings-grid">
                  <div className="message-settings-row header">
                    <span>分類</span>
                    <span>站內</span>
                    <span>Email</span>
                    <span>SMS</span>
                    <span>Push</span>
                  </div>
                  {CATEGORY_OPTIONS.filter((option) => option.value !== 'ALL').map((option) => {
                    const category = option.value as MessageCategory;
                    const setting = draftPreferences[category];
                    return (
                      <div className="message-settings-row" key={category}>
                        <span>{option.label}</span>
                        <label>
                          <input type="checkbox" checked disabled />
                          開啟
                        </label>
                        <label>
                          <input
                            type="checkbox"
                            checked={setting.emailEnabled}
                            disabled={setting.locked}
                            onChange={() => togglePreferenceChannel(category, 'emailEnabled')}
                          />
                          {setting.locked ? '不可關閉' : '開啟'}
                        </label>
                        <label>
                          <input
                            type="checkbox"
                            checked={setting.smsEnabled}
                            disabled={setting.locked}
                            onChange={() => togglePreferenceChannel(category, 'smsEnabled')}
                          />
                          {setting.locked ? '不可關閉' : '開啟'}
                        </label>
                        <label>
                          <input
                            type="checkbox"
                            checked={setting.pushEnabled}
                            disabled={setting.locked}
                            onChange={() => togglePreferenceChannel(category, 'pushEnabled')}
                          />
                          {setting.locked ? '不可關閉' : '開啟'}
                        </label>
                      </div>
                    );
                  })}
                </div>

                <div className="detail-actions">
                  <button
                    type="button"
                    onClick={() => {
                      void saveMessagePreferences();
                    }}
                  >
                    儲存設定
                  </button>
                  <button type="button" onClick={discardPreferenceChanges}>
                    放棄變更
                  </button>
                </div>

                {preferenceSaving ? <div className="message-empty">儲存中...</div> : null}
                {preferenceError ? <div className="message-empty error">{preferenceError}</div> : null}
                {preferenceFeedback ? <div className="save-feedback">{preferenceFeedback}</div> : null}
              </>
            ) : null}
          </section>
        )}
      </div>
    </div>
  );
}

export default ExchangeConsole;
