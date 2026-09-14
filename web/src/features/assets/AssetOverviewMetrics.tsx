import { Card } from '../../components/base/Card';
import { useI18n } from '../../i18n';
import { accountLabelKeyByTab } from './assetAccountTypes';
import type { AssetProjectionAccount } from './assetProjectionApi';

/** 跨帳戶不做估值加總；沒有可信價格來源時，不能把不同 asset 的 amount 偽裝成同一貨幣總權益。 */
export function AssetOverviewMetrics({ accounts }: { accounts: AssetProjectionAccount[] }) {
  const { t } = useI18n();

  return (
    <section className="assets-metrics">
      {accounts.map((account) => (
        <Card key={account.key} title={t(accountLabelKeyByTab[account.key])}>
          <div className="assets-metric">
            <strong className="assets-metric__value">{account.items.length}</strong>
            <p className="assets-metric__hint">{t('assets.projectedAssetCount')}</p>
          </div>
        </Card>
      ))}
    </section>
  );
}
