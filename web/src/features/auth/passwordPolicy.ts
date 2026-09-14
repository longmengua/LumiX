/**
 * 使用者新密碼的前端同步契約。
 *
 * 這只改善輸入體驗，不能取代 server 驗證；調整這些數值時必須同步更新
 * `UserAuthenticationService` 與 P29-R02 文件，避免不同入口接受不同密碼。
 */
export const MIN_PASSWORD_LENGTH = 8;
export const MAX_PASSWORD_LENGTH = 32;
const DISALLOWED_PASSWORD_CHARACTERS = /[^A-Za-z0-9!@#$%^&*()_+\-=[\]{};:,.?]/g;
const ALLOWED_NEW_PASSWORD = /^[A-Za-z0-9!@#$%^&*()_+\-=[\]{};:,.?]+$/;

export function hasValidPasswordLength(password: string): boolean {
  return password.length >= MIN_PASSWORD_LENGTH && password.length <= MAX_PASSWORD_LENGTH;
}

/** 新密碼只使用 UI 已揭露的 ASCII 字元；登入既有密碼不套用，避免歷史帳戶被前端意外阻斷。 */
export function hasValidNewPassword(password: string): boolean {
  return hasValidPasswordLength(password) && ALLOWED_NEW_PASSWORD.test(password);
}

/**
 * 受控輸入在每次輸入與貼上時移除不允許字元並截斷上限，讓 browser 不會暫存無法提交的新密碼。
 */
export function sanitizeNewPasswordInput(password: string): string {
  return password.replace(DISALLOWED_PASSWORD_CHARACTERS, '').slice(0, MAX_PASSWORD_LENGTH);
}
