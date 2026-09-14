import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { NavLink, Route, Routes, useNavigate } from 'react-router-dom';

import { Card } from '../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../components/base/State';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import {
  fetchAccountProfile,
  fetchLoginHistoryPage,
  fetchLoginSecurity,
  removeBoundLoginDevice,
  type AccountProfileRecord,
  type LoginHistoryRecord,
  type LoginSecurityRecord,
} from '../features/account/accountApi';
import { useAuthentication } from '../features/auth/AuthenticationProvider';
import { accountNavItems } from '../features/navigation/accountNav';
import { useI18n } from '../i18n';
import { formatTime } from '../utils/format';

const LOGIN_HISTORY_PAGE_SIZE = 10;
const LOGIN_HISTORY_ANCHOR_PARAM = 'anchor';

/**
 * 個人中心只組合已接入的本人 profile 與登入紀錄。
 *
 * KYC、資產、劃轉、API Key 與偏好設定尚無後端 runtime；不能為了保留舊畫面而載入 mock 資料，否則
 * 使用者會把假資料誤認為自己的帳戶狀態。
 */
export function AccountPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { signOut, updateDisplayName } = useAuthentication();
  const [profile, setProfile] = useState<AccountProfileRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadAccount() {
      setLoading(true);
      setError(null);
      try {
        const userProfile = await fetchAccountProfile();
        if (alive) setProfile(userProfile);
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load account data.');
        }
      } finally {
        if (alive) setLoading(false);
      }
    }

    void loadAccount();
    return () => {
      alive = false;
    };
  }, []);

  async function handleDisplayNameUpdate(displayName: string) {
    const updatedUser = await updateDisplayName(displayName);
    setProfile((current) => (current ? { ...current, displayName: updatedUser.displayName } : current));
  }

  async function handleSignOut() {
    try {
      await signOut();
    } finally {
      // 無論遠端撤銷結果為何，都離開受保護頁面，避免使用者誤以為目前 session 仍可使用。
      navigate('/login', { replace: true });
    }
  }

  const sidebarItems = useMemo(
    () => accountNavItems.map(({ to, labelKey, end }) => ({ to, label: t(labelKey), end })),
    [t],
  );

  return (
    <div className="two-column">
      <Sidebar
        title={t('account.sidebarTitle')}
        items={sidebarItems}
        footer={(
          <button className="sidebar__action" type="button" onClick={() => void handleSignOut()}>
            {t('header.signOut')}
          </button>
        )}
      />
      <div className="stack">
        <PageHeader title={t('account.pageTitle')} description={t('account.pageDescription')} />
        {loading ? <LoadingState title={t('account.loadingTitle')} description={t('account.loadingDescription')} /> : null}
        {error ? (
          <ErrorState
            title={t('account.errorTitle')}
            description={error}
            action={<NavLink to="/account">{t('common.retry')}</NavLink>}
          />
        ) : null}
        {!loading && !error && profile ? (
          <Routes>
            <Route index element={<AccountOverviewPage profile={profile} onDisplayNameUpdate={handleDisplayNameUpdate} />} />
            <Route path="security" element={<AccountSecurityPage />} />
            <Route path="login-history" element={<AccountLoginHistoryPage />} />
            <Route path="*" element={<AccountPendingIntegrationPage />} />
          </Routes>
        ) : null}
      </div>
    </div>
  );
}

