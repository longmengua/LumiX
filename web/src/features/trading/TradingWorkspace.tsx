import { SpotReferenceMarket } from './SpotReferenceMarket';

type TradingWorkspaceProps = { kind: 'spot' | 'futures'; symbol: string };

/** 現貨與合約共用真實 API 面板及版型；永續能力由後端風險 gate 控制，不載入模擬資金或假送單。 */
export function TradingWorkspace({ kind, symbol }: TradingWorkspaceProps) {
  return <SpotReferenceMarket key={`${kind}:${symbol}`} kind={kind} symbol={symbol} />;
}
