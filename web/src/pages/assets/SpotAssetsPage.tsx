import { AssetAccountPage } from '../../features/assets/AssetAccountPage';

export function SpotAssetsPage() {
  return (
    <AssetAccountPage
      accountKey="spot"
      title="Spot Account"
      description="Spot balances remain isolated from futures and margin accounts."
      summaryTitle="Spot Snapshot"
      summaryPoints={[
        { label: 'Available Balance', value: '$12,480.28 USDT' },
        { label: 'Frozen Balance', value: '$180.40 USDT' },
        { label: 'Est. Spot Value', value: '$182,400.84' },
      ]}
    />
  );
}
