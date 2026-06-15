import { useEffect, useMemo, useState } from 'react';

type MessageCategory =
  | 'SYSTEM'
  | 'ANNOUNCEMENT'
  | 'ORDER'
  | 'TRADE'
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'ACCOUNT'
  | 'SECURITY'
  | 'CAMPAIGN'
  | 'COMPLIANCE';

type MessageSeverity = 'info' | 'success' | 'warning' | 'critical';

type MessageTab = 'all' | 'unread' | 'archived';

interface MessageMetadata {
  label: string;
  value: string;
}

interface MessageLink {
  label: string;
  href: string;
}

interface MessageItem {
  id: string;
  title: string;
  summary: string;
  content: string;
  category: MessageCategory;
  severity: MessageSeverity;
  createdAt: string;
  isRead: boolean;
  isDeleted: boolean;
  isArchived: boolean;
  isPinned: boolean;
  link?: MessageLink;
  metadata: MessageMetadata[];
}

interface MessageNotificationSetting {
  inApp: true;
  email: boolean;
  sms: boolean;
  push: boolean;
  locks: {
    email: boolean;
    sms: boolean;
    push: boolean;
  };
}

interface ChannelConfig {
  value: MessageCategory;
  label: string;
}

interface SeverityMeta {
  label: string;
  icon: string;
  tone: 'info' | 'success' | 'warning' | 'critical';
}

const CHANNELS: ChannelConfig[] = [
  { value: 'SYSTEM', label: '系統通知' },
  { value: 'ANNOUNCEMENT', label: '公告通知' },
  { value: 'ORDER', label: '訂單通知' },
  { value: 'TRADE', label: '成交通知' },
  { value: 'DEPOSIT', label: '入金通知' },
  { value: 'WITHDRAWAL', label: '出金通知' },
  { value: 'ACCOUNT', label: '帳戶通知' },
  { value: 'SECURITY', label: '安全通知' },
  { value: 'CAMPAIGN', label: '活動通知' },
  { value: 'COMPLIANCE', label: '法遵通知' }
];

const SEVERITY: Record<MessageSeverity, SeverityMeta> = {
  info: { label: '一般', icon: 'ℹ', tone: 'info' },
  success: { label: '成功', icon: '✓', tone: 'success' },
  warning: { label: '警告', icon: '!', tone: 'warning' },
  critical: { label: '重要', icon: '⚠', tone: 'critical' }
};

const CATEGORY_ICONS: Record<MessageCategory, string> = {
  SYSTEM: '🛰',
  ANNOUNCEMENT: '📢',
  ORDER: '📋',
  TRADE: '💱',
  DEPOSIT: '💰',
  WITHDRAWAL: '🔐',
  ACCOUNT: '👤',
  SECURITY: '🛡',
  CAMPAIGN: '🎯',
  COMPLIANCE: '⚖️'
};

const CATEGORY_LABELS: Record<MessageCategory, string> = {
  SYSTEM: '系統通知',
  ANNOUNCEMENT: '公告通知',
  ORDER: '訂單通知',
  TRADE: '成交通知',
  DEPOSIT: '入金通知',
  WITHDRAWAL: '出金通知',
  ACCOUNT: '帳戶通知',
  SECURITY: '安全通知',
  CAMPAIGN: '活動通知',
  COMPLIANCE: '法遵通知'
};

const CATEGORY_OPTIONS: { value: MessageCategory | 'ALL'; label: string }[] = [
  { value: 'ALL', label: '全部分類' },
  ...CHANNELS
];

