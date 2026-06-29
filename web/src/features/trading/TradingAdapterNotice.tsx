import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';

type TradingAdapterNoticeProps = {
  notice: string;
};

export function TradingAdapterNotice({ notice }: TradingAdapterNoticeProps) {
  const { t } = useI18n();

  return (
    <section className="card trading-adapter-notice">
      <div className="trading-adapter-notice__header">
        <Badge tone="warning">{t('trading.developmentOnly')}</Badge>
        <span className="trading-adapter-notice__label">{t('trading.adapterLabel')}</span>
      </div>
      <p>{notice}</p>
    </section>
  );
}
