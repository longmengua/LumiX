import type { AssetAccountType } from './assetAccountTypes';

export type AssetLedgerHistoryItem = {
  entryId: string;
  journalId: string;
  accountType: AssetAccountType;
  assetSymbol: string;
  direction: 'DEBIT' | 'CREDIT';
  amount: string;
  referenceType: string;
  referenceId: string;
  postedAt: string;
  recordedAt: string;
};

export type AssetLedgerHistoryCursor = { postedAt: string; entryId: string };
export type AssetLedgerHistoryPage = {
  source: 'IMMUTABLE_LEDGER';
  items: AssetLedgerHistoryItem[];
  nextCursor: AssetLedgerHistoryCursor | null;
};

/**
 * 讀取目前 session owner 的 immutable ledger entry；cursor 只用於往更舊資料翻頁，不能指定帳戶或使用者。
 * 金額與 bigint ID 都保持字串，避免瀏覽器數值精度截斷造成錯誤資產呈現。
 */
export async function fetchAssetLedgerHistory(
  cursor?: AssetLedgerHistoryCursor,
  signal?: AbortSignal,
): Promise<AssetLedgerHistoryPage> {
  const parameters = new URLSearchParams({ limit: '25' });
  if (cursor) {
    parameters.set('beforePostedAt', cursor.postedAt);
    parameters.set('beforeEntryId', cursor.entryId);
  }
  const response = await fetch(`/api/v1/assets/history?${parameters.toString()}`, {
    credentials: 'same-origin', cache: 'no-store', signal,
  });
  if (!response.ok) throw new Error('ASSET_HISTORY_REQUEST_FAILED');
  const value: unknown = await response.json();
  if (!isHistoryPage(value)) throw new Error('ASSET_HISTORY_CONTRACT_ERROR');
  return value;
}

function isHistoryPage(value: unknown): value is AssetLedgerHistoryPage {
  return isRecord(value) && value.source === 'IMMUTABLE_LEDGER' && Array.isArray(value.items)
    && value.items.every(isHistoryItem) && (value.nextCursor === null || isCursor(value.nextCursor));
}

function isHistoryItem(value: unknown): value is AssetLedgerHistoryItem {
  return isRecord(value) && isPositiveIntegerString(value.entryId) && isPositiveIntegerString(value.journalId)
    && isOneOf(value.accountType, ['SPOT', 'FUTURES']) && typeof value.assetSymbol === 'string'
    && isOneOf(value.direction, ['DEBIT', 'CREDIT']) && isDecimal(value.amount)
    && typeof value.referenceType === 'string' && typeof value.referenceId === 'string'
    && isInstant(value.postedAt) && isInstant(value.recordedAt);
}

function isCursor(value: unknown): value is AssetLedgerHistoryCursor {
  return isRecord(value) && isInstant(value.postedAt) && isPositiveIntegerString(value.entryId);
}

function isOneOf<T extends string>(value: unknown, values: readonly T[]): value is T {
  return typeof value === 'string' && values.includes(value as T);
}
function isDecimal(value: unknown): value is string { return typeof value === 'string' && /^\d+(?:\.\d+)?$/.test(value); }
function isPositiveIntegerString(value: unknown): value is string { return typeof value === 'string' && /^[1-9]\d*$/.test(value); }
function isInstant(value: unknown): value is string { return typeof value === 'string' && !Number.isNaN(Date.parse(value)); }
function isRecord(value: unknown): value is Record<string, unknown> { return typeof value === 'object' && value !== null; }
