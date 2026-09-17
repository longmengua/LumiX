import { useEffect, useRef, useState } from 'react';

import { EmptyState } from '../../../components/base/State';
import { ConfirmDialog } from '../../../components/base/ConfirmDialog';
import { InlineErrorState } from '../../components/AdminErrorPage';
import { AdminPageHero } from '../../components/AdminPageHero';
import { normalizeAdminError } from '../../api/adminError';
import { useI18n } from '../../../i18n';
import { getInputDateRangePreset, type DateRangePreset } from '../../../utils/dateRange';
import { formatDateTimeParts } from '../../../utils/format';
import {
  findAdminUsers,
  getAdminUser,
  type AdminUser,
  type AdminUserDetail,
  type AdminUserSearch,
  type AdminUserSearchCursor,
  type AdminUserAssetSnapshot,
  type AdminUserLedgerHistoryItem,
  type AdminUserAccountInventoryItem,
  getAdminUserAssets,
  getAdminUserAssetHistory,
  getAdminUserAccounts,
  setAdminUserRestriction,
} from '../../api/adminUsersApi';
import { UserManagementDrawer } from './UserManagementDrawer';
import { activeRestrictionCount, isFundTransferRestricted, isSystemUser, mapUserRestrictions } from './userRestrictions';

type UserDateFilter = 'created' | 'last-login';
type UserDateFilterValues = {
  createdFromDate: string;
  createdToDate: string;
  lastLoginFromDate: string;
  lastLoginToDate: string;
};
type VisualStatus = 'active' | 'inactive' | 'frozen';
type RestrictionAction = { user: AdminUser; kind: 'login' | 'withdrawal'; frozen: boolean };

/**
 * 使用者管理頁只經由受保護後端命令處理限制；前端不保存或自行推導任何權限狀態。
 */
