/** 資產頁的顯示分頁與後端 account type 一一對應；不可由前端自由創造新的資產帳戶域。 */
export type AssetTabKey = 'spot' | 'futures' | 'margin';

export type AssetAccountType = 'SPOT' | 'FUTURES' | 'MARGIN';

export const assetTabs: readonly AssetTabKey[] = ['spot', 'futures', 'margin'];

export const accountTypeByTab: Readonly<Record<AssetTabKey, AssetAccountType>> = {
  spot: 'SPOT',
  futures: 'FUTURES',
  margin: 'MARGIN',
};

export const accountLabelKeyByTab: Readonly<Record<AssetTabKey, string>> = {
  spot: 'account.spotAccount',
  futures: 'account.futuresAccount',
  margin: 'account.marginAccount',
};

export function tabForAccountType(accountType: AssetAccountType): AssetTabKey {
  switch (accountType) {
    case 'SPOT':
      return 'spot';
    case 'FUTURES':
      return 'futures';
    case 'MARGIN':
      return 'margin';
  }
}
