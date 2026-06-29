import { Badge } from '../../components/base/Badge';
import { EmptyState } from '../../components/base/State';
import { formatAmount, formatTime } from '../../utils/format';
import type { AssetHistoryRecord } from './mockAssetService';

type AssetHistoryListProps = {
  title?: string;
  history: AssetHistoryRecord[];
  emptyTitle?: string;
  emptyDescription?: string;
};

export function AssetHistoryList({
  title = 'Recent Asset History',
  history,
  emptyTitle = 'No asset history',
  emptyDescription = 'Asset history will appear after the first transfer or settlement event.',
}: AssetHistoryListProps) {
  return (
    <div className="card">
      <h2 className="card__title">{title}</h2>
      {history.length > 0 ? (
        <div className="history-list">
          {history.map((item) => (
            <article className="history-item" key={`${item.time}-${item.account}-${item.asset}`}>
              <div className="history-item__main">
                <div className="history-item__title-row">
                  <p className="account-row__title">{item.changeType}</p>
                  <Badge tone={getHistoryTone(item.status)}>{item.status}</Badge>
                </div>
                <p className="account-row__meta">
                  {item.account} · {item.asset}
                </p>
              </div>
              <div className="history-item__meta">
                <strong>{formatAmount(item.amount, getFractionDigits(item.asset))}</strong>
                <span>{formatTime(item.time)}</span>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState title={emptyTitle} description={emptyDescription} />
      )}
    </div>
  );
}

function getFractionDigits(asset: string) {
  return asset === 'USDT' ? 2 : 4;
}

function getHistoryTone(status: AssetHistoryRecord['status']) {
  switch (status) {
    case 'Completed':
      return 'success';
    case 'Pending':
      return 'warning';
    case 'Rejected':
      return 'danger';
    default:
      return 'neutral';
  }
}
