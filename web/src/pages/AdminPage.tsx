import { Sidebar } from '../components/layout/Sidebar';
import { PageHeader } from '../components/layout/PageHeader';
import { Card } from '../components/base/Card';
import { adminNavItems } from '../features/navigation/adminNav';

export function AdminPage() {
  return (
    <div className="two-column">
      <Sidebar title="Admin" items={adminNavItems.map(({ to, label }) => ({ to, label }))} />
      <div className="stack">
        <PageHeader title="Admin" description="Back-office placeholder." />
        <Card title="Dashboard">
          <p>Admin console pages will be added in Phase 8.</p>
        </Card>
      </div>
    </div>
  );
}