export function AdminUsersPage() {
  const { t } = useI18n();
  const [displayNamePrefix, setDisplayNamePrefix] = useState('');
  const [createdFromDate, setCreatedFromDate] = useState('');
  const [createdToDate, setCreatedToDate] = useState('');
  const [lastLoginFromDate, setLastLoginFromDate] = useState('');
  const [lastLoginToDate, setLastLoginToDate] = useState('');
  const [openDateFilter, setOpenDateFilter] = useState<UserDateFilter | null>(null);
  const [items, setItems] = useState<AdminUser[]>([]);
  const [nextCursor, setNextCursor] = useState<AdminUserSearchCursor | null>(null);
  const [pageStarts, setPageStarts] = useState<Array<AdminUserSearchCursor | null>>([null]);
  const [pageNumber, setPageNumber] = useState(1);
  const [total, setTotal] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [appliedSearch, setAppliedSearch] = useState<AdminUserSearch>({});
  const [managedUserId, setManagedUserId] = useState<string | null>(null);
  const [detailsByUserId, setDetailsByUserId] = useState<Record<string, AdminUserDetail>>({});
  const [assetsByUserId, setAssetsByUserId] = useState<Record<string, AdminUserAssetSnapshot>>({});
  const [historyByUserId, setHistoryByUserId] = useState<Record<string, AdminUserLedgerHistoryItem[]>>({});
  const [accountsByUserId, setAccountsByUserId] = useState<Record<string, AdminUserAccountInventoryItem[]>>({});
  const [detailLoadingUserIds, setDetailLoadingUserIds] = useState<Set<string>>(() => new Set());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [restrictionAction, setRestrictionAction] = useState<RestrictionAction | null>(null);
  const [restrictionSubmitting, setRestrictionSubmitting] = useState(false);
  const requestSequence = useRef(0);

  function load(search: AdminUserSearch, cursor: AdminUserSearchCursor | null, targetPage: number, clearSnapshot: boolean) {
    const requestId = ++requestSequence.current;
    setLoading(true);
    setError(null);
    setPageNumber(targetPage);
    if (clearSnapshot) {
      // 新條件先清空舊快照與展開列，避免管理員誤把前一次篩選的高權限資料當成目前結果。
      setItems([]);
      setNextCursor(null);
      setTotal(0);
      setDetailsByUserId({});
      setAssetsByUserId({});
      setHistoryByUserId({});
      setAccountsByUserId({});
      setDetailLoadingUserIds(new Set());
    }

    void findAdminUsers({ ...search, cursor: cursor ?? undefined })
      .then((page) => {
        if (requestId !== requestSequence.current) return;
        setItems(page.items);
        setNextCursor(page.nextCursor);
        setTotal(page.total);
        setPageSize(page.pageSize);
      })
      .catch((requestError: unknown) => {
        if (requestId !== requestSequence.current) return;
        const error = normalizeAdminError(requestError);
        setError(error.kind === 'network' ? t('admin.errors.network.description') : t('admin.usersQueryError'));
      })
      .finally(() => {
        if (requestId === requestSequence.current) setLoading(false);
      });
  }

  useEffect(() => {
    load({}, null, 1, true);
  }, []);

  function buildSearch(overrides: Partial<UserDateFilterValues> = {}): AdminUserSearch {
    const filters: UserDateFilterValues = {
      createdFromDate: overrides.createdFromDate ?? createdFromDate,
      createdToDate: overrides.createdToDate ?? createdToDate,
      lastLoginFromDate: overrides.lastLoginFromDate ?? lastLoginFromDate,
      lastLoginToDate: overrides.lastLoginToDate ?? lastLoginToDate,
    };
    const search: AdminUserSearch = { displayNamePrefix: displayNamePrefix.trim() || undefined };
    if (filters.createdFromDate) search.createdFrom = toUtcDayStart(filters.createdFromDate);
    if (filters.createdToDate) search.createdBefore = toUtcDayAfter(filters.createdToDate);
    if (filters.lastLoginFromDate) search.lastLoginFrom = toUtcDayStart(filters.lastLoginFromDate);
    if (filters.lastLoginToDate) search.lastLoginBefore = toUtcDayAfter(filters.lastLoginToDate);
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
    setPageStarts([null]);
    load(search, null, 1, true);
  }

  function resetSearch() {
    setDisplayNamePrefix('');
    setCreatedFromDate('');
    setCreatedToDate('');
    setLastLoginFromDate('');
    setLastLoginToDate('');
    setOpenDateFilter(null);
    setAppliedSearch({});
    setPageStarts([null]);
    load({}, null, 1, true);
  }

  function goPreviousPage() {
    if (pageNumber <= 1) return;
    const previousPage = pageNumber - 1;
    load(appliedSearch, pageStarts[previousPage - 1] ?? null, previousPage, false);
  }

  function goNextPage() {
    if (!nextCursor) return;
    const nextPage = pageNumber + 1;
    // 保存每一頁起始 cursor，上一頁才能維持 keyset 查詢效能而無須退回 offset。
    setPageStarts((current) => [...current.slice(0, pageNumber), nextCursor]);
    load(appliedSearch, nextCursor, nextPage, false);
  }

  function applyDateFilter(filter: UserDateFilter, startDate: string, endDate: string) {
    const overrides = filter === 'created'
      ? { createdFromDate: startDate, createdToDate: endDate }
      : { lastLoginFromDate: startDate, lastLoginToDate: endDate };
    if (filter === 'created') {
      setCreatedFromDate(startDate);
      setCreatedToDate(endDate);
    } else {
      setLastLoginFromDate(startDate);
      setLastLoginToDate(endDate);
    }
    const search = buildSearch(overrides);
    setAppliedSearch(search);
    setPageStarts([null]);
    setOpenDateFilter(null);
    load(search, null, 1, true);
  }

  function clearDateFilter(filter: UserDateFilter) {
    const overrides = filter === 'created'
      ? { createdFromDate: '', createdToDate: '' }
      : { lastLoginFromDate: '', lastLoginToDate: '' };
    if (filter === 'created') {
      setCreatedFromDate('');
      setCreatedToDate('');
    } else {
      setLastLoginFromDate('');
      setLastLoginToDate('');
    }
    const search = buildSearch(overrides);
    setAppliedSearch(search);
    setPageStarts([null]);
    setOpenDateFilter(null);
    load(search, null, 1, true);
  }

  function openManagement(userId: string) {
    setManagedUserId(userId);
    if (detailsByUserId[userId] || detailLoadingUserIds.has(userId)) return;

    setDetailLoadingUserIds((current) => new Set(current).add(userId));
    // 各列獨立載入與快取，才能讓支援人員同時比對多位使用者的詳情。
    // 容器、projection 與 immutable history 各自是不同 read model，並行取得可避免其中一項被誤當成另一項的 fallback。
    void Promise.all([getAdminUser(userId), getAdminUserAccounts(userId), getAdminUserAssets(userId), getAdminUserAssetHistory(userId)])
      .then(([detail, accounts, assets, history]) => { setDetailsByUserId((current) => ({ ...current, [userId]: detail })); setAccountsByUserId((current) => ({ ...current, [userId]: accounts })); setAssetsByUserId((current) => ({ ...current, [userId]: assets })); setHistoryByUserId((current) => ({ ...current, [userId]: history })); })
      .catch(() => setError(t('admin.usersQueryError')))
      .finally(() => {
        setDetailLoadingUserIds((current) => {
          const next = new Set(current);
          next.delete(userId);
          return next;
        });
      });
  }

  function confirmRestriction() {
    if (!restrictionAction || restrictionSubmitting) return;
    setRestrictionSubmitting(true);
    const { user, kind, frozen } = restrictionAction;
    void setAdminUserRestriction(user.userId, kind, frozen)
      // 回應是後端已提交狀態；只更新命中的使用者，避免管理操作重置列表篩選、頁碼或捲動位置。
      .then((result) => {
        const update = (current: AdminUser) => current.userId !== user.userId ? current : {
          ...current, status: result.loginFrozen ? 'SUSPENDED' : 'ACTIVE', withdrawalFrozenAt: result.withdrawalFrozenAt,
          hasActiveRestriction: result.loginFrozen || result.withdrawalFrozenAt !== null || isFundTransferRestricted(current),
        };
        setItems((current) => current.map(update));
        setDetailsByUserId((current) => current[user.userId] ? { ...current, [user.userId]: { ...current[user.userId], user: update(current[user.userId].user) } } : current);
        setRestrictionAction(null);
      })
      .catch(() => setError(t('admin.usersRestrictionUpdateError')))
      .finally(() => setRestrictionSubmitting(false));
  }

  return (
    <section className="admin-users-page">
      <AdminPageHero
        icon={<span className="admin-users-hero-icon"><UsersIcon /></span>}
        title={t('admin.usersTitle')}
        description={t('admin.usersHeroDescription')}
        illustration={<UsersHeroArtwork />}
        slogan={t('admin.usersHeroTrust')}
        supportingText={t('admin.usersHeroTrustDescription')}
      />

      <section className="admin-users-workspace" aria-label={t('admin.usersTitle')}>
        <form className="admin-users-toolbar" onSubmit={(event) => { event.preventDefault(); submitSearch(); }}>
          <label className="admin-users-toolbar__query">
            <span className="sr-only">{t('admin.usersNamePrefix')}</span>
            <SearchIcon />
            <input
              className="input"
              value={displayNamePrefix}
              maxLength={128}
              placeholder={t('admin.usersNamePrefixPlaceholder')}
              onChange={(event) => setDisplayNamePrefix(event.target.value)}
            />
          </label>
          <div className="admin-users-toolbar__filters" aria-label={t('admin.usersDateFilters')}>
            <DateRangeFilter
              filter="created"
              open={openDateFilter === 'created'}
              label={t('admin.usersRegistrationFilter')}
              startDate={createdFromDate}
              endDate={createdToDate}
              onToggle={() => setOpenDateFilter((current) => current === 'created' ? null : 'created')}
              onClose={() => setOpenDateFilter(null)}
              onApply={(startDate, endDate) => applyDateFilter('created', startDate, endDate)}
              onClear={() => clearDateFilter('created')}
            />
            <DateRangeFilter
              filter="last-login"
              open={openDateFilter === 'last-login'}
              label={t('admin.usersLastLoginFilter')}
              startDate={lastLoginFromDate}
              endDate={lastLoginToDate}
              onToggle={() => setOpenDateFilter((current) => current === 'last-login' ? null : 'last-login')}
              onClose={() => setOpenDateFilter(null)}
              onApply={(startDate, endDate) => applyDateFilter('last-login', startDate, endDate)}
              onClear={() => clearDateFilter('last-login')}
            />
          </div>
          <div className="admin-users-toolbar__actions">
            <button className="primary-button admin-users-toolbar__search" disabled={loading}>
              <SearchIcon />{loading ? t('admin.usersSearching') : t('admin.usersSearch')}
            </button>
            <button className="ghost-button admin-users-toolbar__reset" type="button" disabled={loading} onClick={resetSearch}>
              {t('admin.usersReset')}
            </button>
          </div>
        </form>

        {error ? <InlineErrorState title={t('admin.errors.data.title')} description={error} retryLabel={t('common.retry')} onRetry={() => load(appliedSearch, pageStarts[pageNumber - 1] ?? null, pageNumber, false)} /> : null}

        {loading && items.length === 0 ? <UserListSkeleton /> : null}
        {!loading && items.length === 0 && !error ? (
          <EmptyState title={t('admin.usersEmpty')} description={t('admin.usersEmptyDescription')} action={<button className="secondary-button" type="button" onClick={resetSearch}>{t('admin.usersReset')}</button>} />
        ) : null}
        {items.length > 0 ? (
          <UserList
            items={items}
            onManage={openManagement}
          />
        ) : null}

        {!loading && items.length > 0 ? (
          <footer className="admin-users-pagination">
            <span>{t('admin.usersShownTotal', undefined, { shown: items.length, total })}</span>
            <div className="admin-users-pagination__controls">
              <span>{t('admin.usersPageSummary', undefined, { page: pageNumber, totalPages: Math.max(1, Math.ceil(total / pageSize)) })}</span>
              <button className="secondary-button" type="button" aria-label={t('admin.usersPreviousPage')} disabled={loading || pageNumber <= 1} onClick={goPreviousPage}><ChevronLeftIcon /></button>
              <button className="secondary-button" type="button" aria-label={t('admin.usersNextPage')} disabled={loading || !nextCursor} onClick={goNextPage}><ChevronRightIcon /></button>
            </div>
          </footer>
        ) : null}
      </section>
      <UserManagementDrawer
        user={items.find((user) => user.userId === managedUserId) ?? null}
        detail={managedUserId ? detailsByUserId[managedUserId] : undefined}
        accounts={managedUserId ? accountsByUserId[managedUserId] : undefined}
        assets={managedUserId ? assetsByUserId[managedUserId] : undefined}
        history={managedUserId ? historyByUserId[managedUserId] : undefined}
        loading={managedUserId ? detailLoadingUserIds.has(managedUserId) : false}
        onClose={() => setManagedUserId(null)}
        onRestrictionAction={setRestrictionAction}
      />
      <ConfirmDialog
        open={restrictionAction !== null}
        title={restrictionAction ? t(restrictionAction.frozen ? 'admin.usersRestrictionFreezeTitle' : 'admin.usersRestrictionUnfreezeTitle') : ''}
        description={restrictionAction ? t('admin.usersRestrictionConfirmDescription', undefined, { name: restrictionAction.user.displayName }) : ''}
        confirmLabel={restrictionAction ? t(restrictionAction.frozen ? 'admin.usersRestrictionFreeze' : 'admin.usersRestrictionUnfreeze') : ''}
        cancelLabel={t('common.cancel')}
        onCancel={() => { if (!restrictionSubmitting) setRestrictionAction(null); }}
        onConfirm={confirmRestriction}
      >
        {restrictionAction ? <p className="modal-card__note">{t(restrictionAction.kind === 'login' ? 'admin.usersLoginRestrictionNotice' : 'admin.usersWithdrawalRestrictionNotice')}</p> : null}
      </ConfirmDialog>
    </section>
  );
}

