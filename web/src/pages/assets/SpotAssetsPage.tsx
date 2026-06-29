import { AssetAccountPage } from '../../features/assets/AssetAccountPage';
import { useI18n } from '../../i18n';

export function SpotAssetsPage() {
  const { t } = useI18n();

  return (
    <AssetAccountPage
      accountKey="spot"
      title={t('assets.spotTitle')}
      description={t('assets.spotDescription')}
      summaryTitle={t('assets.spotSnapshot')}
      summaryPoints={[
        { label: 'Available Balance', value: '$12,480.28 USDT' },
        { label: 'Frozen Balance', value: '$180.40 USDT' },
        { label: 'Est. Spot Value', value: '$182,400.84' },
      ]}
    />
  );
}
