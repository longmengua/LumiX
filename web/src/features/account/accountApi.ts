/**
 * 帳戶頁已接入的使用者私有 API。
 *
 * <p>session 一律由同源 HttpOnly Cookie 自動附帶；這個 adapter 只接受 server 已去敏的登入時間，
 * 不保存或處理任何 session token。</p>
 */
export type LoginHistoryRecord = {
  occurredAt: string;
  ipAddress: string | null;
  deviceLabel: string | null;
};

/** 個人中心的真實基本資料；不混入尚未接線的 KYC 或資產 mock 欄位。 */
export type AccountProfileRecord = {
  userId: string;
  email: string;
  displayName: string;
  createdAt: string;
};

/** 個人中心安全設定與目前仍有效的綁定裝置；不含 device cookie、token 或 User-Agent digest。 */
export type LoginSecurityRecord = {
  settings: {
    newDeviceLoginEmailNotificationEnabled: boolean;
  };
  devices: Array<{
    deviceId: string;
    deviceLabel: string;
    lastIpAddress: string;
    createdAt: string;
    lastSeenAt: string;
  }>;
};

type LoginHistoryResponse = {
  records: LoginHistoryRecord[];
  hasOlder: boolean;
  hasNewer: boolean;
};

export type LoginHistoryPage = LoginHistoryResponse;

export type LoginHistoryPageRequest = {
  limit?: number;
  before?: string;
  after?: string;
  anchor?: string;
};

type ApiError = {
  code?: string;
};

/**
 * 讀取目前登入者自己的成功登入紀錄頁。
 *
 * cursor 是畫面已顯示的 occurredAt：before 讀更舊、after 讀更新、anchor 讓重新整理可回到原本查看
 * 的時間窗口。它不攜帶 userId 或 session 身分，所有權仍只由 HttpOnly Cookie 決定。
 */
export async function fetchLoginHistoryPage(request: LoginHistoryPageRequest = {}): Promise<LoginHistoryPage> {
  const params = new URLSearchParams({ limit: String(request.limit ?? 10) });
  if (request.before) params.set('before', request.before);
  if (request.after) params.set('after', request.after);
  if (request.anchor) params.set('anchor', request.anchor);

  const response = await fetch(`/api/v1/account/login-history?${params.toString()}`, { credentials: 'same-origin' });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as ApiError | null;
    throw new Error(body?.code ?? 'LOGIN_HISTORY_REQUEST_FAILED');
  }

  const value: unknown = await response.json();
  if (!isLoginHistoryResponse(value)) {
    // 資料契約漂移時不顯示舊 mock 紀錄，避免使用者把不可信畫面誤認為安全活動資訊。
    throw new Error('LOGIN_HISTORY_CONTRACT_ERROR');
  }
  return value;
}

/** 取得目前登入者自己的 profile；沒有成功 response 時不可回退到 mock profile。 */
export async function fetchAccountProfile(): Promise<AccountProfileRecord> {
  const response = await fetch('/api/v1/account/profile', { credentials: 'same-origin' });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as ApiError | null;
    throw new Error(body?.code ?? 'ACCOUNT_PROFILE_REQUEST_FAILED');
  }

  const value: unknown = await response.json();
  if (!isAccountProfileRecord(value)) {
    // profile 是使用者資料真實來源，contract 漂移時不能呈現舊快取或 mock 身份。
    throw new Error('ACCOUNT_PROFILE_CONTRACT_ERROR');
  }
  return value;
}

/** 讀取目前 session owner 的新裝置通知開關與綁定裝置，不能由 client 指定其他 userId。 */
export async function fetchLoginSecurity(): Promise<LoginSecurityRecord> {
  const response = await fetch('/api/v1/account/security', { credentials: 'same-origin' });
  if (!response.ok) throw new Error('LOGIN_SECURITY_REQUEST_FAILED');
  const value: unknown = await response.json();
  if (!isLoginSecurityRecord(value)) throw new Error('LOGIN_SECURITY_CONTRACT_ERROR');
  return value;
}

/** 開關只改變未知裝置是否需要 email 核准；既有裝置與 session 不會被此操作刪除。 */
export async function updateNewDeviceEmailNotification(enabled: boolean): Promise<LoginSecurityRecord['settings']> {
  const response = await fetch('/api/v1/account/security/new-device-email-notification', {
    method: 'PATCH',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled }),
  });
  if (!response.ok) throw new Error('LOGIN_SECURITY_UPDATE_FAILED');
  const value: unknown = await response.json();
  if (typeof value !== 'object' || value === null
    || typeof (value as { newDeviceLoginEmailNotificationEnabled?: unknown }).newDeviceLoginEmailNotificationEnabled !== 'boolean') {
    throw new Error('LOGIN_SECURITY_CONTRACT_ERROR');
  }
  return value as LoginSecurityRecord['settings'];
}

/** 移除綁定裝置會讓後端撤銷該裝置所有 active session，而非只從前端清單刪除。 */
export async function removeBoundLoginDevice(deviceId: string): Promise<void> {
  const response = await fetch(`/api/v1/account/security/devices/${encodeURIComponent(deviceId)}`, {
    method: 'DELETE',
    credentials: 'same-origin',
  });
  if (!response.ok) throw new Error('LOGIN_SECURITY_REMOVE_DEVICE_FAILED');
}

function isLoginHistoryResponse(value: unknown): value is LoginHistoryResponse {
  if (typeof value !== 'object' || value === null || !Array.isArray((value as Partial<LoginHistoryResponse>).records)) {
    return false;
  }
  const page = value as Partial<LoginHistoryResponse>;
  const records = page.records;
  return typeof page.hasOlder === 'boolean'
    && typeof page.hasNewer === 'boolean'
    && Array.isArray(records)
    && records.every((record) => (
    typeof record === 'object'
      && record !== null
      && typeof record.occurredAt === 'string'
      && ((record as Partial<LoginHistoryRecord>).ipAddress === null
        || typeof (record as Partial<LoginHistoryRecord>).ipAddress === 'string')
      && ((record as Partial<LoginHistoryRecord>).deviceLabel === null
        || typeof (record as Partial<LoginHistoryRecord>).deviceLabel === 'string')
    ));
}

function isAccountProfileRecord(value: unknown): value is AccountProfileRecord {
  if (typeof value !== 'object' || value === null) return false;
  const profile = value as Partial<AccountProfileRecord>;
  return typeof profile.userId === 'string'
    && typeof profile.email === 'string'
    && typeof profile.displayName === 'string'
    && typeof profile.createdAt === 'string';
}

function isLoginSecurityRecord(value: unknown): value is LoginSecurityRecord {
  if (typeof value !== 'object' || value === null) return false;
  const security = value as Partial<LoginSecurityRecord>;
  return typeof security.settings === 'object'
    && security.settings !== null
    && typeof security.settings.newDeviceLoginEmailNotificationEnabled === 'boolean'
    && Array.isArray(security.devices)
    && security.devices.every((device) => (
      typeof device === 'object'
      && device !== null
      && typeof (device as LoginSecurityRecord['devices'][number]).deviceId === 'string'
      && typeof (device as LoginSecurityRecord['devices'][number]).deviceLabel === 'string'
      && typeof (device as LoginSecurityRecord['devices'][number]).lastIpAddress === 'string'
      && typeof (device as LoginSecurityRecord['devices'][number]).createdAt === 'string'
      && typeof (device as LoginSecurityRecord['devices'][number]).lastSeenAt === 'string'
    ));
}
