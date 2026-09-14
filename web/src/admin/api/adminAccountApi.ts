import { ADMIN_API_BASE_PATH } from './adminApi';

export type AdminLoginHistoryRecord = {
  occurredAt: string;
  ipAddress: string | null;
  deviceLabel: string | null;
};

/** 管理員僅能讀取自己的 session history；browser 不會傳送或保存 userId。 */
export async function fetchAdminLoginHistory(): Promise<AdminLoginHistoryRecord[]> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/account/login-history?limit=50`, { credentials: 'same-origin' });
  if (!response.ok) throw new Error('ADMIN_LOGIN_HISTORY_REQUEST_FAILED');
  const value = await response.json() as { records?: unknown };
  if (!Array.isArray(value.records) || !value.records.every(isAdminLoginHistoryRecord)) {
    throw new Error('ADMIN_LOGIN_HISTORY_CONTRACT_ERROR');
  }
  return value.records;
}

function isAdminLoginHistoryRecord(value: unknown): value is AdminLoginHistoryRecord {
  if (typeof value !== 'object' || value === null) return false;
  const record = value as Partial<AdminLoginHistoryRecord>;
  return typeof record.occurredAt === 'string'
    && (record.ipAddress === null || typeof record.ipAddress === 'string')
    && (record.deviceLabel === null || typeof record.deviceLabel === 'string');
}
