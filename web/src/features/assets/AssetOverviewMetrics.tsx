import { Card } from '../../components/base/Card';
import type { AssetOverviewData } from './mockAssetService';

export function AssetOverviewMetrics({ metrics }: Pick<AssetOverviewData, 'metrics'>) {
  return (
    <section className="assets-metrics">
      {metrics.map((metric) => (
        <Card key={metric.label} title={metric.label}>
          <div className="assets-metric">
            <strong className="assets-metric__value">{metric.value}</strong>
            <p className="assets-metric__hint">{metric.hint}</p>
          </div>
        </Card>
      ))}
    </section>
  );
}