/** 使用者頁保留既有 orb 視覺，僅放入共用 Hero 插槽以統一各工作台的幾何與降階規則。 */
function UsersHeroArtwork() {
  return <div className="admin-users-hero-artwork"><div className="admin-users-hero__orbs"><span /><span /><span /></div></div>;
}

function UserList({ items, onManage }: { items: AdminUser[]; onManage: (userId: string) => void }) {
  const { t } = useI18n();
  return (
    <div className="admin-users-list" role="table" aria-label={t('admin.usersTitle')}>
      <div className="admin-users-list__head" role="row">
        <span role="columnheader">{t('admin.column.user')}</span>
        <span role="columnheader">{t('admin.usersTableStatus')}</span>
        <span role="columnheader">{t('admin.usersTableRestrictions')}</span>
        <span role="columnheader">{t('admin.column.registeredAt')}</span>
        <span role="columnheader">{t('admin.column.lastLogin')}</span>
        <span role="columnheader">{t('admin.column.actions')}</span>
      </div>
      {items.map((user) => {
        const visualStatus = getVisualStatus(user);
        const systemUser = isSystemUser(user);
        const restrictionCount = activeRestrictionCount(user);
        return (
            <div className="admin-users-list__row" role="row" key={user.userId}>
              <div className="admin-users-list__identity" role="cell">
                <UserAvatar displayName={user.displayName} status={visualStatus} />
                <div>
                  <strong>{user.displayName} {systemUser ? <em className="admin-users-system-badge">SYSTEM</em> : null}<span>({user.email})</span></strong>
                  <p><span>{user.userId}</span></p>
                </div>
              </div>
              <div className="admin-users-list__status" role="cell" data-label={t('admin.usersTableStatus')}><StatusDot status={visualStatus} />{systemUser ? t('admin.usersSystemAccount') : getStatusLabel(visualStatus, t)}</div>
              <div className="admin-users-list__restriction-summary" role="cell" data-label={t('admin.usersTableRestrictions')}>{systemUser ? t('admin.usersSystemRestriction') : getRestrictionSummary(user, restrictionCount, t)}</div>
              <DateTimeCell label={t('admin.column.registeredAt')} value={user.createdAt} />
              <DateTimeCell label={t('admin.column.lastLogin')} value={systemUser ? null : user.lastLoginAt} />
              <div className="admin-users-list__action" role="cell" data-label={t('admin.column.actions')}>
                <button className="secondary-button admin-users-list__manage" type="button" onClick={() => onManage(user.userId)}>{t('admin.usersManage')}<ChevronRightIcon /></button>
              </div>
            </div>
        );
      })}
    </div>
  );
}


