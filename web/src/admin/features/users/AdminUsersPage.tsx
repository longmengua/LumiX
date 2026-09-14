import { Fragment, useEffect, useRef, useState } from 'react';

import { EmptyState } from '../../../components/base/State';
import { InlineErrorState } from '../../components/AdminErrorPage';
import { normalizeAdminError } from '../../api/adminError';
import { useI18n } from '../../../i18n';
import { getInputDateRangePreset, type DateRangePreset } from '../../../utils/dateRange';
import { formatDateTimeParts, formatTime } from '../../../utils/format';
import {
  findAdminUsers,
  getAdminUser,
  type AdminUser,
  type AdminUserDetail,
  type AdminUserSearch,
  type AdminUserSearchCursor,
} from '../../api/adminUsersApi';

type UserDateFilter = 'created' | 'last-login';
type UserDateFilterValues = {
  createdFromDate: string;
  createdToDate: string;
  lastLoginFromDate: string;
  lastLoginToDate: string;
};
type VisualStatus = 'active' | 'inactive' | 'frozen';

/**
 * 使用者管理頁只處理唯讀檢視、查詢與詳情展開；登入、權限與任何帳戶限制的變更仍由既有後端邊界負責。
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
  const [expandedUserIds, setExpandedUserIds] = useState<Set<string>>(() => new Set());
  const [detailsByUserId, setDetailsByUserId] = useState<Record<string, AdminUserDetail>>({});
  const [detailLoadingUserIds, setDetailLoadingUserIds] = useState<Set<string>>(() => new Set());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
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
      setExpandedUserIds(new Set());
      setDetailsByUserId({});
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
    // 各列獨立載入與快取，才能讓支援人員同時比對多位使用者的詳情。
    void getAdminUser(userId)
      .then((detail) => setDetailsByUserId((current) => ({ ...current, [userId]: detail })))
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
    <section className="admin-users-page">
      <header className="admin-users-hero">
        <div className="admin-users-hero__heading">
          <div className="admin-users-hero__icon" aria-hidden="true"><UsersIcon /></div>
          <div>
            <h1>{t('admin.usersTitle')}</h1>
            <p>{t('admin.usersHeroDescription')}</p>
          </div>
        </div>
        <div className="admin-users-hero__decoration" aria-hidden="true">
          <div className="admin-users-hero__orbs"><span /><span /><span /></div>
          <div>
            <strong>{t('admin.usersHeroTrust')}</strong>
            <p>{t('admin.usersHeroTrustDescription')}</p>
          </div>
        </div>
      </header>

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
              <ResetIcon />{t('admin.usersReset')}
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
            detailsByUserId={detailsByUserId}
            detailLoadingUserIds={detailLoadingUserIds}
            expandedUserIds={expandedUserIds}
            onToggleDetail={toggleDetail}
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
    </section>
  );
}

function UserList({
  items, detailsByUserId, detailLoadingUserIds, expandedUserIds, onToggleDetail,
}: {
  items: AdminUser[];
  detailsByUserId: Record<string, AdminUserDetail>;
  detailLoadingUserIds: Set<string>;
  expandedUserIds: Set<string>;
  onToggleDetail: (userId: string) => void;
}) {
  const { t } = useI18n();
  return (
    <div className="admin-users-list" role="table" aria-label={t('admin.usersTitle')}>
      <div className="admin-users-list__head" role="row">
        <span role="columnheader">{t('admin.column.user')}</span>
        <span role="columnheader">{t('admin.column.registeredAt')}</span>
        <span role="columnheader">{t('admin.column.lastLogin')}</span>
        <span role="columnheader">{t('admin.column.actions')}</span>
      </div>
      {items.map((user) => {
        const visualStatus = getVisualStatus(user);
        const expanded = expandedUserIds.has(user.userId);
        const detailId = `admin-user-detail-${user.userId}`;
        const detail = detailsByUserId[user.userId];
        const detailLoading = detailLoadingUserIds.has(user.userId);
        return (
          <Fragment key={user.userId}>
            <div className="admin-users-list__row" role="row">
              <div className="admin-users-list__identity" role="cell">
                <UserAvatar displayName={user.displayName} status={visualStatus} />
                <div>
                  <strong>{user.displayName} <span>({user.email})</span><CopyButton value={user.email} label={t('admin.usersCopyEmail')} /></strong>
                  <p><span>{user.userId}</span><CopyButton value={user.userId} label={t('admin.usersCopyUserId')} /></p>
                </div>
              </div>
              <DateTimeCell label={t('admin.column.registeredAt')} value={user.createdAt} />
              <DateTimeCell label={t('admin.column.lastLogin')} value={user.lastLoginAt} />
              <div className="admin-users-list__action" role="cell" data-label={t('admin.column.actions')}>
                <button className="secondary-button" type="button" aria-expanded={expanded} aria-controls={detailId} onClick={() => onToggleDetail(user.userId)}>
                  {expanded ? t('admin.usersHideDetails') : t('admin.usersShowDetails')}<ChevronRightIcon />
                </button>
              </div>
            </div>
            {expanded ? <UserDetail user={user} detail={detail} loading={detailLoading} id={detailId} /> : null}
          </Fragment>
        );
      })}
    </div>
  );
}

function UserDetail({ user, detail, loading, id }: { user: AdminUser; detail?: AdminUserDetail; loading: boolean; id: string }) {
  const { t } = useI18n();
  const visualStatus = getVisualStatus(user);
  const restrictions = getRestrictionMessages(user, t);
  return (
    <section className="admin-users-list__detail" id={id} aria-label={t('admin.usersDetailsFor', undefined, { name: user.displayName })}>
      {loading ? <p>{t('admin.usersDetailsLoading')}</p> : null}
      {!loading && detail ? (
        <dl>
          <div><dt>{t('admin.usersDetailStatus')}</dt><dd><StatusDot status={visualStatus} />{getStatusLabel(visualStatus, t)}</dd></div>
          <div className={`admin-users-list__restrictions${user.hasActiveRestriction ? '' : ' admin-users-list__restrictions--clear'}`}><dt>{t('admin.usersDetailRestrictions')}</dt><dd>{restrictions.map((message) => <span key={message}>{message}</span>)}</dd></div>
          <div><dt>{t('admin.usersDetailRegistered')}</dt><dd>{formatTime(detail.user.createdAt)}</dd></div>
          <div><dt>{t('admin.usersDetailLastLogin')}</dt><dd>{detail.user.lastLoginAt ? formatTime(detail.user.lastLoginAt) : '—'}</dd></div>
          <div className="admin-users-list__devices"><dt>{t('admin.usersDetailDevices')}</dt><dd>{detail.devices.map((device) => `${device.platform}／${device.label}`).join('、') || t('admin.usersDetailNoDevices')}</dd></div>
        </dl>
      ) : null}
      {!loading && !detail ? <p className="form-message form-message--error">{t('admin.usersQueryError')}</p> : null}
    </section>
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

function CopyButton({ value, label }: { value: string; label: string }) {
  const { t } = useI18n();
  const [copied, setCopied] = useState(false);
  const resetTimer = useRef<number | null>(null);
  useEffect(() => () => { if (resetTimer.current !== null) window.clearTimeout(resetTimer.current); }, []);

  async function copy() {
    if (!await copyText(value)) return;
    setCopied(true);
    if (resetTimer.current !== null) window.clearTimeout(resetTimer.current);
    resetTimer.current = window.setTimeout(() => setCopied(false), 1600);
  }

  return <button className="admin-users-copy" type="button" onClick={() => void copy()} aria-label={copied ? t('admin.usersCopied') : label} title={copied ? t('admin.usersCopied') : label}>{copied ? <CheckIcon /> : <CopyIcon />}<span className="sr-only" aria-live="polite">{copied ? t('admin.usersCopied') : ''}</span></button>;
}

/** Clipboard API 是主要路徑；fallback 只服務受限環境，避免支援人員因瀏覽器權限差異無法複製識別資料。 */
async function copyText(value: string) {
  try {
    if (navigator.clipboard) { await navigator.clipboard.writeText(value); return true; }
  } catch { /* 權限拒絕後繼續嘗試相容 fallback。 */ }
  const element = document.createElement('textarea');
  element.value = value;
  element.setAttribute('readonly', '');
  element.style.position = 'fixed';
  element.style.opacity = '0';
  document.body.append(element);
  element.select();
  const copied = document.execCommand('copy');
  element.remove();
  return copied;
}

