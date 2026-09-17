import { useSearchParams } from 'react-router-dom';

import { EmptyState } from '../../../components/base/State';
import { useI18n } from '../../../i18n';
import { AssetAdjustmentAuditPanel } from './AssetAdjustmentAuditPanel';

type AnalysisTab = 'overview' | 'ledger' | 'reconciliation' | 'revenue-share' | 'organization';
const TABS: AnalysisTab[] = ['overview', 'ledger', 'reconciliation', 'revenue-share', 'organization'];

/** URL 驅動的二級分析導覽；只有對帳核驗接上真實 server-side reconciliation evidence。 */
export function AssetAnalysisPanel() {
  const { t } = useI18n();
  const [parameters, setParameters] = useSearchParams();
  const requested = parameters.get('tab') as AnalysisTab | null;
  const tab = requested && TABS.includes(requested) ? requested : 'overview';
  return <section className="admin-asset-analysis" aria-label={t('admin.assetsAnalysisTitle')}>
    <header><h2>{t('admin.assetsAnalysisTitle')}</h2><p>{t('admin.assetsAnalysisDescription')}</p></header>
    <nav className="admin-asset-analysis__tabs" aria-label={t('admin.assetsAnalysisTitle')}>{TABS.map((item) => <button className={tab === item ? 'is-active' : ''} type="button" key={item} onClick={() => setParameters(item === 'overview' ? {} : { tab: item })}>{t(`admin.assetsAnalysisTab.${item}`)}</button>)}</nav>
    {tab === 'reconciliation' ? <AssetAdjustmentAuditPanel /> : <EmptyState title={t(`admin.assetsAnalysisEmptyTitle.${tab}`)} description={t(`admin.assetsAnalysisEmptyDescription.${tab}`)} />}
  </section>;
}