/**
 * 兩個日期條件共用同一個 anchored popover；草稿只存在此元件內，關閉或點外部時絕不改動已套用的 API 條件。
 */
function DateRangeFilter({
  filter, open, label, startDate, endDate, onToggle, onClose, onApply, onClear,
}: {
  filter: UserDateFilter;
  open: boolean;
  label: string;
  startDate: string;
  endDate: string;
  onToggle: () => void;
  onClose: () => void;
  onApply: (startDate: string, endDate: string) => void;
  onClear: () => void;
}) {
  const { t } = useI18n();
  const containerRef = useRef<HTMLDivElement>(null);
  const [draftStartDate, setDraftStartDate] = useState(startDate);
  const [draftEndDate, setDraftEndDate] = useState(endDate);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const [selectedPreset, setSelectedPreset] = useState<DateRangePreset | null>(null);
  const hasAppliedRange = Boolean(startDate || endDate);
  const summary = hasAppliedRange ? formatCompactDateRange(startDate, endDate) : null;

  useEffect(() => {
    if (!open) return;
    // 每次重新打開都從已套用值建立草稿，取消不會遺留上一次未套用的輸入。
    setDraftStartDate(startDate);
    setDraftEndDate(endDate);
    setValidationMessage(null);
    setSelectedPreset(null);
  }, [open, startDate, endDate]);

  useEffect(() => {
    if (!open) return;
    function closeOnOutsidePointer(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) onClose();
    }
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose();
    }
    document.addEventListener('pointerdown', closeOnOutsidePointer);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsidePointer);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [open, onClose]);

  function apply() {
    if (draftStartDate && draftEndDate && draftStartDate > draftEndDate) {
      setValidationMessage(t('admin.usersDateRangeEndBeforeStart'));
      return;
    }
    onApply(draftStartDate, draftEndDate);
  }

  function selectPreset(preset: DateRangePreset) {
    const range = getInputDateRangePreset(preset);
    setDraftStartDate(range.startDate);
    setDraftEndDate(range.endDate);
    setValidationMessage(null);
    setSelectedPreset(preset);
  }

  const presets: Array<{ value: DateRangePreset; label: string }> = [
    { value: 'today', label: t('admin.usersDatePresetToday') },
    { value: 'last-7-days', label: t('admin.usersDatePresetLast7Days') },
    { value: 'last-30-days', label: t('admin.usersDatePresetLast30Days') },
    { value: 'this-month', label: t('admin.usersDatePresetThisMonth') },
    { value: 'last-month', label: t('admin.usersDatePresetLastMonth') },
  ];

  return (
    <div ref={containerRef} className={`admin-users-filter-control${open ? ' admin-users-filter-control--open' : ''}${hasAppliedRange ? ' admin-users-filter-control--applied' : ''}${filter === 'last-login' ? ' admin-users-filter-control--align-end' : ''}`}>
      <button className="admin-users-filter" type="button" aria-haspopup="dialog" aria-expanded={open} onClick={onToggle}>
        <CalendarIcon />
        <span className="admin-users-filter__label">{label}</span>
        {summary ? <strong>{summary}</strong> : null}
        {hasAppliedRange ? <span className="admin-users-filter__indicator" aria-hidden="true" /> : null}
        <ChevronDownIcon />
      </button>
      {hasAppliedRange ? (
        <button className="admin-users-filter__clear" type="button" aria-label={t('admin.usersClearDateFilter', undefined, { label })} title={t('admin.usersClearDateFilter', undefined, { label })} onClick={(event) => { event.stopPropagation(); onClear(); }}>
          <CloseIcon />
        </button>
      ) : null}
      {open ? (
        <section className="admin-users-date-popover" role="dialog" aria-label={label}>
          <header className="admin-users-date-popover__header">
            <strong>{label}</strong>
            <button type="button" aria-label={t('admin.usersCloseDateFilter')} onClick={onClose}><CloseIcon /></button>
          </header>
          <div className="admin-users-date-popover__presets" aria-label={t('admin.usersDateQuickRanges')}>
            {presets.map((preset) => <button key={preset.value} className={`admin-users-date-popover__preset${selectedPreset === preset.value ? ' admin-users-date-popover__preset--active' : ''}`} type="button" onClick={() => selectPreset(preset.value)}>{preset.label}</button>)}
          </div>
          <div className="admin-users-date-popover__custom">
            <span>{t('admin.usersCustomDate')}</span>
            <label className="field">
              <span className="field__label">{t('admin.usersRangeFrom')}</span>
              <input className="input" type="date" value={draftStartDate} max={draftEndDate || undefined} onChange={(event) => { setDraftStartDate(event.target.value); setValidationMessage(null); setSelectedPreset(null); }} />
            </label>
            <label className="field">
              <span className="field__label">{t('admin.usersRangeTo')}</span>
              <input className="input" type="date" value={draftEndDate} min={draftStartDate || undefined} onChange={(event) => { setDraftEndDate(event.target.value); setValidationMessage(null); setSelectedPreset(null); }} />
            </label>
          </div>
          {validationMessage ? <p className="admin-users-date-popover__error" role="alert">{validationMessage}</p> : null}
          <footer className="admin-users-date-popover__actions">
            <button className="ghost-button" type="button" onClick={onClear}>{t('admin.usersClearThisFilter')}</button>
            <button className="primary-button" type="button" onClick={apply}>{t('admin.usersApplyDateFilter')}</button>
          </footer>
        </section>
      ) : null}
    </div>
  );
}

