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

/** 帳密正確但裝置尚未確認時，前端不得把它視為已登入使用者。 */
export type SignInResult =
  | { kind: 'authenticated'; user: AuthenticatedUser }
  | { kind: 'verification-required' };

type ApiError = {
  code?: string;
  message?: string;
};

export async function signIn(input: { email: string; password: string; captchaToken: string }): Promise<SignInResult> {
  const response = await fetch('/api/v1/auth/login', requestOptions(input));
  if (response.status === 202) {
    const value: unknown = await response.json();
    if (typeof value === 'object' && value !== null && (value as { verificationRequired?: unknown }).verificationRequired === true) {
      return { kind: 'verification-required' };
    }
    throw new Error('AUTH_CONTRACT_ERROR');
  }
  return { kind: 'authenticated', user: await readUser(response) };
}

export async function register(input: {
  email: string;
  displayName: string;
  password: string;
  captchaToken: string;
}): Promise<AuthenticatedUser> {
  return requestUser('/register', input);
}

export async function currentUser(): Promise<AuthenticatedUser> {
  const response = await fetch('/api/v1/auth/me', { credentials: 'same-origin' });
  return readUser(response);
}

/**
 * 更新目前登入者可自行維護的 profile 欄位。
 *
 * 後端從 HttpOnly session 取得 owner，前端不得傳送或保存 userId；回傳值用於同步導覽列的去敏使用者投影。
 */
export async function updateDisplayName(displayName: string): Promise<AuthenticatedUser> {
  const response = await fetch('/api/v1/account/profile', requestOptions({ displayName }, 'PATCH'));
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

export async function requestPasswordReset(input: { email: string; captchaToken: string }): Promise<void> {
  const response = await fetch('/api/v1/auth/password/forgot', requestOptions(input));
  await ensureSuccess(response);
}

export type SliderCaptchaPurpose = 'LOGIN' | 'REGISTRATION' | 'PASSWORD_RESET';

export type SliderCaptchaChallenge = {
  captchaId: string;
  type: 'SLIDER';
  backgroundImage: string;
  pieceImage: string;
  pieceY: number;
  width: number;
  height: number;
  pieceWidth: number;
  pieceHeight: number;
};

export type IconMatchCaptchaChallenge = {
  captchaId: string;
  type: 'ICON_MATCH';
  referenceImage: string;
  candidateImages: string[];
  prompt: string;
};

export type ImageGridCaptchaChallenge = {
  captchaId: string;
  type: 'IMAGE_GRID';
  gridImages: string[];
  prompt: string;
};

export type CaptchaChallenge = SliderCaptchaChallenge | IconMatchCaptchaChallenge | ImageGridCaptchaChallenge;

/**
 * 取得 server-side slider challenge。答案不在 response 中，前端只負責呈現與回傳使用者的位移。
 */
export async function createCaptchaChallenge(): Promise<CaptchaChallenge> {
  const response = await fetch('/api/v1/auth/captcha/challenge', { credentials: 'same-origin', cache: 'no-store' });
  await ensureSuccess(response);
  const value: unknown = await response.json();
  if (!isCaptchaChallenge(value)) throw new Error('CAPTCHA_CONTRACT_ERROR');
  return value;
}

/** 成功後取得用途限定的一次性通行 token；它不是 session，也不能保存至 browser storage。 */
export async function verifySliderCaptcha(input: {
  captchaId: string;
  type: CaptchaChallenge['type'];
  purpose: SliderCaptchaPurpose;
  offsetX?: number;
  selectedIndexes?: number[];
}): Promise<string> {
  const response = await fetch('/api/v1/auth/captcha/challenge/verify', requestOptions(input));
  await ensureSuccess(response);
  const value: unknown = await response.json();
  if (typeof value !== 'object' || value === null || typeof (value as { captchaToken?: unknown }).captchaToken !== 'string') {
    throw new Error('CAPTCHA_CONTRACT_ERROR');
  }
  return (value as { captchaToken: string }).captchaToken;
}

export async function resetPassword(input: { token: string; newPassword: string }): Promise<void> {
  const response = await fetch('/api/v1/auth/password/reset', requestOptions(input));
  await ensureSuccess(response);
}

/**
 * 原始登入瀏覽器才會攜帶 pending HttpOnly cookie。
 *
 * 此呼叫回傳 null 代表尚未核准；前端絕不能自行假設 email 已點選就已登入。
 */
export async function completeLoginVerification(): Promise<AuthenticatedUser | null> {
  const response = await fetch('/api/v1/auth/login-verification/complete', {
    method: 'POST',
    credentials: 'same-origin',
  });
  if (response.status === 202) return null;
  return readUser(response);
}

/** email 確認頁以明確 POST 送出 Yes／No，GET link 不會有任何狀態變更。 */
export async function decideLoginVerification(token: string, approved: boolean): Promise<'APPROVED' | 'REJECTED'> {
  const response = await fetch(
    '/api/v1/auth/login-verification/decision',
    requestOptions({ token, approved }),
  );
  await ensureSuccess(response);
  const value: unknown = await response.json();
  const state = typeof value === 'object' && value !== null ? (value as { state?: unknown }).state : null;
  if (state !== 'APPROVED' && state !== 'REJECTED') throw new Error('AUTH_CONTRACT_ERROR');
  return state;
}

async function requestUser(path: string, body: object): Promise<AuthenticatedUser> {
  const response = await fetch(`/api/v1/auth${path}`, requestOptions(body));
  return readUser(response);
}

function requestOptions(body: object, method = 'POST'): RequestInit {
  return {
    method,
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

function isCaptchaChallenge(value: unknown): value is CaptchaChallenge {
  if (typeof value !== 'object' || value === null) return false;
  const challenge = value as Partial<CaptchaChallenge>;
  if (typeof challenge.captchaId !== 'string' || typeof challenge.type !== 'string') return false;
  if (challenge.type === 'SLIDER') {
    const slider = challenge as Partial<SliderCaptchaChallenge>;
    return typeof slider.backgroundImage === 'string' && typeof slider.pieceImage === 'string'
      && typeof slider.pieceY === 'number' && typeof slider.width === 'number' && typeof slider.height === 'number'
      && typeof slider.pieceWidth === 'number' && typeof slider.pieceHeight === 'number';
  }
  if (challenge.type === 'ICON_MATCH') {
    const icon = challenge as Partial<IconMatchCaptchaChallenge>;
    return typeof icon.referenceImage === 'string' && Array.isArray(icon.candidateImages) && typeof icon.prompt === 'string';
  }
  if (challenge.type === 'IMAGE_GRID') {
    const grid = challenge as Partial<ImageGridCaptchaChallenge>;
    return Array.isArray(grid.gridImages) && typeof grid.prompt === 'string';
  }
  return false;
}
