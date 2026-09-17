import { Fragment, useEffect, useRef, useState, type ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import { Badge } from '../../../components/base/Badge';
import { Card } from '../../../components/base/Card';
import { ConfirmDialog } from '../../../components/base/ConfirmDialog';
import { HelpTooltip } from '../../../components/base/HelpTooltip';
import { ErrorState, LoadingState } from '../../../components/base/State';
import { PageHeader } from '../../../components/layout/PageHeader';
import { useI18n } from '../../../i18n';
import {
  fetchAdminConsoleMock,
  type AdminConsoleSnapshot,
  type AdminMarketMakerRecord,
  type AdminWalletRecord,
} from './mockAdminService';
import {
  findAdminUsers,
  getAdminUser,
  type AdminUser,
  type AdminUserDetail,
  type AdminUserSearch,
  type AdminUserSearchCursor,
} from '../../api/adminUsersApi';

type ConfirmState = {
  title: string;
  description: string;
  confirmLabel: string;
  action: () => void;
};

type UserDateFilter = 'created' | 'last-login';
type AdminUserVisualStatus = 'active' | 'inactive' | 'frozen';

export function AdminConsole() {
  const { t } = useI18n();
  const [data, setData] = useState<AdminConsoleSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirmState, setConfirmState] = useState<ConfirmState | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadAdminConsole() {
      // snapshot 只代表後台示意資料；實際操作必須改成 server-backed API 與審計紀錄。
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchAdminConsoleMock();
        if (alive) {
          setData(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load admin console.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadAdminConsole();

    return () => {
      alive = false;
    };
  }, []);

  function openConfirm(confirm: ConfirmState) {
    // 危險操作先進確認對話框，避免 demo 狀態下直接把 UI 行為誤認為已生效。
    setConfirmState(confirm);
  }

  function closeConfirm() {
    setConfirmState(null);
  }

  return (
    <div className="admin-console stack">
      <PageHeader title={t('admin.pageTitle')} description={t('admin.pageDescription')} />

      {loading ? <LoadingState title={t('admin.loadingTitle')} description={t('admin.loadingDescription')} /> : null}
      {error ? (
        <ErrorState
          title={t('admin.errorTitle')}
          description={error}
          action={
            <button
              className="secondary-button"
              type="button"
              onClick={() => {
                setLoading(true);
                setError(null);
                void fetchAdminConsoleMock()
                  .then((snapshot) => setData(snapshot))
                  .catch((loadError) => setError(loadError instanceof Error ? loadError.message : 'Unable to load admin console.'))
                  .finally(() => setLoading(false));
              }}
            >
              {t('common.retry')}
            </button>
          }
        />
      ) : null}

      {!loading && !error && data ? (
        <>
          <AdminAdapterNotice notice={data.adapterNotice} />

          <Routes>
            <Route index element={<AdminDashboardPage summary={data.summary} />} />
            <Route path="assets" element={<AdminAssetsPage assets={data.assets} />} />
            <Route path="wallet" element={<AdminWalletPage wallets={data.wallets} onPrompt={openConfirm} />} />
            <Route path="spot" element={<AdminSpotPage markets={data.spotMarkets} onPrompt={openConfirm} />} />
            <Route path="futures" element={<AdminFuturesPage markets={data.futuresMarkets} onPrompt={openConfirm} />} />
            <Route path="risk" element={<AdminRiskPage rules={data.riskRules} settings={data.settings} onPrompt={openConfirm} />} />
            <Route path="market-makers" element={<AdminMarketMakersPage makers={data.marketMakers} onPrompt={openConfirm} />} />
            <Route path="insurance-fund" element={<AdminInsuranceFundPage fund={data.insuranceFund} />} />
            <Route path="reconciliation" element={<AdminReconciliationPage records={data.reconciliation} />} />
            <Route path="operation-logs" element={<AdminOperationLogsPage logs={data.operationLogs} />} />
            <Route path="settings" element={<AdminSettingsPage settings={data.settings} onPrompt={openConfirm} />} />
            <Route path="*" element={<Navigate replace to="/" />} />
          </Routes>
        </>
      ) : null}

      <ConfirmDialog
        open={confirmState !== null}
        title={confirmState?.title ?? ''}
        description={confirmState?.description ?? ''}
        confirmLabel={confirmState?.confirmLabel ?? t('common.confirm')}
        cancelLabel={t('common.cancel')}
        note={t('admin.confirmSafetyNote')}
        onCancel={closeConfirm}
        onConfirm={() => {
          confirmState?.action();
          closeConfirm();
        }}
      />
    </div>
  );
}

function AdminAdapterNotice({ notice }: { notice: string }) {
  const { t } = useI18n();
  return (
    <Card title={t('admin.adapterNoticeTitle')}>
      <p>{notice}</p>
    </Card>
  );
}

function AdminDashboardPage({ summary }: { summary: AdminConsoleSnapshot['summary'] }) {
  const { t } = useI18n();
  return (
    <div className="stack">
      <Card title={t('admin.dashboardTitle')}>
        <div className="dashboard-grid">
          {summary.map((item) => (
            <div className="stat-card" key={item.label}>
              <span className="stat-card__label">{item.label}</span>
              <strong>{item.value}</strong>
              <p className="assets-metric__hint">{item.hint}</p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

/**
 * 使用者檢視獨立由外層 router 掛載，避免 AdminConsole 的巢狀 Routes 在 `/users` 再次附加路徑而沒有命中。
 * 其他 mock 模組仍暫留在 AdminConsole，這個真實唯讀頁不需要等待 mock snapshot 才能顯示。
 */
export function AdminUsersPage() {
  const { t } = useI18n();
  const [displayNamePrefix, setDisplayNamePrefix] = useState('');
  const [createdFromDate, setCreatedFromDate] = useState('');
  const [createdToDate, setCreatedToDate] = useState('');
  const [lastLoginFromDate, setLastLoginFromDate] = useState('');
  const [lastLoginToDate, setLastLoginToDate] = useState('');
  const [activeDateFilter, setActiveDateFilter] = useState<UserDateFilter | null>(null);
  const [items, setItems] = useState<AdminUser[]>([]);
  const [nextCursor, setNextCursor] = useState<AdminUserSearchCursor | null>(null);
  const [appliedSearch, setAppliedSearch] = useState<AdminUserSearch>({});
  const [expandedUserIds, setExpandedUserIds] = useState<Set<string>>(() => new Set());
  const [detailsByUserId, setDetailsByUserId] = useState<Record<string, AdminUserDetail>>({});
  const [detailLoadingUserIds, setDetailLoadingUserIds] = useState<Set<string>>(() => new Set());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const requestSequence = useRef(0);
  const hasCreatedDateFilter = Boolean(createdFromDate || createdToDate);
  const hasLastLoginDateFilter = Boolean(lastLoginFromDate || lastLoginToDate);

  function load(search: AdminUserSearch, append: boolean) {
    const requestId = ++requestSequence.current;
    setLoading(true);
    setError(null);
    if (!append) {
      // 新條件送出後先移除舊清單與游標，避免管理員誤把前一次篩選結果當成目前條件的資料。
      setItems([]);
      setNextCursor(null);
      // 篩選結果已變更，收合舊列詳情，避免不同查詢的資料在同一表格中混在一起。
      setExpandedUserIds(new Set());
      setDetailsByUserId({});
      setDetailLoadingUserIds(new Set());
    }

    // 使用者資料屬權限資料；快速連續篩選時，舊 request 的結果不能覆蓋最新條件的畫面。
    void findAdminUsers(search)
      .then((page) => {
        if (requestId !== requestSequence.current) return;
        setItems((current) => append ? [...current, ...page.items] : page.items);
        setNextCursor(page.nextCursor);
      })
      .catch(() => {
        if (requestId !== requestSequence.current) return;
        setError(t('admin.usersQueryError'));
      })
      .finally(() => {
        if (requestId === requestSequence.current) setLoading(false);
      });
  }

  useEffect(() => {
    load({}, false);
  }, []);

  function buildSearch(cursor?: AdminUserSearchCursor): AdminUserSearch {
    const search: AdminUserSearch = {
      displayNamePrefix: displayNamePrefix.trim() || undefined,
      cursor,
    };
    if (createdFromDate) search.createdFrom = toUtcDayStart(createdFromDate);
    if (createdToDate) search.createdBefore = toUtcDayAfter(createdToDate);
    if (lastLoginFromDate) search.lastLoginFrom = toUtcDayStart(lastLoginFromDate);
    if (lastLoginToDate) search.lastLoginBefore = toUtcDayAfter(lastLoginToDate);
    return search;
  }

  function submitSearch() {
    if (createdFromDate && createdToDate && createdFromDate > createdToDate) {
      setError(t('admin.usersInvalidDateRange'));
      return;
    }
    if (lastLoginFromDate && lastLoginToDate && lastLoginFromDate > lastLoginToDate) {
      setError(t('admin.usersInvalidLastLoginRange'));
      return;
    }
    const search = buildSearch();
    setAppliedSearch(search);
    load(search, false);
  }

  function resetSearch() {
    setDisplayNamePrefix('');
    setCreatedFromDate('');
    setCreatedToDate('');
    setLastLoginFromDate('');
    setLastLoginToDate('');
    setActiveDateFilter(null);
    setAppliedSearch({});
    load({}, false);
  }

  function toggleDateFilter(filter: UserDateFilter) {
    setActiveDateFilter((current) => current === filter ? null : filter);
  }

  function clearActiveDateFilter() {
    if (activeDateFilter === 'created') {
      setCreatedFromDate('');
      setCreatedToDate('');
    }
    if (activeDateFilter === 'last-login') {
      setLastLoginFromDate('');
      setLastLoginToDate('');
    }
  }

  function toggleDetail(userId: string) {
    if (expandedUserIds.has(userId)) {
      setExpandedUserIds((current) => {
        const next = new Set(current);
        next.delete(userId);
        return next;
      });
      return;
    }

    setExpandedUserIds((current) => new Set(current).add(userId));
    if (detailsByUserId[userId] || detailLoadingUserIds.has(userId)) return;

    setDetailLoadingUserIds((current) => new Set(current).add(userId));
    // 每列的詳情獨立快取與載入，讓管理員能同時展開多位使用者，而不會互相覆蓋。
    void getAdminUser(userId)
      .then((userDetail) => setDetailsByUserId((current) => ({ ...current, [userId]: userDetail })))
      .catch(() => setError(t('admin.usersQueryError')))
      .finally(() => {
        setDetailLoadingUserIds((current) => {
          const next = new Set(current);
          next.delete(userId);
          return next;
        });
      });
  }

  return (
    <Card className="admin-users-card" title={t('admin.usersTitle')}>
      <form className="admin-user-search" onSubmit={(event) => {
        event.preventDefault();
        submitSearch();
      }}>
        <label className="admin-user-search__query">
          <span className="sr-only">{t('admin.usersNamePrefix')}</span>
          <SearchIcon />
          <input
            className="input"
            value={displayNamePrefix}
            onChange={(event) => setDisplayNamePrefix(event.target.value)}
            maxLength={128}
            placeholder={t('admin.usersNamePrefixPlaceholder')}
          />
        </label>
        <div className="admin-user-search__filters" aria-label={t('admin.usersDateFilters')}>
          <button
            className={`admin-user-filter-chip${activeDateFilter === 'created' ? ' admin-user-filter-chip--active' : ''}`}
            type="button"
            aria-expanded={activeDateFilter === 'created'}
            onClick={() => toggleDateFilter('created')}
          >
            <span>{t('admin.usersRegistrationFilter')}</span>
            {hasCreatedDateFilter ? <strong>{formatDateRangeSummary(createdFromDate, createdToDate, t('admin.usersAllDates'))}</strong> : null}
            <span aria-hidden="true">⌄</span>
          </button>
          <button
            className={`admin-user-filter-chip${activeDateFilter === 'last-login' ? ' admin-user-filter-chip--active' : ''}`}
            type="button"
            aria-expanded={activeDateFilter === 'last-login'}
            onClick={() => toggleDateFilter('last-login')}
          >
            <span>{t('admin.usersLastLoginFilter')}</span>
            {hasLastLoginDateFilter ? <strong>{formatDateRangeSummary(lastLoginFromDate, lastLoginToDate, t('admin.usersAllDates'))}</strong> : null}
            <span aria-hidden="true">⌄</span>
          </button>
          <HelpTooltip message={t('admin.usersSearchHint')} label={t('admin.usersSearchHelpLabel')} />
        </div>
        <div className="admin-user-search__actions">
          <button className="primary-button" disabled={loading}>{loading ? t('admin.usersSearching') : t('admin.usersSearch')}</button>
          <button className="ghost-button" type="button" disabled={loading} onClick={resetSearch}>{t('admin.usersReset')}</button>
        </div>
        {activeDateFilter ? (
          <section className="admin-user-date-panel" aria-label={activeDateFilter === 'created' ? t('admin.usersRegistrationFilter') : t('admin.usersLastLoginFilter')}>
            <div className="admin-user-date-panel__header">
              <strong>{activeDateFilter === 'created' ? t('admin.usersRegistrationFilter') : t('admin.usersLastLoginFilter')}</strong>
              <button
                className="admin-user-date-panel__clear"
                type="button"
                aria-label={t('admin.usersClearThisFilter')}
                title={t('admin.usersClearThisFilter')}
                onClick={clearActiveDateFilter}
              >
                <CloseIcon />
              </button>
            </div>
            <div className="admin-user-date-panel__range">
              <label className="field">
                <span className="field__label">{t('admin.usersRangeFrom')}</span>
                <input
                  className="input"
                  type="date"
                  value={activeDateFilter === 'created' ? createdFromDate : lastLoginFromDate}
                  onChange={(event) => activeDateFilter === 'created' ? setCreatedFromDate(event.target.value) : setLastLoginFromDate(event.target.value)}
                />
              </label>
              <span className="admin-user-date-panel__separator" aria-hidden="true">→</span>
              <label className="field">
                <span className="field__label">{t('admin.usersRangeTo')}</span>
                <input
                  className="input"
                  type="date"
                  value={activeDateFilter === 'created' ? createdToDate : lastLoginToDate}
                  onChange={(event) => activeDateFilter === 'created' ? setCreatedToDate(event.target.value) : setLastLoginToDate(event.target.value)}
                />
              </label>
            </div>
          </section>
        ) : null}
      </form>
      {error ? <p className="form-message form-message--error">{error}</p> : null}
      <AdminTable
        className="admin-users-table"
        columns={[
          t('admin.column.user'), t('admin.column.registeredAt'), t('admin.column.lastLogin'), t('admin.column.actions'),
        ]}
      >
        {items.map((user) => {
          const visualStatus = getAdminUserVisualStatus(user);
          const statusLabel = getAdminUserStatusLabel(visualStatus, t);
          const expanded = expandedUserIds.has(user.userId);
          const detail = detailsByUserId[user.userId];
          const detailLoading = detailLoadingUserIds.has(user.userId);
          const detailId = `admin-user-detail-${user.userId}`;
          const restrictionMessages = getAdminUserRestrictionMessages(user, t);

          return (
            <Fragment key={user.userId}>
              <AdminTableRow>
                <div className="admin-user-identity">
                  <span
                    className={`admin-user-identity__status admin-user-identity__status--${visualStatus}`}
                    role="img"
                    aria-label={statusLabel}
                    title={statusLabel}
                  />
                  <div>
                    <strong>
                      {user.displayName} <span className="admin-user-identity__email">({user.email})</span>
                      <CopyButton value={user.email} label={t('admin.usersCopyEmail')} />
                    </strong>
                    <p className="assets-metric__hint">
                      {user.userId}<CopyButton value={user.userId} label={t('admin.usersCopyUserId')} />
                    </p>
                  </div>
                </div>
                <span>{formatTime(user.createdAt)}</span>
                <span>{user.lastLoginAt ? formatTime(user.lastLoginAt) : '—'}</span>
                <button
                  className="secondary-button"
                  type="button"
                  aria-expanded={expanded}
                  aria-controls={detailId}
                  onClick={() => toggleDetail(user.userId)}
                >
                  {expanded ? t('admin.usersHideDetails') : t('admin.usersShowDetails')}
                </button>
              </AdminTableRow>
              {expanded ? (
                <section className="admin-user-detail" id={detailId} aria-label={t('admin.usersDetailsFor', undefined, { name: user.displayName })}>
                  <div className="admin-user-detail__panel">
                    {detailLoading ? <p className="assets-metric__hint">{t('admin.usersDetailsLoading')}</p> : null}
                    {!detailLoading && detail ? (
                      <dl className="admin-user-detail__fields">
                        <div className="admin-user-detail__access-status">
                          <dt>{t('admin.usersDetailStatus')}</dt>
                          <dd>
                            <span className={`admin-user-identity__status admin-user-identity__status--${visualStatus}`} aria-hidden="true" />
                            {statusLabel}
                          </dd>
                        </div>
                        <div className={`admin-user-detail__restrictions${user.hasActiveRestriction ? '' : ' admin-user-detail__restrictions--clear'}`}>
                          <dt>{t('admin.usersDetailRestrictions')}</dt>
                          <dd>
                            <ul>
                              {restrictionMessages.map((message) => <li key={message}>{message}</li>)}
                            </ul>
                          </dd>
                        </div>
                        <div>
                          <dt>{t('admin.column.id')}</dt>
                          <dd>{detail.user.userId}<CopyButton value={detail.user.userId} label={t('admin.usersCopyUserId')} /></dd>
                        </div>
                        <div>
                          <dt>{t('admin.account.email')}</dt>
                          <dd>{detail.user.email}<CopyButton value={detail.user.email} label={t('admin.usersCopyEmail')} /></dd>
                        </div>
                        <div>
                          <dt>{t('admin.usersDetailRegistered')}</dt>
                          <dd>{formatTime(detail.user.createdAt)}</dd>
                        </div>
                        <div>
                          <dt>{t('admin.usersDetailLastLogin')}</dt>
                          <dd>{detail.user.lastLoginAt ? formatTime(detail.user.lastLoginAt) : '—'}</dd>
                        </div>
                        <div className="admin-user-detail__devices">
                          <dt>{t('admin.usersDetailDevices')}</dt>
                          <dd>{detail.devices.map((device) => `${device.platform}／${device.label}`).join('、') || t('admin.usersDetailNoDevices')}</dd>
                        </div>
                      </dl>
                    ) : null}
                    {!detailLoading && !detail ? <p className="form-message form-message--error">{t('admin.usersQueryError')}</p> : null}
                  </div>
                </section>
              ) : null}
            </Fragment>
          );
        })}
      </AdminTable>
      {!loading && items.length === 0 ? <p className="assets-metric__hint">{t('admin.usersEmpty')}</p> : null}
      {nextCursor ? (
        <button
          className="secondary-button admin-user-search__more"
          type="button"
          disabled={loading}
          onClick={() => load({ ...appliedSearch, cursor: nextCursor }, true)}
        >
          {loading ? t('admin.usersSearching') : t('admin.usersLoadMore')}
        </button>
      ) : null}
    </Card>
  );
}

function AdminAssetsPage({ assets }: { assets: AdminConsoleSnapshot['assets'] }) {
  const { t } = useI18n();
  return (
    <Card title={t('admin.assetsTitle')}>
      <AdminTable columns={[t('admin.column.asset'), t('admin.column.spot'), t('admin.column.futures'), t('admin.column.frozen'), t('admin.column.ledgerDelta')]}>
        {assets.map((asset) => (
          <AdminTableRow key={asset.asset}>
            <strong>{asset.asset}</strong>
            <span>{asset.spotBalance}</span>
            <span>{asset.futuresBalance}</span>
            <span>{asset.frozenBalance}</span>
            <span>{asset.ledgerDelta}</span>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminWalletPage({ wallets, onPrompt }: { wallets: AdminWalletRecord[]; onPrompt: (confirm: ConfirmState) => void }) {
  const { t } = useI18n();
  const [items, setItems] = useState(wallets);

  useEffect(() => {
    setItems(wallets);
  }, [wallets]);

  function promptReview(wallet: AdminWalletRecord, nextStatus: 'Approved' | 'Rejected') {
    onPrompt({
      title: `${wallet.id} · ${nextStatus === 'Approved' ? t('admin.wallet.approveTitle') : t('admin.wallet.rejectTitle')}`,
      description: t('admin.wallet.reviewDescription', undefined, {
        user: wallet.user,
        asset: wallet.asset,
        amount: wallet.amount,
        status: nextStatus.toLowerCase(),
      }),
      confirmLabel: nextStatus === 'Approved' ? t('admin.wallet.approve') : t('admin.wallet.reject'),
      action: () => setItems((current) => current.map((item) => (item.id === wallet.id ? { ...item, status: nextStatus } : item))),
    });
  }

  return (
    <Card title={t('admin.walletTitle')}>
      <AdminTable columns={[t('admin.column.id'), t('admin.column.user'), t('admin.column.type'), t('admin.column.asset'), t('admin.column.network'), t('admin.column.amount'), t('admin.column.risk'), t('admin.column.status'), t('admin.column.actions')]}>
        {items.map((wallet) => (
          <AdminTableRow key={wallet.id}>
            <span>{wallet.id}</span>
            <span>{wallet.user}</span>
            <Badge tone={wallet.type === 'Deposit' ? 'success' : 'warning'}>{wallet.type}</Badge>
            <span>{wallet.asset}</span>
            <span>{wallet.network}</span>
            <strong>{wallet.amount}</strong>
            <Badge tone={getRiskTone(wallet.risk)}>{wallet.risk}</Badge>
            <Badge tone={getStatusTone(wallet.status)}>{wallet.status}</Badge>
            <div className="hero-actions">
              <button className="secondary-button" type="button" onClick={() => promptReview(wallet, 'Approved')}>
                {t('admin.wallet.approve')}
              </button>
              <button className="ghost-button" type="button" onClick={() => promptReview(wallet, 'Rejected')}>
                {t('admin.wallet.reject')}
              </button>
            </div>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminSpotPage({ markets, onPrompt }: { markets: AdminConsoleSnapshot['spotMarkets']; onPrompt: (confirm: ConfirmState) => void }) {
  const { t } = useI18n();
  const [items, setItems] = useState(markets);

  useEffect(() => {
    setItems(markets);
  }, [markets]);

  function promptToggle(pair: (typeof markets)[number]) {
    const nextStatus = pair.status === 'Paused' ? 'Active' : pair.status === 'Active' ? 'Paused' : 'Active';
    onPrompt({
      title: `${pair.pair} · ${nextStatus === 'Paused' ? t('admin.spot.pauseTitle') : t('admin.spot.resumeTitle')}`,
      description: t('admin.spot.toggleDescription', undefined, { status: nextStatus.toLowerCase() }),
      confirmLabel: nextStatus === 'Paused' ? t('admin.spot.pause') : t('admin.spot.resume'),
      action: () => setItems((current) => current.map((item) => (item.pair === pair.pair ? { ...item, status: nextStatus } : item))),
    });
  }

  return (
    <Card title={t('admin.spotTitle')}>
      <AdminTable columns={[t('admin.column.pair'), t('admin.column.status'), t('admin.column.volume24h'), t('admin.column.fee'), t('admin.column.orders'), t('admin.column.action')]}>
        {items.map((pair) => (
          <AdminTableRow key={pair.pair}>
            <strong>{pair.pair}</strong>
            <Badge tone={getStatusTone(pair.status)}>{pair.status}</Badge>
            <span>{pair.volume24h}</span>
            <span>{pair.feeRate}</span>
            <span>{pair.orderCount}</span>
            <button className="secondary-button" type="button" onClick={() => promptToggle(pair)}>
              {pair.actionLabel}
            </button>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminFuturesPage({ markets, onPrompt }: { markets: AdminConsoleSnapshot['futuresMarkets']; onPrompt: (confirm: ConfirmState) => void }) {
  const { t } = useI18n();
  const [items, setItems] = useState(markets);

  useEffect(() => {
    setItems(markets);
  }, [markets]);

  function promptToggle(symbol: (typeof markets)[number]) {
    let nextStatus: (typeof symbol.status);
    if (symbol.status === 'Paused') {
      nextStatus = 'Active';
    } else if (symbol.status === 'Active') {
      nextStatus = 'Reduce only';
    } else {
      nextStatus = 'Active';
    }
    onPrompt({
      title: `${symbol.symbol} · ${t('admin.futures.toggleTitle')}`,
      description: t('admin.futures.toggleDescription', undefined, { status: nextStatus.toLowerCase() }),
      confirmLabel: nextStatus === 'Reduce only' ? t('admin.futures.reduceOnly') : t('admin.futures.resume'),
      action: () => setItems((current) => current.map((item) => (item.symbol === symbol.symbol ? { ...item, status: nextStatus } : item))),
    });
  }

  return (
    <Card title={t('admin.futuresTitle')}>
      <AdminTable columns={[t('admin.column.symbol'), t('admin.column.status'), t('admin.column.oi'), t('admin.column.funding'), t('admin.column.liquidations'), t('admin.column.markPrice'), t('admin.column.actions')]}>
        {items.map((symbol) => (
          <AdminTableRow key={symbol.symbol}>
            <strong>{symbol.symbol}</strong>
            <Badge tone={getStatusTone(symbol.status)}>{symbol.status}</Badge>
            <span>{symbol.openInterest}</span>
            <span>{symbol.fundingRate}</span>
            <span>{symbol.liquidationCount}</span>
            <span>{symbol.markPrice}</span>
            <button className="secondary-button" type="button" onClick={() => promptToggle(symbol)}>
              Toggle mode
            </button>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminRiskPage({
  rules,
  settings,
  onPrompt,
}: {
  rules: AdminConsoleSnapshot['riskRules'];
  settings: AdminConsoleSnapshot['settings'];
  onPrompt: (confirm: ConfirmState) => void;
}) {
  const { t } = useI18n();
  return (
    <div className="stack">
      <Card title={t('admin.riskTitle')}>
        <div className="dashboard-grid dashboard-grid--three">
          <RiskSwitch
            label={t('admin.risk.killSwitch')}
            enabled={settings.killSwitch}
            onClick={() =>
              onPrompt({
                title: t('admin.risk.killSwitchToggleTitle'),
                description: t('admin.risk.mockOnlyDescription'),
                confirmLabel: settings.killSwitch ? t('admin.risk.disable') : t('admin.risk.enable'),
                action: () => undefined,
              })
            }
          />
          <RiskSwitch
            label={t('admin.risk.withdrawPause')}
            enabled={settings.withdrawPause}
            onClick={() =>
              onPrompt({
                title: t('admin.risk.withdrawPauseToggleTitle'),
                description: t('admin.risk.mockOnlyDescription'),
                confirmLabel: settings.withdrawPause ? t('admin.risk.resume') : t('admin.risk.pause'),
                action: () => undefined,
              })
            }
          />
          <RiskSwitch
            label={t('admin.risk.reduceOnly')}
            enabled={settings.futuresReduceOnly}
            onClick={() =>
              onPrompt({
                title: t('admin.risk.reduceOnlyToggleTitle'),
                description: t('admin.risk.mockOnlyDescription'),
                confirmLabel: settings.futuresReduceOnly ? t('admin.risk.disable') : t('admin.risk.enable'),
                action: () => undefined,
              })
            }
          />
        </div>
      </Card>

      <Card title={t('admin.riskRulesTitle')}>
        <AdminTable columns={[t('admin.column.rule'), t('admin.column.scope'), t('admin.column.threshold'), t('admin.column.status')]}>
          {rules.map((rule) => (
            <AdminTableRow key={rule.name}>
              <strong>{rule.name}</strong>
              <span>{rule.scope}</span>
              <span>{rule.threshold}</span>
              <Badge tone={rule.status === 'Enabled' ? 'success' : 'warning'}>{rule.status}</Badge>
            </AdminTableRow>
          ))}
        </AdminTable>
      </Card>

      <Card title={t('admin.maintenanceTitle')}>
        <p>{settings.note}</p>
        <p className="assets-metric__hint">{t('admin.maintenanceWindow', undefined, { window: settings.maintenanceWindow })}</p>
      </Card>
    </div>
  );
}

function AdminMarketMakersPage({
  makers,
  onPrompt,
}: {
  makers: AdminConsoleSnapshot['marketMakers'];
  onPrompt: (confirm: ConfirmState) => void;
}) {
  const { t } = useI18n();
  const [items, setItems] = useState(makers);

  useEffect(() => {
    setItems(makers);
  }, [makers]);

  function promptToggle(maker: AdminMarketMakerRecord) {
    const nextStatus = maker.status === 'Active' ? 'Disabled' : 'Active';
    onPrompt({
      title: `${maker.name} · ${nextStatus === 'Disabled' ? t('admin.marketMakers.disableTitle') : t('admin.marketMakers.enableTitle')}`,
      description: t('admin.marketMakers.toggleDescription', undefined, { apiKey: maker.apiKey, status: nextStatus.toLowerCase() }),
      confirmLabel: nextStatus === 'Disabled' ? t('admin.marketMakers.disable') : t('admin.marketMakers.enable'),
      action: () => setItems((current) => current.map((item) => (item.apiKey === maker.apiKey ? { ...item, status: nextStatus } : item))),
    });
  }

  return (
    <Card title={t('admin.marketMakersTitle')}>
      <AdminTable columns={[t('admin.column.name'), t('admin.column.apiKey'), t('admin.column.status'), t('admin.column.volume'), t('admin.column.pnl'), t('admin.column.heartbeat'), t('admin.column.actions')]}>
        {items.map((maker) => (
          <AdminTableRow key={maker.apiKey}>
            <strong>{maker.name}</strong>
            <span>{maker.apiKey}</span>
            <Badge tone={maker.status === 'Active' ? 'success' : 'warning'}>{maker.status}</Badge>
            <span>{maker.dailyVolume}</span>
            <span>{maker.pnl}</span>
            <span>{formatTime(maker.lastHeartbeat)}</span>
            <button className="secondary-button" type="button" onClick={() => promptToggle(maker)}>
              {maker.status === 'Active' ? t('admin.marketMakers.disable') : t('admin.marketMakers.enable')}
            </button>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminInsuranceFundPage({ fund }: { fund: AdminConsoleSnapshot['insuranceFund'] }) {
  const { t } = useI18n();
  return (
    <Card title={t('admin.insuranceFundTitle')}>
      <AdminTable columns={[t('admin.column.currency'), t('admin.column.balance'), t('admin.column.dailyChange'), t('admin.column.lastTransfer')]}>
        {fund.map((item) => (
          <AdminTableRow key={item.currency}>
            <strong>{item.currency}</strong>
            <span>{item.balance}</span>
            <span>{item.dailyChange}</span>
            <span>{formatTime(item.lastTransferAt)}</span>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminReconciliationPage({ records }: { records: AdminConsoleSnapshot['reconciliation'] }) {
  const { t } = useI18n();
  return (
    <Card title={t('admin.reconciliationTitle')}>
      <AdminTable columns={[t('admin.column.date'), t('admin.column.status'), t('admin.column.matched'), t('admin.column.mismatch'), t('admin.column.note')]}>
        {records.map((record) => (
          <AdminTableRow key={record.date}>
            <strong>{record.date}</strong>
            <Badge tone={getStatusTone(record.status)}>{record.status}</Badge>
            <span>{record.matched}</span>
            <span>{record.mismatch}</span>
            <span>{record.note}</span>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminOperationLogsPage({ logs }: { logs: AdminConsoleSnapshot['operationLogs'] }) {
  const { t } = useI18n();
  return (
    <Card title={t('admin.operationLogsTitle')}>
      <AdminTable columns={[t('admin.column.time'), t('admin.column.actor'), t('admin.column.action'), t('admin.column.target'), t('admin.column.result'), t('admin.column.risk')]}>
        {logs.map((log) => (
          <AdminTableRow key={`${log.time}-${log.actor}`}>
            <span>{formatTime(log.time)}</span>
            <span>{log.actor}</span>
            <span>{log.action}</span>
            <span>{log.target}</span>
            <span>{log.result}</span>
            <Badge tone={getRiskTone(log.risk)}>{log.risk}</Badge>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminSettingsPage({
  settings,
  onPrompt,
}: {
  settings: AdminConsoleSnapshot['settings'];
  onPrompt: (confirm: ConfirmState) => void;
}) {
  const { t } = useI18n();
  return (
    <div className="stack">
      <Card title={t('admin.settingsTitle')}>
        <div className="dashboard-grid dashboard-grid--three">
          <RiskSwitch
            label={t('admin.settings.apiWithdraw')}
            enabled={settings.apiWithdrawEnabled}
            onClick={() =>
              onPrompt({
                title: t('admin.settings.apiWithdrawTitle'),
                description: t('admin.risk.mockOnlyDescription'),
                confirmLabel: settings.apiWithdrawEnabled ? t('admin.risk.disable') : t('admin.risk.enable'),
                action: () => undefined,
              })
            }
          />
          <RiskSwitch
            label={t('admin.settings.spotPause')}
            enabled={settings.spotPause}
            onClick={() =>
              onPrompt({
                title: t('admin.settings.spotPauseTitle'),
                description: t('admin.risk.mockOnlyDescription'),
                confirmLabel: settings.spotPause ? t('admin.risk.resume') : t('admin.risk.pause'),
                action: () => undefined,
              })
            }
          />
          <RiskSwitch
            label={t('admin.settings.internalMmStop')}
            enabled={settings.internalMmStopped}
            onClick={() =>
              onPrompt({
                title: t('admin.settings.internalMmStopTitle'),
                description: t('admin.risk.mockOnlyDescription'),
                confirmLabel: settings.internalMmStopped ? t('admin.risk.resume') : t('admin.risk.stop'),
                action: () => undefined,
              })
            }
          />
        </div>
      </Card>

      <Card title={t('admin.systemNoteTitle')}>
        <p>{settings.note}</p>
      </Card>
    </div>
  );
}

function AdminTable({ columns, children, className = '' }: { columns: string[]; children: ReactNode; className?: string }) {
  return (
    <div className={['trading-table', className].filter(Boolean).join(' ')}>
      <div className="trading-table__head">
        {columns.map((column) => (
          <span key={column}>{column}</span>
        ))}
      </div>
      {children}
    </div>
  );
}

function AdminTableRow({ children }: { children: ReactNode }) {
  return <div className="trading-table__row">{children}</div>;
}

function RiskSwitch({ label, enabled, onClick }: { label: string; enabled: boolean; onClick: () => void }) {
  const { t } = useI18n();
  return (
    <div className="card">
      <div className="notice-row">
        <strong>{label}</strong>
        <Badge tone={enabled ? 'success' : 'warning'}>{enabled ? t('admin.state.enabled') : t('admin.state.disabled')}</Badge>
      </div>
      <p className="assets-metric__hint">{t('admin.risk.switchHint')}</p>
      <button className="secondary-button" type="button" onClick={onClick}>
        {t('admin.risk.openConfirmation')}
      </button>
    </div>
  );
}

function getStatusTone(status: string) {
  if (status === 'ACTIVE' || status === 'Active' || status === 'Matched' || status === 'Approved' || status === 'Enabled') return 'success';
  if (status === 'SUSPENDED' || status === 'Paused' || status === 'Investigating' || status === 'KYC Pending' || status === 'Reduce only') return 'warning';
  return 'danger';
}

/**
 * 限制狀態由後端聚合帳號、資金與帳戶凍結來源；前端只負責呈現，避免用到期時間等欄位自行推測。
 */
function getAdminUserVisualStatus(user: AdminUser): AdminUserVisualStatus {
  if (user.hasActiveRestriction) return 'frozen';
  return user.status === 'ACTIVE' ? 'active' : 'inactive';
}

function getAdminUserStatusLabel(status: AdminUserVisualStatus, t: ReturnType<typeof useI18n>['t']) {
  if (status === 'active') return t('admin.usersStatusActive');
  if (status === 'frozen') return t('admin.usersStatusFrozen');
  return t('admin.usersStatusInactive');
}

/** 將後端已聚合的限制轉成可審閱的文字，避免管理員只看見黃色色點卻不知道限制範圍。 */
function getAdminUserRestrictionMessages(user: AdminUser, t: ReturnType<typeof useI18n>['t']) {
  const messages: string[] = [];
  if (user.status === 'SUSPENDED') messages.push(t('admin.usersRestrictionSuspended'));
  if (hasActiveFundTransferRestriction(user)) {
    messages.push(t('admin.usersRestrictionFundTransfer', undefined, { until: formatTime(user.fundTransferRestrictedUntil!) }));
  }
  if (user.hasActiveRestriction && messages.length === 0) messages.push(t('admin.usersRestrictionFrozenAccount'));
  if (messages.length === 0) messages.push(t('admin.usersNoRestrictions'));
  return messages;
}

function hasActiveFundTransferRestriction(user: AdminUser) {
  const restrictedUntil = user.fundTransferRestrictedUntil ? Date.parse(user.fundTransferRestrictedUntil) : Number.NaN;
  return Number.isFinite(restrictedUntil) && restrictedUntil > Date.now();
}

function getRiskTone(risk: string) {
  if (risk === 'Low') return 'success';
  if (risk === 'Medium') return 'warning';
  return 'danger';
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'UTC',
  }).format(new Date(value));
}

/** date input 沒有時區資訊，統一轉為 UTC 的半開區間，才能穩定涵蓋管理員選取的完整日期。 */
function toUtcDayStart(date: string) {
  return `${date}T00:00:00.000Z`;
}

function toUtcDayAfter(date: string) {
  const endOfSelectedDay = new Date(`${date}T00:00:00.000Z`);
  endOfSelectedDay.setUTCDate(endOfSelectedDay.getUTCDate() + 1);
  return endOfSelectedDay.toISOString();
}

function formatDateRangeSummary(from: string, to: string, allDatesLabel: string) {
  if (!from && !to) return allDatesLabel;
  return `${from || '…'} – ${to || '…'}`;
}

function SearchIcon() {
  return (
    <svg className="admin-user-search__query-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="10.8" cy="10.8" r="5.8" />
      <path d="m15.1 15.1 4.3 4.3" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="m7.5 7.5 9 9M16.5 7.5l-9 9" />
    </svg>
  );
}

/**
 * 電子郵件與使用者 ID 常需貼到支援或稽核工具；集中處理複製與回饋可避免每個欄位各自實作不一致的行為。
 */
function CopyButton({ value, label }: { value: string; label: string }) {
  const { t } = useI18n();
  const [copied, setCopied] = useState(false);
  const resetTimer = useRef<number | null>(null);

  async function copy() {
    const copiedSuccessfully = await copyToClipboard(value);
    if (!copiedSuccessfully) return;

    setCopied(true);
    if (resetTimer.current !== null) window.clearTimeout(resetTimer.current);
    resetTimer.current = window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <button
      className="admin-user-copy-button"
      type="button"
      aria-label={copied ? t('admin.usersCopied') : label}
      title={copied ? t('admin.usersCopied') : label}
      onClick={() => void copy()}
    >
      {copied ? <CheckIcon /> : <CopyIcon />}
      <span className="sr-only" aria-live="polite">{copied ? t('admin.usersCopied') : ''}</span>
    </button>
  );
}

/** Clipboard API 在 HTTPS / localhost 可用；舊瀏覽器才退回到同步選取方式，讓管理操作不因環境差異失效。 */
async function copyToClipboard(value: string) {
  try {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(value);
      return true;
    }
  } catch {
    // Clipboard 權限遭拒時仍嘗試相容 fallback，不將例外暴露成使用者看不懂的 console error。
  }

  const textArea = document.createElement('textarea');
  textArea.value = value;
  textArea.setAttribute('readonly', '');
  textArea.style.position = 'fixed';
  textArea.style.opacity = '0';
  document.body.append(textArea);
  textArea.select();
  const copied = document.execCommand('copy');
  textArea.remove();
  return copied;
}

function CopyIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <rect x="9" y="8" width="9" height="10" rx="1.5" />
      <path d="M15 8V6.5A1.5 1.5 0 0 0 13.5 5h-8A1.5 1.5 0 0 0 4 6.5v8A1.5 1.5 0 0 0 5.5 16H9" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="m5.5 12.5 4 4 9-9" />
    </svg>
  );
}
