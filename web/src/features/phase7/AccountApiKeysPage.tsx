import { useEffect, useMemo, useState } from 'react';

import { Badge } from '../../components/base/Badge';
import { Card } from '../../components/base/Card';
import { ErrorState, LoadingState } from '../../components/base/State';
import { useI18n } from '../../i18n';
import { createDevelopmentApiKeyPreview, fetchApiKeyCenterMock, type ApiPermission, type ManagedApiKey } from './mockPhase7Service';
import { ApiKeyList } from './Phase7Tables';

const permissionOptions: Array<{ value: ApiPermission; label: string; description: string }> = [
  { value: 'read', label: 'Read', description: 'Default access for market and account lookups.' },
  { value: 'spot trade', label: 'Spot trade', description: 'Simulated spot order preview only.' },
  { value: 'futures trade', label: 'Futures trade', description: 'Simulated futures order preview only.' },
  { value: 'margin trade', label: 'Margin trade', description: 'Simulated margin order preview only.' },
  { value: 'withdraw', label: 'Withdraw', description: 'Default off. OL must stay disabled until the real security review.' },
];

export function AccountApiKeysPage() {
  const { t } = useI18n();
  const [keys, setKeys] = useState<ManagedApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [recentSecret, setRecentSecret] = useState<string | null>(null);
  const [recentName, setRecentName] = useState<string | null>(null);
  const [adapterNotice, setAdapterNotice] = useState(t('common.developmentAdapterOnly'));
  const [secretPolicy, setSecretPolicy] = useState('Secret appears once on creation. It is never written back into the adapter snapshot.');
  const [name, setName] = useState('LLM-BOT-DEV');
  const [ipWhitelist, setIpWhitelist] = useState('198.51.100.10');
  const [selectedPermissions, setSelectedPermissions] = useState<ApiPermission[]>(['read']);

  useEffect(() => {
    let alive = true;

    async function loadKeys() {
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchApiKeyCenterMock();
        if (alive) {
          setKeys(snapshot.keys);
          setAdapterNotice(snapshot.adapterNotice);
          setSecretPolicy(snapshot.secretPolicy);
          const defaultPermissions = snapshot.permissionDefaults.filter((permission) => permission !== 'withdraw') as ApiPermission[];
          setSelectedPermissions(defaultPermissions);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : t('state.noApiKeysDescription'));
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadKeys();

    return () => {
      alive = false;
    };
  }, []);

  const permissionSummary = useMemo(
    () => selectedPermissions.join(', ') || 'read',
    [selectedPermissions],
  );

  function togglePermission(permission: ApiPermission) {
    setSelectedPermissions((current) => {
      if (permission === 'withdraw') {
        return current.includes(permission) ? current.filter((item) => item !== permission) : current;
      }

      return current.includes(permission)
        ? current.filter((item) => item !== permission)
        : [...current, permission];
    });
  }

  function handleCreateKey() {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return;
    }

    const nextPermissions = Array.from(new Set<ApiPermission>([...selectedPermissions, 'read']));
    const preview = createDevelopmentApiKeyPreview({
      name: trimmedName,
      permissions: nextPermissions,
      ipWhitelist,
    });

    setKeys((current) => [preview.key, ...current]);
    setRecentName(trimmedName);
    setRecentSecret(preview.secretOnce);
    setName('');
    setIpWhitelist('198.51.100.10');
    setSelectedPermissions(['read']);
  }

  return (
    <div className="stack">
      {loading ? <LoadingState title={t('account.loadingTitle')} description={t('account.loadingDescription')} /> : null}
      {error ? <ErrorState title={t('state.noApiKeysTitle')} description={error} /> : null}

      {!loading && !error ? (
        <>
          <Card title={t('nav.account.apiKeys')}>
            <div className="stack">
              <div className="notice-row">
                <Badge tone="warning">{t('common.developmentAdapterOnly')}</Badge>
                <span>{adapterNotice}</span>
              </div>

              <p className="auth-form__hint">{secretPolicy}</p>

              <div className="phase7-api-key-layout">
                <div className="phase7-api-key-form">
                  <label className="field">
                    <span className="field__label">Key name</span>
                    <input className="input" value={name} onChange={(event) => setName(event.target.value)} placeholder="MM-BOT-DEV" />
                  </label>

                  <label className="field">
                    <span className="field__label">IP whitelist</span>
                    <input className="input" value={ipWhitelist} onChange={(event) => setIpWhitelist(event.target.value)} placeholder="198.51.100.10" />
                  </label>

                  <div className="stack">
                    <span className="field__label">Permissions</span>
                    <div className="phase7-permission-grid">
                      {permissionOptions.map((permission) => (
                        <label className="phase7-permission-item" key={permission.value}>
                          <div className="checkbox">
                            <input
                              checked={selectedPermissions.includes(permission.value)}
                              disabled={permission.value === 'withdraw'}
                              type="checkbox"
                              onChange={() => togglePermission(permission.value)}
                            />
                            <strong>{permission.label}</strong>
                          </div>
                          <p>{permission.description}</p>
                        </label>
                      ))}
                    </div>
                  </div>

                  <div className="phase7-api-key-form__summary">
                    <div className="stat-card">
                      <span className="stat-card__label">Permission summary</span>
                      <strong>{permissionSummary}</strong>
                    </div>
                    <div className="stat-card">
                      <span className="stat-card__label">Withdraw permission</span>
                      <strong>{selectedPermissions.includes('withdraw') ? 'Offered' : 'Default off'}</strong>
                    </div>
                  </div>

                  <button className="primary-button" type="button" onClick={handleCreateKey}>
                    Create development key
                  </button>
                </div>

                <div className="stack">
                  {recentSecret ? (
                    <Card title="Secret shown once">
                      <div className="stack">
                        <div className="notice-row">
                          <Badge tone="danger">One-time view</Badge>
                          <span>{recentName ?? 'New API key'} created in memory only.</span>
                        </div>
                        <p className="phase7-secret-token">{recentSecret}</p>
                        <p className="auth-form__hint">Copy it now if you need it. Refreshing or leaving this page clears the one-time view.</p>
                        <button className="secondary-button" type="button" onClick={() => setRecentSecret(null)}>
                          Dismiss
                        </button>
                      </div>
                    </Card>
                  ) : null}

                  <Card title="Current keys">
                    <ApiKeyList keys={keys} />
                  </Card>
                </div>
              </div>
            </div>
          </Card>
        </>
      ) : null}
    </div>
  );
}
