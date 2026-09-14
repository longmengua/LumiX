export type AdminErrorKind = 'unauthorized' | 'forbidden' | 'not-found' | 'method-not-allowed' | 'gone' | 'server' | 'network' | 'unknown';

export class AdminApiError extends Error {
  constructor(readonly kind: AdminErrorKind, readonly status?: number) {
    super(kind);
    this.name = 'AdminApiError';
  }
}

/** API 失敗只正規化成安全的語意，避免 endpoint、後端例外或權限細節流入管理畫面。 */
export async function createAdminApiError(response: Response): Promise<AdminApiError> {
  const status = response.status;
  const kind: AdminErrorKind = status === 401 ? 'unauthorized'
    : status === 403 ? 'forbidden'
      : status === 404 ? 'not-found'
        : status === 405 ? 'method-not-allowed'
          : status === 410 ? 'gone'
            : status >= 500 ? 'server'
              : 'unknown';
  return new AdminApiError(kind, status);
}

/** 網路層沒有 HTTP response 時不得誤報資源不存在。 */
export function normalizeAdminError(error: unknown): AdminApiError {
  if (error instanceof AdminApiError) return error;
  if (error instanceof TypeError) return new AdminApiError('network');
  return new AdminApiError('unknown');
}

export const ADMIN_UNAUTHORIZED_EVENT = 'lumix:admin-unauthorized';

/** 401 由 auth provider 統一清除 session；其他 HTTP error 必須留在原本資料／操作 context 處理。 */
export function notifyAdminUnauthorized() {
  window.dispatchEvent(new Event(ADMIN_UNAUTHORIZED_EVENT));
}
