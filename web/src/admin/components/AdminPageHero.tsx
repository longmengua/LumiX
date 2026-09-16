import { useId, type ReactNode } from 'react';
import './AdminPageHero.css';

type AdminPageHeroProps = {
  title: string;
  description: string;
  icon: ReactNode;
  chips?: readonly { id: string; label: string; icon?: ReactNode }[];
  illustration?: ReactNode;
  slogan?: string;
  supportingText?: string;
};

/** Hero 只組合呈現插槽，避免共用視覺元件綁定資產命令或頁面資料來源。 */
export function AdminPageHero({ title, description, icon, chips = [], illustration, slogan, supportingText }: AdminPageHeroProps) {
  const titleId = useId();
  return <header className="admin-page-hero" aria-labelledby={titleId}>
    <div className="admin-page-hero__layout">
      <div className="admin-page-hero__identity">
        <div className="admin-page-hero__icon" aria-hidden="true">{icon}</div>
        <div className="admin-page-hero__copy">
          <h2 id={titleId}>{title}</h2>
          <p>{description}</p>
          {chips.length > 0 && <ul className="admin-page-hero__chips">{chips.map((chip) => <li key={chip.id}><span aria-hidden="true">{chip.icon}</span>{chip.label}</li>)}</ul>}
        </div>
      </div>
      {(illustration || slogan) && <div className="admin-page-hero__visual">
        {illustration && <div className="admin-page-hero__illustration" aria-hidden="true">{illustration}</div>}
        {slogan && <div className="admin-page-hero__slogan"><strong>{slogan}</strong>{supportingText && <p>{supportingText}</p>}</div>}
      </div>}
    </div>
  </header>;
}