function AccountOverviewPage({
  profile,
  onDisplayNameUpdate,
}: {
  profile: AccountProfileRecord;
  onDisplayNameUpdate: (displayName: string) => Promise<void>;
}) {
  const { t } = useI18n();
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [boundDevices, setBoundDevices] = useState<LoginSecurityRecord['devices'] | null>(null);
  const [fundTransferRestrictedUntil, setFundTransferRestrictedUntil] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    setDisplayName(profile.displayName);
  }, [profile.displayName]);

  useEffect(() => {
    let alive = true;

    // 總覽僅摘要本人既有的去敏裝置資料；讀取失敗時保留可前往安全頁的入口，不能猜測為「沒有裝置」。
    void fetchLoginSecurity()
      .then((security) => {
        if (alive) {
          setBoundDevices(security.devices);
          setFundTransferRestrictedUntil(security.fundTransferRestrictedUntil);
        }
      })
      .catch(() => {
        if (alive) setBoundDevices([]);
      });

    return () => {
      alive = false;
    };
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await onDisplayNameUpdate(displayName);
      setSuccess(t('account.profileSaveSuccess'));
    } catch {
      setError(t('account.profileSaveError'));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="stack">
      <Card title={t('account.profileTitle')}>
        <form className="stack" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field__label">{t('account.profileDisplayName')}</span>
            <input
              className="input"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              autoComplete="name"
              minLength={2}
              maxLength={128}
              required
            />
          </label>
          <StatList
            items={[
              [t('account.profileUserId'), profile.userId],
              [t('account.profileEmail'), profile.email],
              [t('account.profileCreatedAt'), formatTime(profile.createdAt)],
            ]}
          />
          {error ? <p className="form-message form-message--error">{error}</p> : null}
          {success ? <p className="form-message form-message--success">{success}</p> : null}
          <button className="primary-button account-profile__save" type="submit" disabled={saving}>
            {saving ? t('account.profileSaving') : t('account.profileSave')}
          </button>
        </form>
      </Card>
      <Card title={t('account.overviewBoundDevicesTitle')}>
        <p className="account-device-policy-note">{t('account.boundDevicesPolicy')}</p>
        <FundTransferRestrictionNotice restrictedUntil={fundTransferRestrictedUntil} />
        {boundDevices === null ? <p className="account-overview-devices__loading">{t('account.overviewBoundDevicesLoading')}</p> : null}
        {boundDevices?.length === 0 ? <p className="account-overview-devices__loading">{t('account.overviewBoundDevicesUnavailable')}</p> : null}
        {boundDevices && boundDevices.length > 0 ? (
          <div className="account-overview-devices">
            {boundDevices.slice(0, 3).map((device) => (
              <div className="account-overview-devices__item" key={device.deviceId}>
                <div className="account-overview-devices__identity">
                  <p className="account-overview-devices__platform">{t(`account.devicePlatform.${device.platform}`)}</p>
                  <p className="account-row__title">{device.deviceLabel}</p>
                  <p className="account-row__meta">{t('account.boundDevicesMeta', undefined, { ipAddress: device.lastIpAddress })}</p>
                </div>
                <time className="account-overview-devices__time" dateTime={device.lastSeenAt}>
                  {t('account.boundDevicesLastSeenAt', undefined, { time: formatTime(device.lastSeenAt) })}
                </time>
              </div>
            ))}
            <div className="account-overview-devices__footer">
              <span>{t('account.overviewBoundDevicesCount', undefined, { count: boundDevices.length })}</span>
              <NavLink to="/account/security">{t('account.overviewBoundDevicesManage')}</NavLink>
            </div>
          </div>
        ) : null}
      </Card>
      <Card title={t('account.securityTitle')}>
        <AccountSecurityActions />
      </Card>
      <AccountPendingIntegrationPage />
    </div>
  );
}

