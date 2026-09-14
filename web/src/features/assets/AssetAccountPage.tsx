import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { accountLabelKeyByTab, type AssetTabKey } from './assetAccountTypes';
import { AssetAccountTable } from './AssetAccountTable';
import { AssetProjectionEvidence } from './AssetProjectionEvidence';
import { AssetSectionNav } from './AssetSectionNav';
import { useAssetProjectionSnapshot } from './useAssetProjectionSnapshot';

type AssetAccountPageProps = {
  accountKey: AssetTabKey;
  title: string;
  description: string;
};

export function AssetAccountPage({ accountKey, title, description }: AssetAccountPageProps) {
  const { t } = useI18n();
  const { data, loading, errorCode, reload } = useAssetProjectionSnapshot();
  const account = data?.accounts.find((item) => item.key === accountKey) ?? null;

  return (
    <div className="stack assets-page">
      <PageHeader title={title} description={description} />
      <AssetSectionNav active={accountKey} />

      {loading ? <LoadingState title={t('assets.loadingTitle')} description={t('assets.loadingDescription')} /> : null}
      {errorCode ? <ErrorState title={t('assets.errorTitle')} description={t('assets.errorDescription')} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} /> : null}

      {!loading && !errorCode && data ? (
        <>
          {account ? <AssetProjectionEvidence account={account} /> : null}

          <Card title={t(accountLabelKeyByTab[accountKey])}>
            {account && account.items.length > 0
              ? <AssetAccountTable account={account} />
              : <EmptyState title={t('assets.noProjectionTitle')} description={t('assets.noProjectionDescription')} />}
          </Card>
        </>
      ) : null}
    </div>
  );
}
