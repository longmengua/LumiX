import { useEffect, useState } from 'react';

import { Badge } from '../../../components/base/Badge';
import { CopyButton } from '../../../components/base/CopyButton';
import { useI18n } from '../../../i18n';
import { formatDecimalString, formatTime } from '../../../utils/format';
import type { AdminUser, AdminUserAccountInventoryItem, AdminUserAssetSnapshot, AdminUserDetail, AdminUserLedgerHistoryItem } from '../../api/adminUsersApi';
import { isSystemUser, mapUserRestrictions, type UserRestrictionKey } from './userRestrictions';

type RestrictionAction = { user: AdminUser; kind: 'login' | 'withdrawal'; frozen: boolean };
type DrawerTab = 'overview' | 'assets' | 'security' | 'history';

type UserManagementDrawerProps = {
  user: AdminUser | null;
  detail?: AdminUserDetail;
  accounts?: AdminUserAccountInventoryItem[];
  assets?: AdminUserAssetSnapshot;
  history?: AdminUserLedgerHistoryItem[];
  loading: boolean;
  onClose: () => void;
  onRestrictionAction: (action: RestrictionAction) => void;
};

/**
 * 使用者高權限資料集中在右側管理抽屜，讓列表維持掃描用途，並為日後更多限制能力保留固定位置。
 */
export function UserManagementDrawer({ user, detail, accounts, assets, history, loading, onClose, onRestrictionAction }: UserManagementDrawerProps) {
  const { t } = useI18n();
  const [tab, setTab] = useState<DrawerTab>('overview');

  useEffect(() => {
    if (!user) return;
    setTab('overview');
    function closeOnEscape(event: KeyboardEvent) { if (event.key === 'Escape') onClose(); }
    document.addEventListener('keydown', closeOnEscape);
    return () => document.removeEventListener('keydown', closeOnEscape);
  }, [user?.userId, onClose]);

  if (!user) return null;
  const systemUser = isSystemUser(user);
  const tabs: Array<{ id: DrawerTab; label: string }> = [
    { id: 'overview', label: t('admin.usersDrawerOverview') },
    { id: 'assets', label: t('admin.usersDrawerAssets') },
    { id: 'security', label: t('admin.usersDrawerSecurity') },
    { id: 'history', label: t('admin.usersDrawerHistory') },
  ];

  return (
    <div className="admin-user-drawer-backdrop" role="presentation" onMouseDown={onClose}>
      <aside className="admin-user-drawer" role="dialog" aria-modal="true" aria-labelledby="admin-user-drawer-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="admin-user-drawer__header">
          <div className="admin-user-drawer__identity">
            <div><p className="eyebrow">{systemUser ? t('admin.usersSystemAccount') : t('admin.usersManagement')}</p><h2 id="admin-user-drawer-title">{user.displayName}</h2><p>{user.email}<CopyButton value={user.email} label={t('admin.usersCopyEmail')} copiedLabel={t('admin.usersCopied')} /></p><small>{user.userId}<CopyButton value={user.userId} label={t('admin.usersCopyUserId')} copiedLabel={t('admin.usersCopied')} /></small></div>
            <button className="ghost-button admin-user-drawer__close" type="button" aria-label={t('common.close')} onClick={onClose}><CloseIcon /></button>
          </div>
          <div className="admin-user-drawer__status"><Badge tone={systemUser ? 'neutral' : user.hasActiveRestriction ? 'warning' : user.status === 'ACTIVE' ? 'success' : 'danger'}>{systemUser ? t('admin.usersSystemAccount') : user.hasActiveRestriction ? t('admin.usersStatusFrozen') : user.status === 'ACTIVE' ? t('admin.usersStatusActive') : t('admin.usersStatusInactive')}</Badge></div>
        </header>
        <nav className="admin-user-drawer__tabs" aria-label={t('admin.usersManagement')}>
          {tabs.map((item) => <button key={item.id} className={tab === item.id ? 'is-active' : ''} type="button" onClick={() => setTab(item.id)}>{item.label}</button>)}
        </nav>
        <div className="admin-user-drawer__body">
          {loading ? <p className="admin-user-drawer__loading">{t('admin.usersDetailsLoading')}</p> : null}
          {!loading && tab === 'overview' ? <Overview user={user} detail={detail} accounts={accounts} systemUser={systemUser} /> : null}
          {!loading && tab === 'assets' ? <Assets assets={assets} /> : null}
          {!loading && tab === 'security' ? <Restrictions user={user} systemUser={systemUser} onRestrictionAction={onRestrictionAction} /> : null}
          {!loading && tab === 'history' ? <History history={history} /> : null}
        </div>
      </aside>
    </div>
  );
}