function UserListSkeleton() {
  return <div className="admin-users-skeleton" aria-label="Loading" aria-busy="true">{Array.from({ length: 4 }, (_, index) => <span key={index} />)}</div>;
}

/** 時間拆為結構化兩行，避免完整 timestamp 撐寬資料欄而破壞 header 與 row 的對齊。 */
function DateTimeCell({ label, value }: { label: string; value: string | null }) {
  if (!value) return <time className="admin-users-list__date" role="cell" data-label={label}>—</time>;
  const formatted = formatDateTimeParts(value);
  return <time className="admin-users-list__date" role="cell" data-label={label} dateTime={value}><span>{formatted.date}</span><span>{formatted.time}</span></time>;
}

function UserAvatar({ displayName, status }: { displayName: string; status: VisualStatus }) {
  const avatarInitials = initials(displayName);
  // 中文名稱通常只會取到一個字，改用人物符號能避免出現難以辨識的單字縮寫。
  const usePersonIcon = avatarInitials.length === 1 && displayName.trim().length > 1;
  return <div className={`admin-users-avatar${usePersonIcon ? ' admin-users-avatar--icon' : ''}`} aria-hidden="true">{usePersonIcon ? <UserAvatarIcon /> : <span>{avatarInitials}</span>}<StatusDot status={status} /></div>;
}

