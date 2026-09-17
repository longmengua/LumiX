import { findAdminUsers, getAdminUser, getAdminUserAssets, type AdminUserAssetSnapshot } from '../../api/adminUsersApi';
import { normalizeAdminError } from '../../api/adminError';

export type AssetUserSearchResult = Awaited<ReturnType<typeof findAdminUsers>>['items'][number];
/** 用戶資產與使用者管理共用同一個 admin user search API，資產頁只在 adapter 層組合 projection 查詢。 */
export function searchAssetUsers(query?: string): Promise<AssetUserSearchResult[]> {
  const normalized = query?.trim() ?? '';
  // user_id 由註冊流程產生 UUID；已有的 detail API 可合法支援精確 UID 查詢，避免把 email 假裝成名稱前綴。
  if (isUserId(normalized)) {
    return getAdminUser(normalized)
      .then((detail) => [detail.user])
      .catch((error: unknown) => {
        if (normalizeAdminError(error).kind === 'not-found') return [];
        throw error;
      });
  }
  return findAdminUsers({ displayNamePrefix: normalized || undefined }).then((page) => page.items);
}
export function getAssetUserProjection(userId: string): Promise<AdminUserAssetSnapshot> { return getAdminUserAssets(userId); }

function isUserId(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}
