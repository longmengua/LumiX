/**
 * 後台密碼復原 API adapter。
 *
 * 後台使用獨立 endpoint，讓 server 可驗證 ACTIVE admin principal 並寄送固定導向 `/admin` 的一次性連結；
 * browser 不保存 token，僅在 email 連結開啟的重設表單中短暫提交。
 */
import { ADMIN_API_BASE_PATH } from './adminApi';

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
