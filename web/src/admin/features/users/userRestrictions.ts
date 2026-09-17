import type { AdminUser } from '../../api/adminUsersApi';

export type UserRestrictionKey = 'login' | 'withdrawal' | 'internal-transfer' | 'spot-trading' | 'futures-trading';

export type UserRestrictionView = {
  key: UserRestrictionKey;
  active: boolean;
  available: boolean;
};

/**
 * 將目前 API 的限制欄位收斂成 UI view model；新增後端能力時只需擴充此處，不可把欄位判斷散落到 JSX。
 */
export function mapUserRestrictions(user: AdminUser): UserRestrictionView[] {
  return [
    { key: 'login', active: user.status === 'SUSPENDED', available: true },
    { key: 'withdrawal', active: user.withdrawalFrozenAt !== null, available: true },
    { key: 'internal-transfer', active: isFundTransferRestricted(user), available: false },
    { key: 'spot-trading', active: false, available: false },
    { key: 'futures-trading', active: false, available: false },
  ];
}

/** system: 前綴是 V021 建立的系統 principal 慣例，不以 email 或顯示名稱猜測帳戶身分。 */
export function isSystemUser(user: AdminUser): boolean {
  return user.userId.startsWith('system:');
}

export function isFundTransferRestricted(user: AdminUser): boolean {
  return user.fundTransferRestrictedUntil !== null && Date.parse(user.fundTransferRestrictedUntil) > Date.now();
}

export function activeRestrictionCount(user: AdminUser): number {
  return mapUserRestrictions(user).filter((restriction) => restriction.active).length;
}
