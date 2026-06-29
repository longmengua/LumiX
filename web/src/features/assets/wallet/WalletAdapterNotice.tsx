import { Badge } from '../../../components/base/Badge';

type WalletAdapterNoticeProps = {
  notice: string;
};

export function WalletAdapterNotice({ notice }: WalletAdapterNoticeProps) {
  return (
    <section className="card wallet-adapter-notice">
      <div className="wallet-adapter-notice__header">
        <Badge tone="warning">Development adapter</Badge>
        <span className="wallet-adapter-notice__label">OL prerequisite</span>
      </div>
      <p>{notice}</p>
    </section>
  );
}
