import type { ReactNode } from 'react';

type StateProps = {
  title: string;
  description?: string;
  action?: ReactNode;
};

export function LoadingState({ title, description, action }: StateProps) {
  return <StateBox tone="loading" title={title} description={description} action={action} />;
}

export function EmptyState({ title, description, action }: StateProps) {
  return <StateBox tone="empty" title={title} description={description} action={action} />;
}

export function ErrorState({ title, description, action }: StateProps) {
  return <StateBox tone="error" title={title} description={description} action={action} />;
}

function StateBox({
  tone,
  title,
  description,
  action,
}: StateProps & { tone: 'loading' | 'empty' | 'error' }) {
  return (
    <section className={`state-box state-box--${tone}`}>
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
      {action ? <div className="state-box__action">{action}</div> : null}
    </section>
  );
}

