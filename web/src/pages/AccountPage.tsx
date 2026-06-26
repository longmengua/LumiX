import { Sidebar } from '../components/layout/Sidebar';
import { PageHeader } from '../components/layout/PageHeader';
import { Card } from '../components/base/Card';
import { accountNavItems } from '../features/navigation/accountNav';

export function AccountPage() {
  return (
    <div className="two-column">
      <Sidebar title="Account" items={accountNavItems.map(({ to, label }) => ({ to, label }))} />
      <div className="stack">
        <PageHeader title="Account" description="Personal center placeholder." />
        <Card title="Overview">
          <p>Account details will be expanded in Phase 4.</p>
        </Card>
      </div>
    </div>
  );
}

