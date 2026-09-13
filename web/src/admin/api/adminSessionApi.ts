import { ADMIN_API_BASE_PATH } from './adminApi';

/** 後台只讀取 server 驗證後的最小 principal，絕不從 localStorage 還原權限或登入狀態。 */
export type AdminSession = {
  userId: string;
  email: string;
  displayName: string;
};

export async function fetchAdminSession(): Promise<AdminSession> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/session`, { credentials: 'same-origin' });
  if (!response.ok) {
    throw new Error((await response.json().catch(() => null) as { code?: string } | null)?.code ?? 'ADMIN_SESSION_UNAVAILABLE');
  }
  return response.json() as Promise<AdminSession>;
}
