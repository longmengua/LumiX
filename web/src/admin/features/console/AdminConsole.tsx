import { useEffect, useState, type ReactNode } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import { Badge } from '../../../components/base/Badge';
import { Card } from '../../../components/base/Card';
import { ConfirmDialog } from '../../../components/base/ConfirmDialog';
import { ErrorState, LoadingState } from '../../../components/base/State';
import { PageHeader } from '../../../components/layout/PageHeader';
import { useI18n } from '../../../i18n';
import {
  fetchAdminConsoleMock,
  type AdminConsoleSnapshot,
  type AdminMarketMakerRecord,
  type AdminUserRecord,
  type AdminWalletRecord,
} from './mockAdminService';

type ConfirmState = {
  title: string;
  description: string;
  confirmLabel: string;
  action: () => void;
};

export function AdminConsole() {
  const { t } = useI18n();
  const [data, setData] = useState<AdminConsoleSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirmState, setConfirmState] = useState<ConfirmState | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadAdminConsole() {
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
            <Route path="users" element={<AdminUsersPage users={data.users} onPrompt={openConfirm} />} />
            <Route path="assets" element={<AdminAssetsPage assets={data.assets} />} />
            <Route path="wallet" element={<AdminWalletPage wallets={data.wallets} onPrompt={openConfirm} />} />
            <Route path="spot" element={<AdminSpotPage markets={data.spotMarkets} onPrompt={openConfirm} />} />
            <Route path="futures" element={<AdminFuturesPage markets={data.futuresMarkets} onPrompt={openConfirm} />} />
            <Route path="margin" element={<AdminMarginPage marginAccounts={data.marginAccounts} />} />
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

function AdminUsersPage({ users, onPrompt }: { users: AdminUserRecord[]; onPrompt: (confirm: ConfirmState) => void }) {
  const { t } = useI18n();
  const [items, setItems] = useState(users);

  useEffect(() => {
    setItems(users);
  }, [users]);

  function promptToggle(user: AdminUserRecord) {
    const nextStatus = user.status === 'Frozen' ? 'Active' : 'Frozen';
    onPrompt({
      title: `${user.name} · ${nextStatus === 'Frozen' ? t('admin.user.freezeTitle') : t('admin.user.unfreezeTitle')}`,
      description: t('admin.user.stateChangeDescription', undefined, {
        email: user.email,
        status: nextStatus.toLowerCase(),
      }),
      confirmLabel: nextStatus === 'Frozen' ? t('admin.user.freeze') : t('admin.user.unfreeze'),
      action: () => setItems((current) => current.map((item) => (item.id === user.id ? { ...item, status: nextStatus } : item))),
    });
  }

  function promptReset2fa(user: AdminUserRecord) {
    onPrompt({
      title: `${user.name} · ${t('admin.user.reset2faTitle')}`,
      description: t('admin.user.reset2faDescription'),
      confirmLabel: t('admin.user.reset2fa'),
      action: () => setItems((current) => current.map((item) => (item.id === user.id ? { ...item, twoFactorState: 'Reset pending' } : item))),
    });
  }

  return (
    <Card title={t('admin.usersTitle')}>
      <AdminTable columns={[t('admin.column.id'), t('admin.column.user'), t('admin.column.status'), t('admin.column.role'), t('admin.column.kyc'), t('admin.column.2fa'), t('admin.column.lastLogin'), t('admin.column.actions')]}>
        {items.map((user) => (
          <AdminTableRow key={user.id}>
            <span>{user.id}</span>
            <div>
              <strong>{user.name}</strong>
              <p className="assets-metric__hint">{user.email}</p>
            </div>
            <Badge tone={getStatusTone(user.status)}>{user.status}</Badge>
            <span>{user.role}</span>
            <span>{user.kycLevel}</span>
            <Badge tone={user.twoFactorState === 'Enabled' ? 'success' : 'warning'}>{user.twoFactorState}</Badge>
            <span>{formatTime(user.lastLoginAt)}</span>
            <div className="hero-actions">
              <button className="secondary-button" type="button" onClick={() => promptToggle(user)}>
                {user.status === 'Frozen' ? t('admin.user.unfreeze') : t('admin.user.freeze')}
              </button>
              <button className="ghost-button" type="button" onClick={() => promptReset2fa(user)}>
                {t('admin.user.reset2fa')}
              </button>
            </div>
          </AdminTableRow>
        ))}
      </AdminTable>
    </Card>
  );
}

function AdminAssetsPage({ assets }: { assets: AdminConsoleSnapshot['assets'] }) {
  const { t } = useI18n();
  return (
    <Card title={t('admin.assetsTitle')}>
      <AdminTable columns={[t('admin.column.asset'), t('admin.column.spot'), t('admin.column.futures'), t('admin.column.margin'), t('admin.column.frozen'), t('admin.column.ledgerDelta')]}>
        {assets.map((asset) => (
          <AdminTableRow key={asset.asset}>
            <strong>{asset.asset}</strong>
            <span>{asset.spotBalance}</span>
            <span>{asset.futuresBalance}</span>
            <span>{asset.marginBalance}</span>
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

function AdminMarginPage({ marginAccounts }: { marginAccounts: AdminConsoleSnapshot['marginAccounts'] }) {
  const { t } = useI18n();
  return (
    <Card title={t('admin.marginTitle')}>
      <AdminTable columns={[t('admin.column.user'), t('admin.column.debt'), t('admin.column.interest'), t('admin.column.borrowStatus'), t('admin.column.riskRatio')]}>
        {marginAccounts.map((account) => (
          <AdminTableRow key={account.user}>
            <strong>{account.user}</strong>
            <span>{account.debt}</span>
            <span>{account.interest}</span>
            <Badge tone={account.borrowStatus === 'Active' ? 'success' : 'warning'}>{account.borrowStatus}</Badge>
            <span>{account.riskRatio}</span>
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

function AdminTable({ columns, children }: { columns: string[]; children: ReactNode }) {
  return (
    <div className="trading-table">
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
  if (status === 'Active' || status === 'Matched' || status === 'Approved' || status === 'Enabled') return 'success';
  if (status === 'Paused' || status === 'Investigating' || status === 'KYC Pending' || status === 'Reduce only') return 'warning';
  return 'danger';
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
