import { useEffect, useState } from 'react';

import { EmptyState, ErrorState, LoadingState } from '../../../components/base/State';
import { CopyButton } from '../../../components/base/CopyButton';
import { useI18n } from '../../../i18n';
import { formatDecimalString } from '../../../utils/format';
import type { AdminUser, AdminUserAssetSnapshot } from '../../api/adminUsersApi';
import { getAssetUserProjection, searchAssetUsers } from './adminUserAssetSearch';

/** 使用既有管理使用者與餘額 projection API，從資產維度查找使用者，不自行彙總不同帳戶語意的餘額。 */
export function AssetUserSearchPanel() {
  const { t } = useI18n();
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState<AdminUser[] | null>(null);
  const [error, setError] = useState(false);
  const [selected, setSelected] = useState<AdminUser | null>(null);
  const [assets, setAssets] = useState<AdminUserAssetSnapshot | null>(null);
  const [assetError, setAssetError] = useState(false);

  function search() {
    setUsers(null); setError(false);
    void searchAssetUsers(query).then(setUsers).catch(() => setError(true));
  }
  useEffect(() => { search(); }, []);
  function open(user: AdminUser) {
    setSelected(user); setAssets(null); setAssetError(false);
    void getAssetUserProjection(user.userId).then(setAssets).catch(() => setAssetError(true));
  }

  return <section className="admin-asset-users" aria-label={t('admin.assetsUsersTitle')}>
    <header><h2>{t('admin.assetsUsersTitle')}</h2><p>{t('admin.assetsUsersDescription')}</p></header>
    <form className="admin-asset-users__search" onSubmit={(event) => { event.preventDefault(); search(); }}><input className="input" value={query} maxLength={128} placeholder={t('admin.assetsUsersSearchPlaceholder')} onChange={(event) => setQuery(event.target.value)} /><button className="primary-button" type="submit">{t('admin.usersSearch')}</button></form>
    <p className="admin-asset-users__search-hint">{t('admin.assetsUsersSearchCapability')}</p>
    {error ? <ErrorState title={t('admin.assetsUsersLoadErrorTitle')} description={t('admin.assetsUsersLoadErrorDescription')} action={<button className="secondary-button" type="button" onClick={search}>{t('common.retry')}</button>} /> : null}
    {users === null && !error ? <LoadingState title={t('admin.assetsUsersLoadingTitle')} description={t('admin.assetsUsersLoadingDescription')} /> : null}
    {users?.length === 0 ? <EmptyState title={t('admin.assetsUsersEmptyTitle')} description={t('admin.assetsUsersEmptyDescription')} /> : null}
    {users?.length ? <div className="admin-asset-users__table-wrap"><table><thead><tr><th>{t('admin.column.user')}</th><th>{t('admin.assetsUsersAccountData')}</th><th>{t('admin.column.actions')}</th></tr></thead><tbody>{users.map((user) => <tr key={user.userId}><td><strong>{user.displayName}</strong><small>{user.email}</small><small>{user.userId}</small></td><td>{t('admin.assetsUsersAssetDataAvailable')}</td><td><button className="secondary-button" type="button" onClick={() => open(user)}>{t('admin.assetsUsersView')}</button></td></tr>)}</tbody></table></div> : null}
    {selected ? <AssetUserDrawer user={selected} assets={assets} error={assetError} onClose={() => setSelected(null)} /> : null}
  </section>;
}

function AssetUserDrawer({ user, assets, error, onClose }: { user: AdminUser; assets: AdminUserAssetSnapshot | null; error: boolean; onClose: () => void }) {
  const { t } = useI18n();
  return <div className="admin-user-drawer-backdrop" role="presentation" onMouseDown={onClose}><aside className="admin-user-drawer" role="dialog" aria-modal="true" aria-labelledby="asset-user-drawer-title" onMouseDown={(event) => event.stopPropagation()}><header className="admin-user-drawer__header"><div className="admin-user-drawer__identity"><div><p className="eyebrow">{t('admin.assetsUsersTitle')}</p><h2 id="asset-user-drawer-title">{user.displayName}</h2><p>{user.email}<CopyButton value={user.email} label={t('admin.usersCopyEmail')} copiedLabel={t('admin.usersCopied')} /></p><small>{user.userId}<CopyButton value={user.userId} label={t('admin.usersCopyUserId')} copiedLabel={t('admin.usersCopied')} /></small></div><button className="ghost-button admin-user-drawer__close" type="button" aria-label={t('common.close')} onClick={onClose}>×</button></div></header><div className="admin-user-drawer__body"><section className="admin-user-drawer__section"><h3>{t('admin.assetsUsersDrawerAssets')}</h3>{error ? <EmptyState title={t('admin.assetsUsersLoadErrorTitle')} description={t('admin.assetsUsersLoadErrorDescription')} /> : assets === null ? <LoadingState title={t('admin.assetsUsersLoadingTitle')} description={t('admin.assetsUsersLoadingDescription')} /> : assets.items.length === 0 ? <EmptyState title={t('admin.assetsUsersDrawerEmptyTitle')} description={t('admin.assetsUsersDrawerEmptyDescription')} /> : assets.items.map((item) => <div className="admin-user-drawer__asset" key={`${item.accountType}-${item.assetSymbol}`}><strong>{item.assetSymbol}</strong><span>{item.accountType}</span><span>{t('admin.assetsUsersAvailable')} {formatDecimalString(item.available)}</span><span>{t('admin.assetsUsersLocked')} {formatDecimalString(item.locked)}</span><span>{t('admin.assetsUsersTotal')} {formatDecimalString(item.total)}</span></div>)}</section></div></aside></div>;
}