/** 已接入的安全操作只連向已驗證 route，不把尚未提供的 MFA 或裝置資料偽裝成可用功能。 */
function AccountSecurityPage() {
  const { t } = useI18n();
  const [security, setSecurity] = useState<LoginSecurityRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [removingDeviceId, setRemovingDeviceId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    void fetchLoginSecurity()
      .then((value) => {
        if (alive) setSecurity(value);
      })
      .catch(() => {
        if (alive) setError(t('account.loginSecurityLoadError'));
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => { alive = false; };
  }, [t]);

  async function handleDeviceRemoval(deviceId: string) {
    setRemovingDeviceId(deviceId);
    setError(null);
    try {
      await removeBoundLoginDevice(deviceId);
      setSecurity((current) => current
        ? { ...current, devices: current.devices.filter((device) => device.deviceId !== deviceId) }
        : current);
    } catch {
      setError(t('account.boundDevicesRemoveError'));
    } finally {
      setRemovingDeviceId(null);
    }
  }

  if (loading) return <LoadingState title={t('account.loginSecurityLoadingTitle')} description={t('account.loginSecurityLoadingDescription')} />;
  if (!security) return <ErrorState title={t('account.errorTitle')} description={error ?? t('account.loginSecurityLoadError')} />;

  return (
    <div className="stack">
      <Card title={t('account.securityCenterTitle')}>
        <AccountSecurityActions />
      </Card>
      <Card title={t('account.boundDevicesTitle')}>
        <p className="account-device-policy-note">{t('account.boundDevicesPolicy')}</p>
        <FundTransferRestrictionNotice restrictedUntil={security.fundTransferRestrictedUntil} />
        {security.devices.length === 0 ? <EmptyState title={t('account.boundDevicesEmptyTitle')} description={t('account.boundDevicesEmptyDescription')} /> : null}
        <div className="timeline-list">
          {security.devices.map((device) => (
            <div className="timeline-item" key={device.deviceId}>
              <div>
                <p className="account-overview-devices__platform">{t(`account.devicePlatform.${device.platform}`)}</p>
                <p className="account-row__title">{device.deviceLabel}</p>
                <p className="account-row__meta">{t('account.boundDevicesMeta', undefined, { ipAddress: device.lastIpAddress })}</p>
                <p className="account-row__meta">{t('account.boundDevicesBoundAt', undefined, { time: formatTime(device.createdAt) })}</p>
              </div>
              <div className="account-device__actions">
                <span className="timeline-item__time">{t('account.boundDevicesLastSeenAt', undefined, { time: formatTime(device.lastSeenAt) })}</span>
                <button
                  className="secondary-button account-device__remove"
                  disabled={removingDeviceId !== null}
                  type="button"
                  onClick={() => void handleDeviceRemoval(device.deviceId)}
                >
                  {removingDeviceId === device.deviceId ? t('account.boundDevicesRemoving') : t('account.boundDevicesRemove')}
                </button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

/**
 * 資金外流限制的倒數只改善使用者理解；真正的提款與帳戶間轉帳未來必須各自以 server 時間重新拒絕。
 */
function FundTransferRestrictionNotice({ restrictedUntil }: { restrictedUntil: string | null | undefined }) {
  const { t } = useI18n();
  const [now, setNow] = useState(() => Date.now());
  const until = restrictedUntil === null || restrictedUntil === undefined ? Number.NaN : Date.parse(restrictedUntil);
  const remainingSeconds = Number.isNaN(until) ? 0 : Math.max(0, Math.ceil((until - now) / 1_000));

  useEffect(() => {
    if (remainingSeconds === 0) return undefined;
    const timerId = window.setInterval(() => setNow(Date.now()), 1_000);
    return () => window.clearInterval(timerId);
  }, [remainingSeconds]);

  if (remainingSeconds === 0) return null;
  const hours = Math.floor(remainingSeconds / 3_600);
  const minutes = Math.floor((remainingSeconds % 3_600) / 60);
  const seconds = remainingSeconds % 60;
  const countdown = [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':');

  return (
    <p className="account-fund-transfer-restriction" role="status">
      {t('account.fundTransferRestriction', undefined, { countdown })}
    </p>
  );
}

function AccountSecurityActions() {
  const { t } = useI18n();
  return (
    <div className="stack">
      <div className="account-row">
        <div>
          <p className="account-row__title">{t('account.profilePasswordTitle')}</p>
          <p className="account-row__meta">{t('account.profilePasswordDescription')}</p>
        </div>
        <NavLink className="secondary-button" to="/change-password">
          {t('account.profilePasswordAction')}
        </NavLink>
      </div>
      <div className="account-row">
        <div>
          <p className="account-row__title">{t('account.loginHistoryTitle')}</p>
        </div>
        <NavLink className="secondary-button" to="/account/login-history">
          {t('account.profileLoginHistoryAction')}
        </NavLink>
      </div>
    </div>
  );
}

/**
 * 雙向無限捲動的登入紀錄。
 *
 * URL anchor 永遠對應目前視窗最上方可見的紀錄；刷新後先以該時間作為 server cursor，因此不會被最新
 * 登入事件拉回第一頁。往上 prepend 時補償文件高度差，保留使用者正在閱讀的紀錄位置。
 */
function AccountLoginHistoryPage() {
  const { t } = useI18n();
  const [records, setRecords] = useState<LoginHistoryRecord[]>([]);
  const [hasOlder, setHasOlder] = useState(false);
  const [hasNewer, setHasNewer] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [loadingNewer, setLoadingNewer] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const recordsRef = useRef<LoginHistoryRecord[]>([]);
  const topSentinelRef = useRef<HTMLDivElement>(null);
  const bottomSentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    recordsRef.current = records;
  }, [records]);

  useEffect(() => {
    let alive = true;
    const anchor = readLoginHistoryAnchor();

    async function loadInitialPage() {
      setInitialLoading(true);
      setError(null);
      try {
        const page = await fetchLoginHistoryPage({ limit: LOGIN_HISTORY_PAGE_SIZE, anchor: anchor ?? undefined });
        if (alive) {
          setRecords(page.records);
          setHasOlder(page.hasOlder);
          setHasNewer(page.hasNewer);
        }
      } catch (loadError) {
        if (alive) setError(loadError instanceof Error ? loadError.message : 'LOGIN_HISTORY_REQUEST_FAILED');
      } finally {
        if (alive) setInitialLoading(false);
      }
    }

    void loadInitialPage();
    return () => {
      alive = false;
    };
  }, []);

  async function loadOlder() {
    const oldest = recordsRef.current[recordsRef.current.length - 1];
    if (!oldest || !hasOlder || loadingOlder) return;

    setLoadingOlder(true);
    try {
      const page = await fetchLoginHistoryPage({ limit: LOGIN_HISTORY_PAGE_SIZE, before: oldest.occurredAt });
      setRecords((current) => mergeLoginHistoryRecords(current, page.records));
      setHasOlder(page.hasOlder);
      setHasNewer((current) => current || page.hasNewer);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'LOGIN_HISTORY_REQUEST_FAILED');
    } finally {
      setLoadingOlder(false);
    }
  }

  async function loadNewer() {
    const newest = recordsRef.current[0];
    if (!newest || !hasNewer || loadingNewer) return;

    const previousHeight = document.documentElement.scrollHeight;
    const previousScrollTop = window.scrollY;
    setLoadingNewer(true);
    try {
      const page = await fetchLoginHistoryPage({ limit: LOGIN_HISTORY_PAGE_SIZE, after: newest.occurredAt });
      setRecords((current) => mergeLoginHistoryRecords(page.records, current));
      setHasNewer(page.hasNewer);
      setHasOlder((current) => current || page.hasOlder);
      requestAnimationFrame(() => {
        // prepend 後用高度差回補，避免使用者滾到上方時目前閱讀的列突然向下跳動。
        window.scrollTo({ top: previousScrollTop + document.documentElement.scrollHeight - previousHeight });
      });
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'LOGIN_HISTORY_REQUEST_FAILED');
    } finally {
      setLoadingNewer(false);
    }
  }

  useEffect(() => {
    if (!hasNewer || !topSentinelRef.current) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) void loadNewer();
    }, { rootMargin: '160px 0px 0px' });
    observer.observe(topSentinelRef.current);
    return () => observer.disconnect();
  }, [hasNewer, loadingNewer, records]);

  useEffect(() => {
    if (!hasOlder || !bottomSentinelRef.current) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) void loadOlder();
    }, { rootMargin: '0px 0px 240px' });
    observer.observe(bottomSentinelRef.current);
    return () => observer.disconnect();
  }, [hasOlder, loadingOlder, records]);

  useEffect(() => {
    let scheduled = false;
    const updateAnchor = () => {
      if (scheduled) return;
      scheduled = true;
      requestAnimationFrame(() => {
        scheduled = false;
        const visibleRecord = Array.from(document.querySelectorAll<HTMLElement>('[data-login-history-at]'))
          .find((element) => element.getBoundingClientRect().bottom > 0);
        if (visibleRecord?.dataset.loginHistoryAt) writeLoginHistoryAnchor(visibleRecord.dataset.loginHistoryAt);
      });
    };
    window.addEventListener('scroll', updateAnchor, { passive: true });
    return () => window.removeEventListener('scroll', updateAnchor);
  }, [records]);

  if (initialLoading) {
    return <LoadingState title={t('account.loginHistoryLoadingTitle')} description={t('account.loginHistoryLoadingDescription')} />;
  }

  if (error && records.length === 0) {
    return <ErrorState title={t('account.errorTitle')} description={error} />;
  }

  return (
    <Card title={t('account.loginHistoryTitle')}>
      <div className="timeline-list">
        <div ref={topSentinelRef} aria-hidden="true" />
        {loadingNewer ? <p className="auth-form__hint">{t('account.loginHistoryLoadingNewer')}</p> : null}
        {records.map((record) => (
          <div className="timeline-item" data-login-history-at={record.occurredAt} key={record.occurredAt}>
            <div>
              <p className="account-row__title">{t('account.loginHistoryEntryTitle')}</p>
              <p className="account-row__meta">
                {record.deviceLabel && record.ipAddress
                  ? t('account.loginHistoryEntryDescriptionWithDevice', undefined, {
                    device: record.deviceLabel,
                    ipAddress: record.ipAddress,
                  })
                  : t('account.loginHistoryEntryDescription')}
              </p>
            </div>
            <span className="timeline-item__time">{formatTime(record.occurredAt)}</span>
          </div>
        ))}
        {error ? <p className="form-message form-message--error">{error}</p> : null}
        {loadingOlder ? <p className="auth-form__hint">{t('account.loginHistoryLoadingOlder')}</p> : null}
        <div ref={bottomSentinelRef} aria-hidden="true" />
        {!hasOlder && records.length > 0 ? <p className="auth-form__hint">{t('account.loginHistoryNoOlder')}</p> : null}
      </div>
      {records.length === 0 ? <EmptyState title={t('account.loginHistoryTitle')} description={t('account.noLoginRecords')} /> : null}
    </Card>
  );
}

