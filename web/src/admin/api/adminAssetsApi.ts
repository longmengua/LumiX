import { ADMIN_API_BASE_PATH } from './adminApi';
import { createAdminApiError, notifyAdminUnauthorized } from './adminError';

export type AssetAdjustmentType = 'MANUAL_CORRECTION' | 'COMPENSATION' | 'BUSINESS_REVERSAL';
export type AssetAdjustmentDirection = 'CREDIT' | 'DEBIT';
export type AdminAssetAdjustmentRequest = { adjustmentType: AssetAdjustmentType; userId?: string; accountType?: 'SPOT'; assetSymbol?: string; direction?: AssetAdjustmentDirection; amount: string; sourceBusinessType?: string; sourceBusinessId?: string; sourceLedgerEntryId?: string; incidentReference?: string; reason: string; };
export type AdminAssetAdjustmentResult = { adjustmentId: string; ledgerJournalId: string; replayed: boolean; };
export type AdminReversalSource = { ledgerEntryId: string; journalId: string | null; userId: string | null; userEmail: string | null; accountType: string | null; assetSymbol: string | null; originalDirection: AssetAdjustmentDirection | null; originalAmount: string | null; alreadyReversed: string | null; remainingReversible: string | null; sourceBusinessType: string | null; sourceBusinessId: string | null; eligible: boolean; ineligibleReason: string | null; };
export type AdminAssetAdjustmentOption = { assetSymbol: string; internalName: string; precisionScale: number; };
export type AdminSpotAssetConfiguration = AdminAssetAdjustmentOption & { status: 'ACTIVE' | 'HALTED' | 'DELISTED'; updatedAt: string; };
export type CreateAdminSpotAssetConfigurationRequest = { assetSymbol: string; internalName: string; precisionScale: number; };
export type AdminAssetReconciliationItem = {
  auditLogId: string;
  ledgerJournalId: string;
  actorId: string;
  targetUserId: string;
  targetUserEmail: string;
  accountType: string;
  assetSymbol: string | null;
  direction: 'CREDIT' | 'DEBIT' | null;
  amount: string | null;
  activityType: AssetAdjustmentType | 'AIRDROP';
  note: string;
  reconciliationStatus: 'VERIFIED' | 'EXCEPTION';
  postedAt: string;
};

/** 通用調整一律以 caller-level key 防止 browser 重送造成第二次 immutable ledger effect。 */
export async function createAdminAssetAdjustment(request: AdminAssetAdjustmentRequest, idempotencyKey: string): Promise<AdminAssetAdjustmentResult> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/adjustments`, { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json', 'Idempotency-Key': idempotencyKey }, body: JSON.stringify(request) });
  if (!response.ok) throw await adminAssetsError(response);
  return response.json() as Promise<AdminAssetAdjustmentResult>;
}

/** Business Reversal 的 source 資訊只能由後端 ledger lookup 提供，禁止 browser 自行推導方向或餘額。 */
export async function fetchAdminAssetAdjustmentReversalSource(ledgerEntryId: string): Promise<AdminReversalSource> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/adjustments/reversal-sources/${encodeURIComponent(ledgerEntryId)}`, { credentials: 'same-origin' });
  if (!response.ok) throw await adminAssetsError(response);
  return response.json() as Promise<AdminReversalSource>;
}

/** 可調整幣別由新 adjustment boundary 的現貨主檔提供；前端不能以常數或 mock 擴張可入帳資產範圍。 */
export async function fetchAdminAssetAdjustmentOptions(): Promise<AdminAssetAdjustmentOption[]> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/adjustments/configuration`, { credentials: 'same-origin' });
  if (!response.ok) {
    const error = await createAdminApiError(response);
    if (error.kind === 'unauthorized') notifyAdminUnauthorized();
    throw error;
  }
  const payload = await response.json() as { assets?: AdminAssetAdjustmentOption[] };
  return Array.isArray(payload.assets) ? payload.assets : [];
}

/**
 * 對賬畫面只讀既有管理操作、journal 與雙分錄的交叉檢查結果。
 * 不在 browser 推算帳務狀態，也不以假資料填補尚未導入的手續費與損益來源。
 */
export async function fetchAssetAdjustmentReconciliation(): Promise<AdminAssetReconciliationItem[]> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/audit/adjustments`, { credentials: 'same-origin' });
  if (!response.ok) {
    const error = await createAdminApiError(response);
    if (error.kind === 'unauthorized') notifyAdminUnauthorized();
    throw error;
  }
  const payload = await response.json() as { items?: AdminAssetReconciliationItem[] };
  return Array.isArray(payload.items) ? payload.items : [];
}


/** 現貨幣種設定讀取與空投選項分開，讓設定頁能呈現 HALTED 資產而空投只取得 ACTIVE 資產。 */
export async function fetchAdminSpotAssetConfigurations(): Promise<AdminSpotAssetConfiguration[]> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/spot-configurations`, { credentials: 'same-origin' });
  if (!response.ok) throw await adminAssetsError(response);
  return response.json() as Promise<AdminSpotAssetConfiguration[]>;
}

/** 新增資產預設停用，由後端寫入 audit evidence 後才回傳可供管理頁呈現的設定。 */
export async function createAdminSpotAssetConfiguration(request: CreateAdminSpotAssetConfigurationRequest): Promise<AdminSpotAssetConfiguration> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/spot-configurations`, {
    method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request),
  });
  if (!response.ok) throw await adminAssetsError(response);
  return response.json() as Promise<AdminSpotAssetConfiguration>;
}

/** 啟用或暫停同樣必須走後端最高管理員授權，browser 不可直接改寫任何設定快照。 */
export async function updateAdminSpotAssetConfigurationStatus(assetSymbol: string, status: 'ACTIVE' | 'HALTED'): Promise<AdminSpotAssetConfiguration> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/spot-configurations/${encodeURIComponent(assetSymbol)}/status`, {
    method: 'PATCH', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status }),
  });
  if (!response.ok) throw await adminAssetsError(response);
  return response.json() as Promise<AdminSpotAssetConfiguration>;
}

async function adminAssetsError(response: Response) {
  const error = await createAdminApiError(response);
  if (error.kind === 'unauthorized') notifyAdminUnauthorized();
  return error;
}
