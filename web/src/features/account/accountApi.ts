/**
 * 帳戶頁已接入的使用者私有 API。
 *
 * <p>session 一律由同源 HttpOnly Cookie 自動附帶；這個 adapter 只接受 server 已去敏的登入時間，
 * 不保存或處理任何 session token。</p>
 */
export type LoginHistoryRecord = {
  occurredAt: string;
};

type LoginHistoryResponse = {
  records: LoginHistoryRecord[];
};

type ApiError = {
  code?: string;
};

/** 取得目前登入使用者自己的成功登入歷程；未授權時交由路由與呼叫端採 fail-closed 處理。 */
export async function fetchLoginHistory(): Promise<LoginHistoryRecord[]> {
  const response = await fetch('/api/v1/account/login-history', { credentials: 'same-origin' });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as ApiError | null;
    throw new Error(body?.code ?? 'LOGIN_HISTORY_REQUEST_FAILED');
  }

  const value: unknown = await response.json();
  if (!isLoginHistoryResponse(value)) {
    // 資料契約漂移時不顯示舊 mock 紀錄，避免使用者把不可信畫面誤認為安全活動資訊。
    throw new Error('LOGIN_HISTORY_CONTRACT_ERROR');
  }
  return value.records;
}

function isLoginHistoryResponse(value: unknown): value is LoginHistoryResponse {
  if (typeof value !== 'object' || value === null || !Array.isArray((value as Partial<LoginHistoryResponse>).records)) {
    return false;
  }
  return (value as LoginHistoryResponse).records.every((record) => (
    typeof record === 'object' && record !== null && typeof record.occurredAt === 'string'
  ));
}