function getVisualStatus(user: AdminUser): VisualStatus { return user.hasActiveRestriction ? 'frozen' : user.status === 'ACTIVE' ? 'active' : 'inactive'; }
function getStatusLabel(status: VisualStatus, t: ReturnType<typeof useI18n>['t']) { return status === 'active' ? t('admin.usersStatusActive') : status === 'frozen' ? t('admin.usersStatusFrozen') : t('admin.usersStatusInactive'); }
function getRestrictionMessages(user: AdminUser, t: ReturnType<typeof useI18n>['t']) {
  const messages: string[] = [];
  if (user.status === 'SUSPENDED') messages.push(t('admin.usersRestrictionSuspended'));
  if (isFundTransferRestricted(user)) messages.push(t('admin.usersRestrictionFundTransfer', undefined, { until: formatTime(user.fundTransferRestrictedUntil!) }));
  if (user.hasActiveRestriction && messages.length === 0) messages.push(t('admin.usersRestrictionFrozenAccount'));
  return messages.length > 0 ? messages : [t('admin.usersNoRestrictions')];
}
function isFundTransferRestricted(user: AdminUser) { return user.fundTransferRestrictedUntil !== null && Date.parse(user.fundTransferRestrictedUntil) > Date.now(); }
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
function ResetIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 8v4h4M5.5 15.5A7 7 0 1 0 5 8" /></svg>; }
function ChevronDownIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7.5 9.5 4.5 4.5 4.5-4.5" /></svg>; }
function ChevronRightIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>; }
function ChevronLeftIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 5-7 7 7 7" /></svg>; }
function CloseIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7.5 7.5 9 9M16.5 7.5l-9 9" /></svg>; }
function CopyIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="9" y="8" width="9" height="10" rx="1.5" /><path d="M15 8V6.5A1.5 1.5 0 0 0 13.5 5h-8A1.5 1.5 0 0 0 4 6.5v8A1.5 1.5 0 0 0 5.5 16H9" /></svg>; }
function CheckIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5.5 12.5 4 4 9-9" /></svg>; }
function UserAvatarIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.2" /><path d="M5.7 20a6.3 6.3 0 0 1 12.6 0" /></svg>; }
