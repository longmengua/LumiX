import { assetTabs, tabForAccountType, type AssetAccountType, type AssetTabKey } from './assetAccountTypes';

export type AssetProjectionFreshness = 'RECONCILED' | 'UNRECONCILED';

export type AssetProjectionItem = {
  accountId: string;
  accountType: AssetAccountType;
  accountStatus: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  assetSymbol: string;
  assetDisplayName: string;
  assetStatus: 'ACTIVE' | 'HALTED' | 'DELISTED';
  precisionScale: number;
  total: string;
  available: string;
  locked: string;
  projectionVersion: number;
  projectedAt: string;
  reconciledAt: string | null;
  freshness: AssetProjectionFreshness;
};

export type AssetProjectionAccount = {
  key: AssetTabKey;
  items: AssetProjectionItem[];
};

export type AssetProjectionSnapshot = {
  source: 'BALANCE_PROJECTION';
  accounts: AssetProjectionAccount[];
};

type ApiError = { code?: unknown };

/**
 * 讀取登入者自己的後端 projection；不接受 owner、account 或 asset query parameter。
 *
 * <p>amount 全程保留後端的十進位字串，不能轉成 JavaScript number，否則大額／18 位小數會失去精度。</p>
 */
export async function fetchAssetProjectionSnapshot(signal?: AbortSignal): Promise<AssetProjectionSnapshot> {
  const response = await fetch('/api/v1/assets/balances', {
    credentials: 'same-origin',
    cache: 'no-store',
    signal,
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as ApiError | null;
    throw new Error(typeof body?.code === 'string' ? body.code : 'ASSET_PROJECTION_REQUEST_FAILED');
  }

  const value: unknown = await response.json();
  if (!isProjectionResponse(value)) {
    throw new Error('ASSET_PROJECTION_CONTRACT_ERROR');
  }

  const accounts = assetTabs.map((key) => ({ key, items: [] as AssetProjectionItem[] }));
  const accountByKey = new Map(accounts.map((account) => [account.key, account]));
  for (const item of value.items) {
    accountByKey.get(tabForAccountType(item.accountType))?.items.push(item);
  }
  return { source: 'BALANCE_PROJECTION', accounts };
}

function isProjectionResponse(value: unknown): value is { source: 'BALANCE_PROJECTION'; items: AssetProjectionItem[] } {
  if (!isRecord(value) || value.source !== 'BALANCE_PROJECTION' || !Array.isArray(value.items)) return false;
  return value.items.every(isProjectionItem);
}

/** 前端先驗證 public contract，避免 schema 漂移時以不完整資料呈現成可用餘額。 */
function isProjectionItem(value: unknown): value is AssetProjectionItem {
  if (!isRecord(value)) return false;
  return typeof value.accountId === 'string'
    && isAccountType(value.accountType)
    && isOneOf(value.accountStatus, ['ACTIVE', 'FROZEN', 'CLOSED'])
    && typeof value.assetSymbol === 'string'
    && typeof value.assetDisplayName === 'string'
    && isOneOf(value.assetStatus, ['ACTIVE', 'HALTED', 'DELISTED'])
    && typeof value.precisionScale === 'number' && Number.isInteger(value.precisionScale)
    && value.precisionScale >= 0 && value.precisionScale <= 18
    && isDecimalString(value.total) && isDecimalString(value.available) && isDecimalString(value.locked)
    && typeof value.projectionVersion === 'number' && Number.isSafeInteger(value.projectionVersion)
    && value.projectionVersion >= 0
    && isInstantString(value.projectedAt) && (value.reconciledAt === null || isInstantString(value.reconciledAt))
    && isOneOf(value.freshness, ['RECONCILED', 'UNRECONCILED']);
}

function isAccountType(value: unknown): value is AssetAccountType {
  return isOneOf(value, ['SPOT', 'FUTURES', 'MARGIN']);
}

function isOneOf<T extends string>(value: unknown, values: readonly T[]): value is T {
  return typeof value === 'string' && values.includes(value as T);
}

function isDecimalString(value: unknown): value is string {
  return typeof value === 'string' && /^\d+(?:\.\d+)?$/.test(value);
}

function isInstantString(value: unknown): value is string {
  return typeof value === 'string' && !Number.isNaN(Date.parse(value));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