const INITIAL_MESSAGES: MessageItem[] = [
  {
    id: 'msg-001',
    title: '平台維護公告',
    summary: '今晚 02:00 ~ 04:00 進行系統維護',
    content:
      '為維持撮合與撮合歷史資料穩定，平台將於今晚 02:00 開始進行例行維護，預估維持 2 小時，期間下單與提現暫停。',
    category: 'SYSTEM',
    severity: 'warning',
    createdAt: '2026-06-15T13:40:00.000Z',
    isRead: false,
    isDeleted: false,
    isArchived: false,
    isPinned: true,
    link: { label: '查看維護細節', href: '/system-maintenance' },
    metadata: [
      { label: '維護窗口', value: '2026-06-16 02:00 - 04:00' },
      { label: '影響模組', value: '撮合、訂單、提現' }
    ]
  },
  {
    id: 'msg-002',
    title: 'BTC/USDT 永續上線',
    summary: '新上線永續合約，先行開啟測試交易',
    content: 'BTC/USDT 永續合約將於本週開放，開放名額將以先到先得為準，詳情請參考交易規則。',
    category: 'ANNOUNCEMENT',
    severity: 'info',
    createdAt: '2026-06-15T11:20:00.000Z',
    isRead: true,
    isDeleted: false,
    isArchived: false,
    isPinned: true,
    link: { label: '看規則', href: '/announcement/futures-launch' },
    metadata: [{ label: '交易對', value: 'BTC/USDT' }, { label: '類型', value: '永續' }]
  },
  {
    id: 'msg-003',
    title: 'BTC 買單已成交',
    summary: '訂單 #ORD-9001 已成交 0.1 BTC，成交價 65250 USDT',
    content:
      '您的 BTC 買單已成交。成交筆數 1，成交數量 0.10000000，成交價格 65,250.00 USDT。可在交易頁查閱完整明細。',
    category: 'TRADE',
    severity: 'success',
    createdAt: '2026-06-15T11:00:00.000Z',
    isRead: false,
    isDeleted: false,
    isArchived: false,
    isPinned: false,
    link: { label: '查看成交流', href: '/trade/ORD-9001' },
    metadata: [
      { label: '訂單 ID', value: 'ORD-9001' },
      { label: '成交 ID', value: 'TRD-1101' },
      { label: '數量', value: '0.10000000 BTC' },
      { label: '手續費', value: '0.0010 BTC' }
    ]
  },
  {
    id: 'msg-004',
    title: '限價單已部分成交',
    summary: '訂單 #ORD-9002 尚餘 30% 未成交',
    content: '您的限價單已成功部分成交，剩餘未成交量將繼續等待。',
    category: 'ORDER',
    severity: 'info',
    createdAt: '2026-06-15T10:38:00.000Z',
    isRead: false,
    isDeleted: false,
    isArchived: false,
    isPinned: false,
    metadata: [
      { label: '訂單 ID', value: 'ORD-9002' },
      { label: '交易對', value: 'ETH/USDT' },
      { label: '已成交', value: '70%' }
    ]
  },
  {
    id: 'msg-005',
    title: 'USDT 入金完成',
    summary: '入金筆數 #DEP-2201 已完成',
    content: '入金已入帳，可於資產頁查詢到最新餘額。請確認該筆資金用途與來源合規。',
    category: 'DEPOSIT',
    severity: 'success',
    createdAt: '2026-06-15T09:55:00.000Z',
    isRead: true,
    isDeleted: false,
    isArchived: false,
    isPinned: false,
    link: { label: '查看資產', href: '/assets' },
    metadata: [
      { label: '幣種', value: 'USDT' },
      { label: '數量', value: '1,000 USDT' },
      { label: '鏈別', value: 'TRC20' },
      { label: 'TxID', value: '0x7f3d...a2d' }
    ]
  },
  {
    id: 'msg-006',
    title: 'BTC 出金已提交',
    summary: '提現申請 #WD-3301 已送達風控',
    content: '提現申請已提交，待確認打包簽名與鏈上審核。若審核超時，將寄送進度更新。',
    category: 'WITHDRAWAL',
    severity: 'warning',
    createdAt: '2026-06-15T09:05:00.000Z',
    isRead: false,
    isDeleted: false,
    isArchived: false,
    isPinned: false,
    link: { label: '查看提現詳情', href: '/withdrawal/WD-3301' },
    metadata: [
      { label: '幣種', value: 'BTC' },
      { label: '數量', value: '0.42 BTC' },
      { label: '狀態', value: '風控中' }
    ]
  },
  {
    id: 'msg-007',
    title: '新裝置登入',
    summary: '您的帳號從新裝置登入，請確認是否為本人操作',
    content: '本次登入位於新 IP，若非本人，請立即修改密碼並聯繫客服。',
    category: 'SECURITY',
    severity: 'critical',
    createdAt: '2026-06-15T08:30:00.000Z',
    isRead: false,
    isDeleted: false,
    isArchived: false,
    isPinned: true,
    metadata: [
      { label: '登入時間', value: '2026-06-15 16:30 (UTC+8)' },
      { label: '登入地區', value: 'Singapore (SG)' },
      { label: '裝置', value: 'iOS App' }
    ]
  },
  {
    id: 'msg-008',
    title: 'KYC 審核未通過',
    summary: '身份驗證被退回，請補件後再次提交',
    content: '您的 KYC 審核未通過，請於 5 個工作日內補交清晰度更高的身份證明文件。',
    category: 'COMPLIANCE',
    severity: 'warning',
    createdAt: '2026-06-14T18:12:00.000Z',
    isRead: true,
    isDeleted: false,
    isArchived: false,
    isPinned: false,
    link: { label: '重新提交', href: '/kyc/resubmit' },
    metadata: [
      { label: '審核狀態', value: '未通過' },
      { label: '原因', value: '文件模糊，缺少清晰人臉特寫' }
    ]
  },
  {
    id: 'msg-009',
    title: '交易規則將調整保證金率',
    summary: 'BTC 合約保證金率將在本週五起調整',
    content: '為降低市場波動風險，BTC 合約保證金率將由 3% 調整為 4%。',
    category: 'ANNOUNCEMENT',
    severity: 'info',
    createdAt: '2026-06-14T16:02:00.000Z',
    isRead: true,
    isDeleted: false,
    isArchived: false,
    isPinned: false,
    link: { label: '閱讀規則', href: '/announcement/margin-update' },
    metadata: [
      { label: '影響商品', value: 'BTCUSDT 永續' },
      { label: '生效日', value: '2026-06-18' }
    ]
  },
  {
    id: 'msg-010',
    title: '活動：10USDT 手續費抵扣券',
    summary: '完成新手任務可獲得交易抵扣券',
    content: '活動截止前完成「完成首筆交易」可領 10 USDT 抵扣券，適用隔月帳單。',
    category: 'CAMPAIGN',
    severity: 'success',
    createdAt: '2026-06-14T12:00:00.000Z',
    isRead: false,
    isDeleted: true,
    isArchived: false,
    isPinned: false,
    metadata: [{ label: '優惠', value: '10 USDT 抵扣券' }]
  },
  {
    id: 'msg-011',
    title: '手機綁定成功',
    summary: '您的手機號碼已完成綁定',
    content: '手機綁定成功，可用於日後驗證與風險異常通知。',
    category: 'ACCOUNT',
    severity: 'success',
    createdAt: '2026-06-14T08:20:00.000Z',
    isRead: true,
    isDeleted: false,
    isArchived: true,
    isPinned: false,
    metadata: [{ label: '綁定時間', value: '2026-06-14 16:20' }]
  },
  {
    id: 'msg-012',
    title: '訂單已取消',
    summary: '訂單 #ORD-8988 已由系統自動取消',
    content: '該筆限價單因長時間未成交並超過風控規則停留時間，已自動取消。',
    category: 'ORDER',
    severity: 'warning',
    createdAt: '2026-06-14T05:35:00.000Z',
    isRead: false,
    isDeleted: false,
    isArchived: true,
    isPinned: false,
    link: { label: '查看訂單', href: '/orders/ORD-8988' },
    metadata: [
      { label: '訂單 ID', value: 'ORD-8988' },
      { label: '原因', value: '到期取消' }
    ]
  }
];

