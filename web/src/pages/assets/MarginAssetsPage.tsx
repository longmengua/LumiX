import { AssetAccountPage } from '../../features/assets/AssetAccountPage';
import { useI18n } from '../../i18n';

export function MarginAssetsPage() {
  const { t } = useI18n();

  return (
    <AssetAccountPage
      accountKey="margin"
      title={t('assets.marginTitle')}
      description={t('assets.marginDescription')}
    />
  );
}
