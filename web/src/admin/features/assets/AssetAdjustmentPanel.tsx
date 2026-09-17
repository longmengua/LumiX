import { GovernedAirdropForm } from './GovernedAirdropForm';

/** 資產調整頁層與目前唯一可執行 command 分離，避免把歷史 /airdrops endpoint 誤當通用 adjustment engine。 */
export function AssetAdjustmentPanel() {
  return <GovernedAirdropForm />;
}
