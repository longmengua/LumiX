/**
 * 使用者認證 API adapter。
 *
 * session 由瀏覽器以 HttpOnly Cookie 管理，前端只取得去敏後的使用者投影；不可將 token 寫入
 * localStorage、sessionStorage 或 React state。
 */
export type AuthenticatedUser = {
  userId: string;
  email: string;
  displayName: string;
};

type ApiError = {
  code?: string;
  message?: string;
};

export async function signIn(input: { email: string; password: string }): Promise<AuthenticatedUser> {
  return requestUser('/login', input);
}

export async function register(input: {
  email: string;
  displayName: string;
  password: string;
}): Promise<AuthenticatedUser> {
  return requestUser('/register', input);
}

export async function currentUser(): Promise<AuthenticatedUser> {
  const response = await fetch('/api/v1/auth/me', { credentials: 'same-origin' });
  return readUser(response);
}

export async function signOut(): Promise<void> {
  const response = await fetch('/api/v1/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
  });
  await ensureSuccess(response);
}

export async function changePassword(input: { currentPassword: string; newPassword: string }): Promise<AuthenticatedUser> {
  return requestUser('/password/change', input);
}

export async function requestPasswordReset(email: string): Promise<void> {
  const response = await fetch('/api/v1/auth/password/forgot', requestOptions({ email }));
  await ensureSuccess(response);
}

export async function resetPassword(input: { token: string; newPassword: string }): Promise<void> {
  const response = await fetch('/api/v1/auth/password/reset', requestOptions(input));
  await ensureSuccess(response);
}

async function requestUser(path: string, body: object): Promise<AuthenticatedUser> {
  const response = await fetch(`/api/v1/auth${path}`, requestOptions(body));
  return readUser(response);
}

function requestOptions(body: object): RequestInit {
  return {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}

async function readUser(response: Response): Promise<AuthenticatedUser> {
  await ensureSuccess(response);
  const value: unknown = await response.json();
  if (!isAuthenticatedUser(value)) {
    // Contract 漂移不能被前端靜默吞掉，否則容易將非認證 response 當成登入成功。
    throw new Error('AUTH_CONTRACT_ERROR');
  }
  return value;
}

async function ensureSuccess(response: Response): Promise<void> {
  if (response.ok) return;

  const body = await response.json().catch(() => null) as ApiError | null;
  // 只使用 server 的穩定錯誤碼；不把未受信任的 response message 直接顯示到 UI。
  throw new Error(body?.code ?? 'AUTH_REQUEST_FAILED');
}

function isAuthenticatedUser(value: unknown): value is AuthenticatedUser {
  if (typeof value !== 'object' || value === null) return false;
  const user = value as Partial<AuthenticatedUser>;
  return typeof user.userId === 'string'
    && typeof user.email === 'string'
    && typeof user.displayName === 'string';
}