const INITIAL_SETTINGS: Record<MessageCategory, MessageNotificationSetting> = {
  SYSTEM: { inApp: true, email: true, sms: false, push: false, locks: { email: true, sms: true, push: true } },
  ANNOUNCEMENT: { inApp: true, email: true, sms: false, push: false, locks: { email: false, sms: false, push: false } },
  ORDER: { inApp: true, email: true, sms: true, push: true, locks: { email: false, sms: false, push: false } },
  TRADE: { inApp: true, email: true, sms: true, push: true, locks: { email: false, sms: false, push: false } },
  DEPOSIT: { inApp: true, email: true, sms: false, push: true, locks: { email: false, sms: true, push: false } },
  WITHDRAWAL: { inApp: true, email: true, sms: true, push: true, locks: { email: false, sms: false, push: false } },
  ACCOUNT: { inApp: true, email: true, sms: false, push: false, locks: { email: false, sms: true, push: true } },
  SECURITY: { inApp: true, email: true, sms: true, push: true, locks: { email: true, sms: true, push: true } },
  CAMPAIGN: { inApp: true, email: true, sms: false, push: true, locks: { email: false, sms: false, push: false } },
  COMPLIANCE: { inApp: true, email: true, sms: true, push: true, locks: { email: true, sms: true, push: true } }
};