function mergeLoginHistoryRecords(
  leading: LoginHistoryRecord[],
  trailing: LoginHistoryRecord[],
): LoginHistoryRecord[] {
  // createdAt 是 API 唯一公開的安全事件識別，不加入 session ID 來消除重疊頁面的重複列。
  const unique = new Map<string, LoginHistoryRecord>();
  [...leading, ...trailing].forEach((record) => unique.set(record.occurredAt, record));
  return [...unique.values()].sort((left, right) => right.occurredAt.localeCompare(left.occurredAt));
}

function readLoginHistoryAnchor(): string | null {
  const value = new URLSearchParams(window.location.search).get(LOGIN_HISTORY_ANCHOR_PARAM);
  return value && !Number.isNaN(Date.parse(value)) ? value : null;
}

function writeLoginHistoryAnchor(anchor: string) {
  const url = new URL(window.location.href);
  if (url.searchParams.get(LOGIN_HISTORY_ANCHOR_PARAM) === anchor) return;
  url.searchParams.set(LOGIN_HISTORY_ANCHOR_PARAM, anchor);
  window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
}

function AccountPendingIntegrationPage() {
  const { t } = useI18n();
  return (
    <EmptyState
      title={t('account.profileAdditionalDataTitle')}
      description={t('account.profileAdditionalDataDescription')}
    />
  );
}

function StatList({ items }: { items: Array<[string, ReactNode]> }) {
  return (
    <dl className="stat-list">
      {items.map(([label, value]) => (
        <div className="stat-list__row" key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}
