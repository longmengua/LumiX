import { Badge } from '../../components/base/Badge';

/**
 * P30 的前端可信資料狀態。
 *
 * browser 不可自行推論資金或市場真相；只根據 API contract 顯式傳回的狀態決定是否提示使用者可操作。
 */
export type TrustedDataState = 'live' | 'stale' | 'degraded' | 'sandbox' | 'unavailable';

type TrustedDataNoticeProps = {
  state: TrustedDataState;
  asOf: string;
  source: string;
};

const presentation: Record<TrustedDataState, { label: string; tone: 'success' | 'warning' | 'danger' | 'neutral'; description: string }> = {
  live: { label: '資料正常', tone: 'success', description: '資料來源健康；仍須由伺服器執行所有風控與資金決策。' },
  stale: { label: '資料過期', tone: 'warning', description: '資料不是即時狀態；敏感操作必須保持停用。' },
  degraded: { label: '資料降級', tone: 'warning', description: '資料完整性或來源健康度不足；不可據此判斷交易或資產真相。' },
  sandbox: { label: 'Sandbox', tone: 'warning', description: '這是開發／模擬資料，絕非正式交易或資金服務。' },
  unavailable: { label: '資料不可用', tone: 'danger', description: '無法取得可驗證資料；請勿嘗試敏感操作。' },
};

/** 顯示 source/as-of 與信任狀態，讓每個 UX flow 都能避免把 mock 或 stale data 誤當 production data。 */
export function TrustedDataNotice({ state, asOf, source }: TrustedDataNoticeProps) {
  const item = presentation[state];
  return (
    <section className="card" aria-live="polite" data-trusted-state={state}>
      <Badge tone={item.tone}>{item.label}</Badge>
      <p>{item.description}</p>
      <small>來源：{source}；資料時間：{asOf}</small>
    </section>
  );
}

/** 唯一可供敏感 UX 啟用控制項使用的判定；非 live 永遠保持 disabled。 */
export function canEnableSensitiveUserAction(state: TrustedDataState): boolean {
  return state === 'live';
}