const MAX_PAGE_SIZE = 8;

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

function normalizeFilterKey(raw: string): string {
  return raw.trim().toLowerCase();
}

export function App() {
  const [activeTab, setActiveTab] = useState<MessageTab>('all');
  const [categoryFilter, setCategoryFilter] = useState<MessageCategory | 'ALL'>('ALL');
  const [searchText, setSearchText] = useState('');
  const [selectedMessageId, setSelectedMessageId] = useState<string | null>('msg-001');
  const [pageSize, setPageSize] = useState<number>(MAX_PAGE_SIZE);
  const [viewMode, setViewMode] = useState<'list' | 'settings'>('list');
  const [saveFeedback, setSaveFeedback] = useState('');
  const [errorState, setErrorState] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [messages, setMessages] = useState<MessageItem[]>(INITIAL_MESSAGES);
  const [draftSettings, setDraftSettings] = useState<Record<MessageCategory, MessageNotificationSetting>>(INITIAL_SETTINGS);
  const [activeSettings, setActiveSettings] = useState<Record<MessageCategory, MessageNotificationSetting>>(INITIAL_SETTINGS);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 450);
    return () => {
      clearTimeout(timer);
    };
  }, []);

  const filterBase = useMemo(() => {
    const search = normalizeFilterKey(searchText);
    const now = messages.filter((msg) => {
      if (msg.isDeleted) {
        return false;
      }

      if (activeTab === 'unread' && (msg.isRead || msg.isArchived)) {
        return false;
      }

      if (activeTab === 'archived' && !msg.isArchived) {
        return false;
      }

      if (activeTab !== 'archived' && msg.isArchived) {
        return false;
      }

      if (categoryFilter !== 'ALL' && msg.category !== categoryFilter) {
        return false;
      }

      if (search) {
        const title = normalizeFilterKey(msg.title);
        const summary = normalizeFilterKey(msg.summary);
        const content = normalizeFilterKey(msg.content);
        return title.includes(search) || summary.includes(search) || content.includes(search);
      }

      return true;
    });

    return now.sort((a, b) => {
      if (a.isPinned !== b.isPinned) {
        return a.isPinned ? -1 : 1;
      }

      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }, [messages, activeTab, categoryFilter, searchText]);

  const visibleMessages = useMemo(() => filterBase.slice(0, pageSize), [filterBase, pageSize]);
  const hasMore = pageSize < filterBase.length;

  const unreadCountByCategory = useMemo(() => {
    const counters: Record<MessageCategory, number> = {
      SYSTEM: 0,
      ANNOUNCEMENT: 0,
      ORDER: 0,
      TRADE: 0,
      DEPOSIT: 0,
      WITHDRAWAL: 0,
      ACCOUNT: 0,
      SECURITY: 0,
      CAMPAIGN: 0,
      COMPLIANCE: 0
    };

    for (const item of messages) {
      if (item.isDeleted || item.isArchived || item.isRead) {
        continue;
      }
      counters[item.category] += 1;
    }
    return counters;
  }, [messages]);

  const unreadTotal = useMemo(() => Object.values(unreadCountByCategory).reduce((sum, count) => sum + count, 0), [unreadCountByCategory]);

  const selectedMessage = selectedMessageId ? messages.find((m) => m.id === selectedMessageId) ?? null : null;

  useEffect(() => {
    if (!selectedMessageId) {
      return;
    }
    if (!messages.some((msg) => msg.id === selectedMessageId && !msg.isDeleted)) {
      setSelectedMessageId(null);
    }
  }, [messages, selectedMessageId]);

  useEffect(() => {
    setPageSize(MAX_PAGE_SIZE);
  }, [activeTab, categoryFilter, searchText]);

  function markRead(messageId: string) {
    setMessages((prev) =>
      prev.map((msg) => (msg.id === messageId ? { ...msg, isRead: true } : msg))
    );
  }

  function markAllRead() {
    setMessages((prev) =>
      prev.map((msg) => {
        const shouldMark =
          !msg.isDeleted &&
          !msg.isArchived &&
          (activeTab === 'all' || activeTab === 'unread') &&
          !msg.isRead &&
          (categoryFilter === 'ALL' || msg.category === categoryFilter);
        return shouldMark ? { ...msg, isRead: true } : msg;
      })
    );
  }

  function archiveMessage(messageId: string) {
    setMessages((prev) =>
      prev.map((msg) => (msg.id === messageId ? { ...msg, isArchived: true } : msg))
    );
  }

  function deleteMessage(messageId: string) {
    setMessages((prev) =>
      prev.map((msg) => (msg.id === messageId ? { ...msg, isDeleted: true, isRead: true } : msg))
    );
  }

  function togglePin(messageId: string) {
    setMessages((prev) =>
      prev.map((msg) => (msg.id === messageId ? { ...msg, isPinned: !msg.isPinned } : msg))
    );
  }

  function applyMockError() {
    setErrorState('無法載入訊息設定，請稍後再試。');
    setSaveFeedback('');
  }

  function clearError() {
    setErrorState(null);
  }

  function onToggleChannel(category: MessageCategory, channel: 'email' | 'sms' | 'push') {
    setDraftSettings((prev) => {
      const item = prev[category];
      if (!item) {
        return prev;
      }
      if (item.locks[channel]) {
        return prev;
      }
      return {
        ...prev,
        [category]: { ...item, [channel]: !item[channel] }
      };
    });
  }

  function saveSettings() {
    clearError();
    setSaveFeedback('');
    if (draftSettings.SECURITY.email === false || draftSettings.SECURITY.push === false) {
      setErrorState('安全與法遵通知不可關閉站內外通知。');
      return;
    }
    if (draftSettings.COMPLIANCE.email === false || draftSettings.COMPLIANCE.push === false) {
      setErrorState('安全與法遵通知不可關閉站內外通知。');
      return;
    }
    setActiveSettings(draftSettings);
    setSaveFeedback('設定已儲存');
    setTimeout(() => setSaveFeedback(''), 1800);
  }

  function discardChanges() {
    setDraftSettings(activeSettings);
    setSaveFeedback('');
    setErrorState(null);
  }

  return (
    <div className="exchange-page">
      <header className="message-center-header">
        <div>
          <h1>訊息中心</h1>
          <p>集中管理公告、交易、資產與安全相關訊息</p>
        </div>
        <div className="message-center-badge">
          未讀：<strong>{unreadTotal}</strong>
          <span className="badge-dot">總</span>
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
                    placeholder="搜尋標題或內容"
                  />
                </div>

                <div className="message-category-grid">
                  {CATEGORY_OPTIONS.map((option) => {
                    const count =
                      option.value === 'ALL'
                        ? unreadTotal
                        : unreadCountByCategory[option.value as MessageCategory];
                    return (
                      <button
                        key={option.value}
                        type="button"
                        className={`category-chip ${categoryFilter === option.value ? 'active' : ''}`}
                        onClick={() => setCategoryFilter(option.value)}
                      >
                        {option.label}
                        {option.value === 'ALL' ? null : <span>{count}</span>}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="message-toolbar">
                <button type="button" onClick={() => markAllRead()}>
                  全部未讀標為已讀
                </button>
                <button type="button" onClick={() => {}}>
                  重新整理
                </button>
              </div>
            </section>

            <div className="message-center-grid">
              <section className="panel">
                <div className="panel-head">
                  <h2>
                    訊息列表 <span className="message-list-sub">{filterBase.length} 筆</span>
                  </h2>
                </div>
                <div className="message-list-shell">
                  {isLoading && (
                    <div className="message-skeleton">
                      <div className="skeleton-line" />
                      <div className="skeleton-line short" />
                      <div className="skeleton-line" />
                    </div>
                  )}
                  {!isLoading &&
                    !errorState &&
                    visibleMessages.map((msg) => {
                      const severity = SEVERITY[msg.severity];
                      return (
                        <article
                          key={msg.id}
                          className={`message-row ${msg.isRead ? '' : 'unread'} ${selectedMessageId === msg.id ? 'selected' : ''} tone-${severity.tone}`}
                          onClick={() => {
                            setSelectedMessageId(msg.id);
                          }}
                        >
                          <div className="message-row__head">
                            <div className="message-row__icon">
                              {CATEGORY_ICONS[msg.category]}
                            </div>
                            <div className="message-row__main">
                              <div className="message-row__title">
                                <span>{msg.title}</span>
                                {msg.isPinned && <span className="pin-badge">已釘選</span>}
                                {msg.category === 'SECURITY' || msg.category === 'COMPLIANCE' ? (
                                  <span className="pin-badge critical">高優先級</span>
                                ) : null}
                              </div>
                              <div className="message-row__summary">{msg.summary}</div>
                              <div className="message-row__meta">
                                <span>{CATEGORY_LABELS[msg.category]}</span>
                                <span className={`severity-badge ${severity.tone}`}>
                                  {severity.icon} {severity.label}
                                </span>
                                <span>{formatDate(msg.createdAt)}</span>
                              </div>
                            </div>
                          </div>
                          <div className="message-row__actions">
                            <button
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                markRead(msg.id);
                              }}
                              aria-label="標為已讀"
                            >
                              {msg.isRead ? '已讀' : '標為已讀'}
                            </button>
                            <button
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                togglePin(msg.id);
                              }}
                              aria-label="切換釘選"
                            >
                              {msg.isPinned ? '取消釘選' : '釘選'}
                            </button>
                            <button
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                archiveMessage(msg.id);
                              }}
                              aria-label="封存"
                            >
                              封存
                            </button>
                            <button
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                deleteMessage(msg.id);
                              }}
                              aria-label="刪除"
                            >
                              刪除
                            </button>
                          </div>
                        </article>
                      );
                    })}

                  {!isLoading && !errorState && visibleMessages.length === 0 ? (
                    <div className="message-empty">
                      目前沒有符合條件的訊息
                    </div>
                  ) : null}
                  {!isLoading && errorState ? <div className="message-empty error">{errorState}</div> : null}

                  {!isLoading && !errorState && hasMore ? (
                    <button type="button" className="load-more" onClick={() => setPageSize((prev) => prev + MAX_PAGE_SIZE)}>
                      載入更多
                    </button>
                  ) : null}
                </div>
              </section>

              <section className="panel message-detail-panel">
                <div className="panel-head">
                  <h2>訊息詳情</h2>
                </div>
                {!selectedMessage ? (
                  <div className="message-empty">請先選擇一則訊息</div>
                ) : (
                  <article className="message-detail">
                    <h3>{selectedMessage.title}</h3>
                    <p className="muted">{selectedMessage.summary}</p>
                    <div className="message-meta-grid">
                      <span>分類</span>
                      <strong>{CATEGORY_LABELS[selectedMessage.category]}</strong>
                      <span>重要度</span>
                      <strong>{SEVERITY[selectedMessage.severity].label}</strong>
                      <span>時間</span>
                      <strong>{formatDate(selectedMessage.createdAt)}</strong>
                      <span>狀態</span>
                      <strong>{selectedMessage.isRead ? '已讀' : '未讀'}</strong>
                      <span>釘選</span>
                      <strong>{selectedMessage.isPinned ? '是' : '否'}</strong>
                    </div>
                    <p className="message-detail-content">{selectedMessage.content}</p>
                    <ul>
                      {selectedMessage.metadata.map((item) => (
                        <li key={item.label}>
                          <strong>{item.label}</strong>
                          <span>{item.value}</span>
                        </li>
                      ))}
                    </ul>

                    {selectedMessage.link ? (
                      <a className="message-link" href={selectedMessage.link.href}>
                        {selectedMessage.link.label}
                      </a>
                    ) : null}

                    <div className="detail-actions">
                      <button
                        type="button"
                        onClick={() => {
                          markRead(selectedMessage.id);
                        }}
                      >
                        標記已讀
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          togglePin(selectedMessage.id);
                        }}
                      >
                        {selectedMessage.isPinned ? '取消釘選' : '釘選'}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          archiveMessage(selectedMessage.id);
                          setSelectedMessageId(null);
                        }}
                      >
                        封存
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          deleteMessage(selectedMessage.id);
                          setSelectedMessageId(null);
                        }}
                      >
                        刪除
                      </button>
                    </div>
                  </article>
                )}
              </section>
            </div>
          </section>
        ) : (
          <section className="panel message-settings-panel">
            <div className="panel-head">
              <h2>通知偏好</h2>
              <span>站內訊息一律保留，安全與法遵不可關閉 Email / SMS / Push。</span>
            </div>
            <p className="message-settings-error">{errorState}</p>
            <div className="message-settings-grid">
              <div className="message-settings-row header">
                <span>分類</span>
                <span>站內</span>
                <span>Email</span>
                <span>SMS</span>
                <span>Push</span>
              </div>
              {CHANNELS.map((channel) => {
                const setting = draftSettings[channel.value];
                return (
                  <div className="message-settings-row" key={channel.value}>
                    <span>{channel.label}</span>
                    <label>
                      <input type="checkbox" checked disabled />
                      開啟
                    </label>
                    <label>
                      <input
                        type="checkbox"
                        checked={setting.email}
                        disabled={setting.locks.email}
                        onChange={() => onToggleChannel(channel.value, 'email')}
                      />
                      {setting.locks.email ? '不可關閉' : '開啟'}
                    </label>
                    <label>
                      <input
                        type="checkbox"
                        checked={setting.sms}
                        disabled={setting.locks.sms}
                        onChange={() => onToggleChannel(channel.value, 'sms')}
                      />
                      {setting.locks.sms ? '不可關閉' : '開啟'}
                    </label>
                    <label>
                      <input
                        type="checkbox"
                        checked={setting.push}
                        disabled={setting.locks.push}
                        onChange={() => onToggleChannel(channel.value, 'push')}
                      />
                      {setting.locks.push ? '不可關閉' : '開啟'}
                    </label>
                  </div>
                );
              })}
            </div>
            <div className="detail-actions">
              <button type="button" onClick={saveSettings}>
                儲存設定
              </button>
              <button type="button" onClick={discardChanges}>
                放棄變更
              </button>
              <button type="button" onClick={applyMockError}>
                模擬 API 錯誤
              </button>
            </div>
            <div className="save-feedback">
              {saveFeedback ? <span className="save-success">{saveFeedback}</span> : null}
            </div>
            <p className="message-empty muted">
              測試提示：點擊「模擬 API 錯誤」可驗證錯誤狀態顯示，實際對接 API 後會被對應錯誤替換。
            </p>
          </section>
        )}
      </div>
    </div>
  );
}

export default App;
