import { AssetAccountPage } from '../../features/assets/AssetAccountPage';
import { useI18n } from '../../i18n';

export function FuturesAssetsPage() {
  const { t } = useI18n();

  return (
    <AssetAccountPage
      accountKey="futures"
      title={t('assets.futuresTitle')}
      description={t('assets.futuresDescription')}
    />
  );
}
