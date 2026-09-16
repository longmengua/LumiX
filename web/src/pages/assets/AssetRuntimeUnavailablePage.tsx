import { ErrorState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { useI18n } from '../../i18n';

/**
 * 入金、提款與帳戶劃轉尚未取得真實 runtime 時的正式路由保護頁。
 *
 * <p>保留 route 可避免舊書籤誤導至 404，但絕不載入 mock adapter 或呈現看似可動用真實資產的操作。</p>
 */
export function AssetRuntimeUnavailablePage() {
  const { t } = useI18n();
  return <div className="stack assets-page"><PageHeader title={t('assets.unavailableTitle')} description={t('assets.unavailableDescription')} /><AssetSectionNav /><ErrorState title={t('assets.unavailableStateTitle')} description={t('assets.unavailableStateDescription')} /></div>;
}
