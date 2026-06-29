import { Badge } from '../../../components/base/Badge';
import { useI18n } from '../../../i18n';

type WalletSecurityStatusProps = {
  twoFactor: string;
  whitelist: string;
  riskReview: string;
  riskNote: string;
  eta?: string;
};

export function WalletSecurityStatus({ twoFactor, whitelist, riskReview, riskNote, eta }: WalletSecurityStatusProps) {
  const { t } = useI18n();

  return (
    <section className="card">
      <h2 className="card__title">{t('account.securityCenterTitle')}</h2>
      <div className="wallet-security-grid">
        <div className="wallet-security-item">
          <span className="wallet-security-item__label">2FA</span>
          <Badge tone={twoFactor === 'Enabled' ? 'success' : 'danger'}>{twoFactor}</Badge>
        </div>
        <div className="wallet-security-item">
          <span className="wallet-security-item__label">Address whitelist</span>
          <Badge tone={whitelist === 'Enabled' ? 'success' : 'warning'}>{whitelist}</Badge>
        </div>
        <div className="wallet-security-item">
          <span className="wallet-security-item__label">Risk review</span>
          <Badge tone={riskReview === 'Approved' ? 'success' : 'warning'}>{riskReview}</Badge>
        </div>
        {eta ? (
          <div className="wallet-security-item">
            <span className="wallet-security-item__label">ETA</span>
            <strong>{eta}</strong>
          </div>
        ) : null}
      </div>
      <p className="wallet-security-note">{riskNote}</p>
    </section>
  );
}