function StatusDot({ status }: { status: VisualStatus }) { return <span className={`admin-users-status admin-users-status--${status}`} aria-hidden="true" />; }

function getVisualStatus(user: AdminUser): VisualStatus { return user.hasActiveRestriction ? 'frozen' : user.status === 'ACTIVE' ? 'active' : 'inactive'; }
function getStatusLabel(status: VisualStatus, t: ReturnType<typeof useI18n>['t']) { return status === 'active' ? t('admin.usersStatusActive') : status === 'frozen' ? t('admin.usersStatusFrozen') : t('admin.usersStatusInactive'); }
function getRestrictionSummary(user: AdminUser, count: number, t: ReturnType<typeof useI18n>['t']) {
  if (count === 0) return t('admin.usersNoRestrictions');
  if (count > 2) return t('admin.usersRestrictionCount', undefined, { count });
  const keys = mapUserRestrictions(user).filter((item) => item.active).map((item) => item.key);
  if (keys.length === 2) return t('admin.usersRestrictionLoginAndWithdrawal');
  return t(keys[0] === 'login' ? 'admin.usersRestrictionLoginSummary' : keys[0] === 'withdrawal' ? 'admin.usersRestrictionWithdrawalSummary' : 'admin.usersRestrictionTransferSummary');
}
function initials(name: string) { return name.trim().split(/\s+/).slice(0, 2).map((part) => part.slice(0, 1)).join('').toUpperCase() || '?'; }
function toUtcDayStart(date: string) { return `${date}T00:00:00.000Z`; }
function toUtcDayAfter(date: string) { const value = new Date(`${date}T00:00:00.000Z`); value.setUTCDate(value.getUTCDate() + 1); return value.toISOString(); }
function formatCompactDateRange(from: string, to: string) {
  const format = (value: string) => value ? `${value.slice(5, 7)}/${value.slice(8, 10)}` : '…';
  return `${format(from)}–${format(to)}`;
}

function UsersIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M16 20v-1.4a4.6 4.6 0 0 0-4.6-4.6H7.6A4.6 4.6 0 0 0 3 18.6V20M9.5 10.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM21 20v-1.4a4.6 4.6 0 0 0-3.2-4.4M16.5 3.7a3.5 3.5 0 0 1 0 6.6" /></svg>; }
function SearchIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.8" cy="10.8" r="5.8" /><path d="m15.1 15.1 4.3 4.3" /></svg>; }
function CalendarIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="5.5" width="16" height="14" rx="2" /><path d="M8 3.5v4M16 3.5v4M4 10h16" /></svg>; }
function ChevronDownIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7.5 9.5 4.5 4.5 4.5-4.5" /></svg>; }
function ChevronRightIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>; }
function ChevronLeftIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 5-7 7 7 7" /></svg>; }
function CloseIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7.5 7.5 9 9M16.5 7.5l-9 9" /></svg>; }
function UserAvatarIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.2" /><path d="M5.7 20a6.3 6.3 0 0 1 12.6 0" /></svg>; }
