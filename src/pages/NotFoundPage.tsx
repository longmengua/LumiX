import { Card } from '../components/base/Card';
import { PageHeader } from '../components/layout/PageHeader';

export function NotFoundPage() {
  return (
    <>
      <PageHeader title="404" description="Page not found." />
      <Card title="Unknown route">
        <p>This route is not implemented yet.</p>
      </Card>
    </>
  );
}