function Overview({ user, detail, accounts, systemUser }: Pick<UserManagementDrawerProps, 'user' | 'detail' | 'accounts'> & { user: AdminUser; systemUser: boolean }) {
  const { t } = useI18n();
  return <section className="admin-user-drawer__section"><h3>{t('admin.usersDrawerOverview')}</h3><dl className="admin-user-drawer__facts"><div><dt>{t('admin.usersDetailStatus')}</dt><dd>{systemUser ? t('admin.usersSystemAccount') : user.hasActiveRestriction ? t('admin.usersStatusFrozen') : user.status === 'ACTIVE' ? t('admin.usersStatusActive') : t('admin.usersStatusInactive')}</dd></div><div><dt>{t('admin.usersDetailRegistered')}</dt><dd>{formatTime(user.createdAt)}</dd></div><div><dt>{t('admin.usersDetailLastLogin')}</dt><dd>{systemUser ? '—' : user.lastLoginAt ? formatTime(user.lastLoginAt) : '—'}</dd></div><div><dt>{t('admin.usersDetailDevices')}</dt><dd>{systemUser ? t('admin.usersNotApplicable') : detail?.devices.map((device) => `${device.platform}／${device.label}`).join('、') || t('admin.usersDetailNoDevices')}</dd></div></dl><h3>{t('admin.usersAccounts')}</h3>{accounts?.length ? <div className="admin-user-drawer__accounts">{accounts.map((account) => <div key={account.accountId}><strong>{account.accountType}</strong><span>{account.accountStatus}</span></div>)}</div> : <EmptyDrawerState label={t('admin.usersNoAccounts')} />}</section>;
}

function Assets({ assets }: Pick<UserManagementDrawerProps, 'assets'>) {
  const { t } = useI18n();
  if (!assets?.items.length) return <EmptyDrawerState label={t('admin.usersDrawerNoAssets')} />;
  return <section className="admin-user-drawer__section"><h3>{t('admin.usersAssets')}</h3>{assets.items.map((asset) => <div className="admin-user-drawer__asset" key={`${asset.accountType}-${asset.assetSymbol}`}><strong>{asset.accountType} · {asset.assetSymbol}</strong><span>{t('admin.usersAssetAvailable')} {formatDecimalString(asset.available)}</span><span>{t('admin.usersAssetTotal')} {formatDecimalString(asset.total)}</span></div>)}</section>;
}

function Restrictions({ user, systemUser, onRestrictionAction }: { user: AdminUser; systemUser: boolean; onRestrictionAction: (action: RestrictionAction) => void }) {
  const { t } = useI18n();
  return <section className="admin-user-drawer__section"><h3>{t('admin.usersDrawerSecurity')}</h3><p className="admin-user-drawer__intro">{t('admin.usersDrawerSecurityDescription')}</p>{mapUserRestrictions(user).map((restriction) => <RestrictionRow key={restriction.key} restriction={restriction} user={user} disabled={systemUser} onRestrictionAction={onRestrictionAction} />)}</section>;
}

function RestrictionRow({ restriction, user, disabled, onRestrictionAction }: { restriction: ReturnType<typeof mapUserRestrictions>[number]; user: AdminUser; disabled: boolean; onRestrictionAction: (action: RestrictionAction) => void }) {
  const { t } = useI18n();
  const label = t(`admin.usersCapability.${restriction.key}`);
  const description = t(`admin.usersCapabilityDescription.${restriction.key}`);
  const actionable = restriction.available && !disabled;
  const kind = restriction.key as UserRestrictionKey;
  return <div className="admin-user-restriction-row"><div><strong>{label}</strong><p>{description}</p><span>{disabled ? t('admin.usersNotApplicable') : restriction.active ? t('admin.usersCapabilityRestricted') : t('admin.usersCapabilityAllowed')}</span></div>{actionable ? <button className={restriction.active ? 'secondary-button' : 'ghost-button'} type="button" onClick={() => onRestrictionAction({ user, kind: kind as 'login' | 'withdrawal', frozen: !restriction.active })}>{t(restriction.active ? 'admin.usersRestrictionUnfreeze' : 'admin.usersRestrictionFreeze')}</button> : <button className="ghost-button" type="button" disabled>{t('admin.usersCapabilityUnavailable')}</button>}</div>;
}

function History({ history }: Pick<UserManagementDrawerProps, 'history'>) {
  const { t } = useI18n();
  if (!history?.length) return <EmptyDrawerState label={t('admin.usersDrawerNoHistory')} />;
  return <section className="admin-user-drawer__section"><h3>{t('admin.usersDrawerHistory')}</h3>{history.map((item) => <div className="admin-user-drawer__history" key={item.entryId}><strong>{item.direction === 'CREDIT' ? '+' : '-'}{formatDecimalString(item.amount)} {item.assetSymbol}</strong><span>{item.referenceType} #{item.referenceId}</span><small>{formatTime(item.postedAt)}</small></div>)}</section>;
}

function EmptyDrawerState({ label }: { label: string }) { return <p className="admin-user-drawer__empty">{label}</p>; }
function CloseIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7.5 7.5 9 9M16.5 7.5l-9 9" /></svg>; }
