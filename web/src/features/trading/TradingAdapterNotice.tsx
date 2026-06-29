import { Badge } from '../../components/base/Badge';

type TradingAdapterNoticeProps = {
  notice: string;
};

export function TradingAdapterNotice({ notice }: TradingAdapterNoticeProps) {
  return (
    <section className="card trading-adapter-notice">
      <div className="trading-adapter-notice__header">
        <Badge tone="warning">Development adapter only</Badge>
        <span className="trading-adapter-notice__label">OL prerequisite</span>
      </div>
      <p>{notice}</p>
    </section>
  );
}

