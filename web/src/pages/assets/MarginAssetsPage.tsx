import { AssetAccountPage } from '../../features/assets/AssetAccountPage';
import { useI18n } from '../../i18n';

export function MarginAssetsPage() {
  const { t } = useI18n();

  return (
    <AssetAccountPage
      accountKey="margin"
      title={t('assets.marginTitle')}
      description={t('assets.marginDescription')}
      summaryTitle={t('assets.marginSnapshot')}
      summaryPoints={[
        { label: 'Net Asset', value: '$21,120.50 USDT' },
        { label: 'Borrowed', value: '$6,800.05 USDT' },
        { label: 'Risk Ratio', value: '62.4%' },
      ]}
    />
  );
}
