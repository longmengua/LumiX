import { AssetAccountPage } from '../../features/assets/AssetAccountPage';
import { useI18n } from '../../i18n';

export function SpotAssetsPage() {
  const { t } = useI18n();

  return (
    <AssetAccountPage
      accountKey="spot"
      title={t('assets.spotTitle')}
      description={t('assets.spotDescription')}
    />
  );
}
