import { AssetAccountPage } from '../../features/assets/AssetAccountPage';
import { useI18n } from '../../i18n';

export function FuturesAssetsPage() {
  const { t } = useI18n();

  return (
    <AssetAccountPage
      accountKey="futures"
      title={t('assets.futuresTitle')}
      description={t('assets.futuresDescription')}
      summaryTitle={t('assets.futuresSnapshot')}
      summaryPoints={[
        { label: 'Wallet Balance', value: '$33,311.25 USDT' },
        { label: 'Margin Used', value: '$22,040.11 USDT' },
        { label: 'Unrealized PnL', value: '+$860.52 USDT' },
      ]}
    />
  );
}
