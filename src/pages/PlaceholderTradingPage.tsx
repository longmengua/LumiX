import { useParams } from 'react-router-dom';

import { Card } from '../components/base/Card';
import { PageHeader } from '../components/layout/PageHeader';

type PlaceholderTradingPageProps = {
  kind: 'Spot' | 'Futures' | 'Margin';
};

export function PlaceholderTradingPage({ kind }: PlaceholderTradingPageProps) {
  const params = useParams<{ symbol: string }>();
  const symbol = params.symbol ?? 'BTC-USDT';

  return (
    <>
      <PageHeader title={`${kind} Trading`} description={`${kind} page placeholder for ${symbol}.`} />
      <Card title={`${kind} ${symbol}`}>
        <p>Trading interactions will be expanded in later phases.</p>
      </Card>
    </>
  );
}

