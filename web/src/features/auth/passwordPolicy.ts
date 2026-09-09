/**
 * 使用者密碼長度的前端同步契約。
 *
 * 這只改善輸入體驗，不能取代 server 驗證；調整這些數值時必須同步更新
 * `UserAuthenticationService` 與 P29-R02 文件，避免不同入口接受不同密碼。
 */
export const MIN_PASSWORD_LENGTH = 8;
export const MAX_PASSWORD_LENGTH = 32;

export function hasValidPasswordLength(password: string): boolean {
  return password.length >= MIN_PASSWORD_LENGTH && password.length <= MAX_PASSWORD_LENGTH;
}
