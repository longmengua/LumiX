import { Badge } from '../../../components/base/Badge';
import { useI18n } from '../../../i18n';

type WalletAdapterNoticeProps = {
  notice: string;
};

export function WalletAdapterNotice({ notice }: WalletAdapterNoticeProps) {
  const { t } = useI18n();

  return (
    <section className="card wallet-adapter-notice">
      <div className="wallet-adapter-notice__header">
        <Badge tone="warning">{t('common.developmentAdapterOnly')}</Badge>
        <span className="wallet-adapter-notice__label">{t('common.olPrerequisite')}</span>
      </div>
      <p>{notice}</p>
    </section>
  );
}
