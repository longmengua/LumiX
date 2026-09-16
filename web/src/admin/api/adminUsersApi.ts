import { ADMIN_API_BASE_PATH } from './adminApi';
import { createAdminApiError, notifyAdminUnauthorized } from './adminError';

export type AdminUser = {
  userId: string;
  email: string;
  displayName: string;
  status: string;
  createdAt: string;
  lastLoginAt: string | null;
  fundTransferRestrictedUntil: string | null;
  hasActiveRestriction: boolean;
};

export type AdminUserDetail = {
  user: AdminUser;
  devices: Array<{ platform: string; label: string; lastSeenAt: string }>;
};
export type AdminUserAssetProjection = { accountType: string; assetSymbol: string; total: string; available: string; locked: string; projectionVersion: number; projectedAt: string; reconciledAt: string | null; };
export type AdminUserAssetSnapshot = { source: 'BALANCE_PROJECTION'; items: AdminUserAssetProjection[]; };
export type AdminUserLedgerHistoryItem = { entryId: string; accountType: string; assetSymbol: string; direction: 'CREDIT' | 'DEBIT'; amount: string; referenceType: string; referenceId: string; postedAt: string; };
/** 帳戶容器不含任何金額，前端不得將它當成零餘額 projection。 */
export type AdminUserAccountInventoryItem = { accountId: string; accountType: string; accountStatus: string; createdAt: string; };

export type AdminUserSearchCursor = {
  createdAt: string;
  userId: string;
};

export type AdminUserSearch = {
  displayNamePrefix?: string;
  createdFrom?: string;
  createdBefore?: string;
  lastLoginFrom?: string;
  lastLoginBefore?: string;
  cursor?: AdminUserSearchCursor;
};

export type AdminUserSearchPage = {
  items: AdminUser[];
  nextCursor: AdminUserSearchCursor | null;
  /** 同一篩選條件的完整筆數，不會因為目前 cursor 而縮小。 */
  total: number;
  /** 後端實際採用的 page size，供前端計算總頁數而非猜測預設值。 */
  pageSize: number;
};

/**
 * 管理端只傳送已套用的篩選條件，避免輸入中的每個字元都觸發讀取使用者資料。
 * API 的名稱條件是 prefix 搜尋，不能在此自行改成前綴萬用字元。
 */
export function findAdminUsers(search: AdminUserSearch): Promise<AdminUserSearchPage> {
  const parameters = new URLSearchParams();
  if (search.displayNamePrefix) parameters.set('displayNamePrefix', search.displayNamePrefix);
  if (search.createdFrom) parameters.set('createdFrom', search.createdFrom);
  if (search.createdBefore) parameters.set('createdBefore', search.createdBefore);
  if (search.lastLoginFrom) parameters.set('lastLoginFrom', search.lastLoginFrom);
  if (search.lastLoginBefore) parameters.set('lastLoginBefore', search.lastLoginBefore);
  if (search.cursor) {
    parameters.set('cursorCreatedAt', search.cursor.createdAt);
    parameters.set('cursorUserId', search.cursor.userId);
  }

  const query = parameters.size > 0 ? `?${parameters.toString()}` : '';
  return read<AdminUserSearchPage>(`/users${query}`);
}

export function getAdminUser(id: string): Promise<AdminUserDetail> {
  return read<AdminUserDetail>(`/users/${encodeURIComponent(id)}`);
}
/** 管理端資產只讀取現有 projection；不提供餘額調整、空投或任何資金寫入 action。 */
export function getAdminUserAssets(id: string): Promise<AdminUserAssetSnapshot> { return read<AdminUserAssetSnapshot>(`/users/${encodeURIComponent(id)}/assets`); }
export function getAdminUserAssetHistory(id: string): Promise<AdminUserLedgerHistoryItem[]> { return read<AdminUserLedgerHistoryItem[]>(`/users/${encodeURIComponent(id)}/assets/history`); }
export function getAdminUserAccounts(id: string): Promise<AdminUserAccountInventoryItem[]> { return read<AdminUserAccountInventoryItem[]>(`/users/${encodeURIComponent(id)}/assets/accounts`); }

async function read<T>(path: string): Promise<T> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}${path}`, { credentials: 'same-origin' });
  if (!response.ok) {
    const error = await createAdminApiError(response);
    if (error.kind === 'unauthorized') notifyAdminUnauthorized();
    throw error;
  }
  return response.json() as Promise<T>;
}
