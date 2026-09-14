/**
 * 後台密碼復原 API adapter。
 *
 * 後台使用獨立 endpoint，讓 server 可驗證 ACTIVE admin principal 並寄送固定導向 `/admin` 的一次性連結；
 * browser 不保存 token，僅在 email 連結開啟的重設表單中短暫提交。
 */
import { ADMIN_API_BASE_PATH } from './adminApi';

export type AdminAuthenticatedUser = {
  userId: string;
  email: string;
  displayName: string;
};

/**
 * 管理端永遠使用 `/api/admin` 的專用登入入口。
 *
 * 它只會接收 HttpOnly `LUMIX_ADMIN_SESSION`，不能把一般客戶登入 cookie 升格成管理端權限。
 */
export async function signInAdmin(input: {
  email: string;
  password: string;
  captchaToken: string;
}): Promise<AdminAuthenticatedUser> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/auth/login`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    const value = await response.json().catch(() => null) as { code?: string } | null;
    throw new Error(value?.code ?? 'ADMIN_AUTH_REQUEST_FAILED');
  }
  return response.json() as Promise<AdminAuthenticatedUser>;
}

/** 管理端登出只清除 `/api/admin` session，不能誤呼叫被入口隔離的前台 logout endpoint。 */
export async function signOutAdmin(): Promise<void> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}/auth/logout`, {
    method: 'POST',
    credentials: 'same-origin',
  });
  if (!response.ok && response.status !== 401) {
    throw new Error('ADMIN_SIGN_OUT_FAILED');
  }
}

export async function requestAdminPasswordReset(input: { email: string; captchaToken: string }): Promise<void> {
  await request('/auth/password/forgot', input);
}

export async function resetAdminPassword(input: { token: string; newPassword: string }): Promise<void> {
  await request('/auth/password/reset', input);
}

async function request(path: string, body: object): Promise<void> {
  const response = await fetch(`${ADMIN_API_BASE_PATH}${path}`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (response.ok) return;
  const value = await response.json().catch(() => null) as { code?: string } | null;
  throw new Error(value?.code ?? 'ADMIN_AUTH_REQUEST_FAILED');
}
