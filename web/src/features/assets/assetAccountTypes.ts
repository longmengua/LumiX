/** 資產頁的顯示分頁與後端 account type 一一對應；不可由前端自由創造新的資產帳戶域。 */
export type AssetTabKey = 'spot' | 'futures';

export type AssetAccountType = 'SPOT' | 'FUTURES';

export const assetTabs: readonly AssetTabKey[] = ['spot', 'futures'];

export const accountTypeByTab: Readonly<Record<AssetTabKey, AssetAccountType>> = {
  spot: 'SPOT',
  futures: 'FUTURES',
};

export const accountLabelKeyByTab: Readonly<Record<AssetTabKey, string>> = {
  spot: 'account.spotAccount',
  futures: 'account.futuresAccount',
};

export function tabForAccountType(accountType: AssetAccountType): AssetTabKey {
  switch (accountType) {
    case 'SPOT':
      return 'spot';
    case 'FUTURES':
      return 'futures';
  }
}
