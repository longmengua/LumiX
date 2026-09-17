import { ADMIN_API_BASE_PATH } from './adminApi';
import { createAdminApiError, notifyAdminUnauthorized } from './adminError';

export type AdminAirdropRequest = { targetUserId: string; assetSymbol: string; amount: string; activityId: string; reason: string; };
export type AdminAirdropResult = { ledgerJournalId: string; replayed: boolean; };
export type AdminAirdropAssetOption = { assetSymbol: string; internalName: string; precisionScale: number; };
export type AdminSpotAssetConfiguration = AdminAirdropAssetOption & { status: 'ACTIVE' | 'HALTED' | 'DELISTED'; updatedAt: string; };
export type CreateAdminSpotAssetConfigurationRequest = { assetSymbol: string; internalName: string; precisionScale: number; };
export type AdminAssetAdjustmentAuditItem = {
  auditLogId: string;
  ledgerJournalId: string;
  actorId: string;
  targetUserId: string;
  targetUserEmail: string;
  accountType: string;
  assetSymbol: string | null;
  direction: 'CREDIT' | 'DEBIT' | null;
  amount: string | null;
  activityType: 'AIRDROP' | 'REVERSAL' | 'UNKNOWN';
  note: string;
  reconciliationStatus: 'VERIFIED' | 'EXCEPTION';
  postedAt: string;
};

/** 空投金額維持 decimal 字串，且只有 HTTP 成功回應才能顯示成功 journal。 */
export async function createAdminAirdrop(request: AdminAirdropRequest): Promise<AdminAirdropResult> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/airdrops`, {
    method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(request),
  });
  if (!response.ok) {
    const error = await createAdminApiError(response);
    if (error.kind === 'unauthorized') notifyAdminUnauthorized();
    throw error;
  }
  return response.json() as Promise<AdminAirdropResult>;
}

/** 可空投幣別由後端現貨主檔提供；前端不能以常數或 mock 擴張可入帳資產範圍。 */
export async function fetchAdminAirdropAssetOptions(): Promise<AdminAirdropAssetOption[]> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/airdrops/configuration`, { credentials: 'same-origin' });
  if (!response.ok) {
    const error = await createAdminApiError(response);
    if (error.kind === 'unauthorized') notifyAdminUnauthorized();
    throw error;
  }
  const payload = await response.json() as { assets?: AdminAirdropAssetOption[] };
  return Array.isArray(payload.assets) ? payload.assets : [];
}

/**
 * 對賬畫面只讀既有管理操作、journal 與雙分錄的交叉檢查結果。
 * 不在 browser 推算帳務狀態，也不以假資料填補尚未導入的手續費與損益來源。
 */
export async function fetchAdminAssetAdjustmentAudit(): Promise<AdminAssetAdjustmentAuditItem[]> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/assets/audit/adjustments`, { credentials: 'same-origin' });
  if (!response.ok) {
    const error = await createAdminApiError(response);
    if (error.kind === 'unauthorized') notifyAdminUnauthorized();
    throw error;
  }
  const payload = await response.json() as { items?: AdminAssetAdjustmentAuditItem[] };
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
