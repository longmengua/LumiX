import { useCallback, useEffect, useState } from 'react';
import { fetchAssetLedgerHistory, type AssetLedgerHistoryCursor, type AssetLedgerHistoryItem } from './assetLedgerHistoryApi';

/**
 * 歷史頁採 append-only keyset cursor；載入更多只使用 API 已回傳的 cursor，避免 offset 在新 entry 寫入後漂移。
 * 發生錯誤時不會保留或補造 mock history，使用者可明確重試。
 */
export function useAssetLedgerHistory() {
  const [items, setItems] = useState<AssetLedgerHistoryItem[]>([]);
  const [nextCursor, setNextCursor] = useState<AssetLedgerHistoryCursor | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setErrorCode(null);
    void fetchAssetLedgerHistory(undefined, controller.signal).then((page) => {
      if (!controller.signal.aborted) { setItems(page.items); setNextCursor(page.nextCursor); }
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) { setItems([]); setNextCursor(null); setErrorCode(error instanceof Error ? error.message : 'ASSET_HISTORY_REQUEST_FAILED'); }
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);
  const loadMore = useCallback(() => {
    if (!nextCursor || loadingMore) return;
    setLoadingMore(true); setErrorCode(null);
    void fetchAssetLedgerHistory(nextCursor).then((page) => {
      setItems((current) => [...current, ...page.items]); setNextCursor(page.nextCursor);
    }).catch((error: unknown) => setErrorCode(error instanceof Error ? error.message : 'ASSET_HISTORY_REQUEST_FAILED'))
      .finally(() => setLoadingMore(false));
  }, [loadingMore, nextCursor]);
  return { items, nextCursor, loading, loadingMore, errorCode, reload, loadMore };
}
