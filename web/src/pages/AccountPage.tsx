import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';

import { Badge } from '../components/base/Badge';
import { Card } from '../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../components/base/State';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import { useI18n } from '../i18n';
import { AccountApiKeysPage as Phase7AccountApiKeysPage } from '../features/phase7/AccountApiKeysPage';
import { AccountNotificationsPage as Phase7AccountNotificationsPage } from '../features/phase7/AccountNotificationsPage';
import { accountNavItems } from '../features/navigation/accountNav';
import {
  fetchAccountDashboardMock,
  type AccountDashboardData,
  type AccountProfile,
  type AssetAccountSummary,
  type TimelineItem,
} from '../features/account/mockAccountService';
import { formatCurrency, formatAmount, formatTime, maskEmail, maskPhone } from '../utils/format';

export function AccountPage() {
  const { t } = useI18n();
  const [data, setData] = useState<AccountDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadAccount() {
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchAccountDashboardMock();
        if (alive) {
          setData(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load account data.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadAccount();

    return () => {
      alive = false;
    };
  }, []);

  const sidebarItems = useMemo(() => accountNavItems.map(({ to, labelKey, end }) => ({ to, label: t(labelKey), end })), [t]);

  return (
    <div className="two-column">
      <Sidebar title={t('account.sidebarTitle')} items={sidebarItems} />
      <div className="stack">
        <PageHeader title={t('account.pageTitle')} description={t('account.pageDescription')} />

        {loading ? <LoadingState title={t('account.loadingTitle')} description={t('account.loadingDescription')} /> : null}
        {error ? <ErrorState title={t('account.errorTitle')} description={error} action={<NavLink to="/account">{t('common.retry')}</NavLink>} /> : null}

        {!loading && !error && data ? (
          <Routes>
            <Route index element={<AccountOverviewPage profile={data.profile} />} />
            <Route path="security" element={<AccountSecurityPage securityItems={data.securityItems} />} />
            <Route path="kyc" element={<AccountKycPage profile={data.profile} />} />
            <Route path="assets" element={<AccountAssetsPage assetAccounts={data.assetAccounts} />} />
            <Route path="transfer" element={<AccountTransferPage />} />
            <Route path="api-keys" element={<Phase7AccountApiKeysPage />} />
            <Route path="notifications" element={<Phase7AccountNotificationsPage />} />
            <Route path="login-history" element={<AccountTimelinePage title={t('account.loginHistoryTitle')} items={data.loginHistory} emptyText={t('account.noLoginRecords')} />} />
            <Route path="security-logs" element={<AccountTimelinePage title={t('account.securityLogsTitle')} items={data.securityLogs} emptyText={t('account.noSecurityLogs')} />} />
            <Route path="preferences" element={<AccountPreferencesPage preferences={data.preferences} />} />
            <Route path="*" element={<Navigate replace to="/account" />} />
          </Routes>
        ) : null}
      </div>
    </div>
  );
}

function AccountOverviewPage({ profile }: { profile: AccountProfile }) {
  const { t } = useI18n();

  return (
    <div className="stack">
      <div className="dashboard-grid">
        <Card title={t('account.profileTitle')}>
          <StatList
            items={[
              ['UID', profile.uid],
              ['Email', maskEmail(profile.email)],
              ['Phone', maskPhone(profile.phone)],
              ['Registered', formatTime(profile.registeredAt)],
            ]}
          />
        </Card>
        <Card title={t('account.kycTitle')}>
          <StatList
            items={[
              ['Status', profile.kycStatus.replace(/_/g, ' ')],
              ['Level', profile.kycLevel],
              ['Limits', t('account.profileLimits')],
            ]}
          />
        </Card>
        <Card title={t('account.securityTitle')}>
          <StatList
            items={[
              ['Security Level', profile.securityLevel],
              ['2FA', t('account.security2fa')],
              ['Whitelist', t('account.securityWhitelist')],
            ]}
          />
        </Card>
        <Card title={t('account.assetsTitle')}>
          <StatList
            items={[
              ['Total Equity', formatCurrency(profile.totalEquity)],
              ['Spot', formatCurrency(profile.spotValue)],
              ['Futures', formatCurrency(profile.futuresValue)],
              ['Margin', formatCurrency(profile.marginValue)],
            ]}
          />
        </Card>
      </div>

      <Card title={t('account.riskTitle')}>
        <div className="stack">
          {profile.riskWarnings.map((warning) => (
            <div className="notice-row" key={warning}>
              <Badge tone="warning">{t('account.notice')}</Badge>
              <span>{warning}</span>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

function AccountSecurityPage({ securityItems }: { securityItems: Array<{ label: string; status: string; description: string }> }) {
  const { t } = useI18n();

  return (
    <Card title={t('account.securityCenterTitle')}>
      <div className="stack">
        {securityItems.map((item) => (
          <div className="account-row" key={item.label}>
            <div>
              <p className="account-row__title">{item.label}</p>
              <p className="account-row__meta">{item.description}</p>
            </div>
            <Badge tone={getSecurityTone(item.status)}>{item.status}</Badge>
          </div>
        ))}
      </div>
    </Card>
  );
}

function AccountKycPage({ profile }: { profile: AccountProfile }) {
  const { t } = useI18n();

  return (
    <div className="stack">
      <Card title={t('account.kycStatusTitle')}>
        <div className="dashboard-grid dashboard-grid--three">
          <StatTile label="Status" value={profile.kycStatus.replace(/_/g, ' ')} />
          <StatTile label="KYC Level" value={profile.kycLevel} />
          <StatTile label="Limits" value={t('account.profileLimits')} />
        </div>
      </Card>

      <Card title={t('account.kycDetailsTitle')}>
        <div className="stack">
          <div className="notice-row">
            <Badge tone={getKycTone(profile.kycStatus)}>{profile.kycStatus.replace(/_/g, ' ')}</Badge>
            <span>{profile.kycNotes}</span>
          </div>
          <div className="dashboard-grid dashboard-grid--three">
            <StatTile label="Withdrawal limit" value="$250,000 / day" />
            <StatTile label="Futures permission" value={t('account.security2fa')} />
            <StatTile label="Margin permission" value={t('account.security2fa')} />
          </div>
          <button className="primary-button" type="button">
            {t('account.submitKyc')}
          </button>
        </div>
      </Card>
    </div>
  );
}

function AccountAssetsPage({ assetAccounts }: { assetAccounts: AssetAccountSummary[] }) {
  const { t } = useI18n();
  const [activeTab, setActiveTab] = useState<AssetAccountSummary['accountType']>('Spot Account');
  const activeAccount = assetAccounts.find((account) => account.accountType === activeTab) ?? assetAccounts[0];

  return (
    <div className="stack">
      <Card title={t('account.assetSummaryTitle')}>
        <div className="tab-list">
          {assetAccounts.map((account) => (
            <button
              key={account.accountType}
              className={`tab-button${activeTab === account.accountType ? ' tab-button--active' : ''}`}
              type="button"
              onClick={() => setActiveTab(account.accountType)}
            >
              {account.accountType}
            </button>
          ))}
        </div>
      </Card>

      <Card title={activeAccount?.accountType ?? t('account.assetTableTitle')}>
        {activeAccount ? (
          <div className="asset-table">
            <div className="asset-table__head">
              <span>Asset</span>
              <span>Available</span>
              <span>Frozen</span>
              <span>Margin Used</span>
              <span>Debt</span>
              <span>Interest</span>
              <span>Equity</span>
              <span>Estimated Value</span>
              <span>Action</span>
            </div>
            {activeAccount.assets.map((asset) => (
              <div className="asset-table__row" key={`${activeAccount.accountType}-${asset.asset}`}>
                <strong>{asset.asset}</strong>
                <span>{formatAmount(asset.available, 4)}</span>
                <span>{formatAmount(asset.frozen, 4)}</span>
                <span>{formatAmount(asset.marginUsed, 4)}</span>
                <span>{formatAmount(asset.debt, 4)}</span>
                <span>{formatAmount(asset.interest, 4)}</span>
                <span>{formatAmount(asset.equity, 4)}</span>
                <span>{formatCurrency(asset.estimatedValue)}</span>
                <NavLink className="secondary-button" to="/account/transfer">
                  {t('account.transferAction')}
                </NavLink>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState title={t('account.noAssetsSelected')} description={t('account.noAssetsSelectedDescription')} />
        )}
      </Card>
    </div>
  );
}

function AccountTransferPage() {
  const { t } = useI18n();

  return (
    <div className="stack">
      <Card title={t('account.assetTransferTitle')}>
        <div className="transfer-grid">
          <label className="field">
            <span className="field__label">{t('account.fromAccount')}</span>
            <select className="input" defaultValue="spot">
              <option value="spot">{t('account.spotAccount')}</option>
              <option value="futures">{t('account.futuresAccount')}</option>
              <option value="margin">{t('account.marginAccount')}</option>
            </select>
          </label>
          <label className="field">
            <span className="field__label">{t('account.toAccount')}</span>
            <select className="input" defaultValue="futures">
              <option value="spot">{t('account.spotAccount')}</option>
              <option value="futures">{t('account.futuresAccount')}</option>
              <option value="margin">{t('account.marginAccount')}</option>
            </select>
          </label>
          <label className="field">
            <span className="field__label">{t('account.asset')}</span>
            <select className="input" defaultValue="usdt">
              <option value="usdt">USDT</option>
              <option value="btc">BTC</option>
              <option value="eth">ETH</option>
            </select>
          </label>
          <label className="field">
            <span className="field__label">{t('account.amount')}</span>
            <input className="input" defaultValue="1500" />
          </label>
        </div>

        <div className="transfer-actions">
          <button className="primary-button" type="button">
            {t('account.submitTransfer')}
          </button>
          <p className="auth-form__hint">{t('account.mockOnly')}</p>
        </div>
      </Card>

      <Card title={t('account.recentTransfersTitle')}>
        <EmptyState title={t('account.noTransferRecordsTitle')} description={t('account.noTransferRecordsDescription')} />
      </Card>
    </div>
  );
}

function AccountTimelinePage({
  title,
  items,
  emptyText,
}: {
  title: string;
  items: TimelineItem[];
  emptyText: string;
}) {
  return (
    <Card title={title}>
      {items.length > 0 ? (
        <div className="timeline-list">
          {items.map((item) => (
            <div className="timeline-item" key={`${item.title}-${item.createdAt}`}>
              <div>
                <p className="account-row__title">{item.title}</p>
                <p className="account-row__meta">{item.description}</p>
              </div>
              <span className="timeline-item__time">{formatTime(item.createdAt)}</span>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState title={title} description={emptyText} />
      )}
    </Card>
  );
}

function AccountPreferencesPage({ preferences }: { preferences: Array<{ label: string; value: string }> }) {
  const { t } = useI18n();

  return (
    <Card title={t('account.preferencesTitle')}>
      <div className="stack">
        {preferences.map((item) => (
          <div className="account-row" key={item.label}>
            <div>
              <p className="account-row__title">{item.label}</p>
              <p className="account-row__meta">{t('account.phasePlaceholder')}</p>
            </div>
            <span>{item.value}</span>
          </div>
        ))}
      </div>
    </Card>
  );
}

function StatTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="stat-card">
      <span className="stat-card__label">{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function getKycTone(status: AccountProfile['kycStatus']) {
  switch (status) {
    case 'verified':
      return 'success';
    case 'pending':
      return 'warning';
    case 'rejected':
      return 'danger';
    default:
      return 'neutral';
  }
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

function getSecurityTone(status: string) {
  const normalized = status.toLowerCase();
  if (normalized.includes('enabled') || normalized.includes('updated') || normalized.includes('active')) {
    return 'success';
  }
  if (normalized.includes('paused')) {
    return 'warning';
  }
  return 'neutral';
}
