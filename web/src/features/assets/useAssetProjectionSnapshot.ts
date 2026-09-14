import { useCallback, useEffect, useState } from 'react';

import { fetchAssetProjectionSnapshot, type AssetProjectionSnapshot } from './assetProjectionApi';

/**
 * 資產 projection 的受控讀取狀態。
 *
 * <p>重新整理與卸載都會取消舊 request，避免慢回應覆蓋較新的真實 snapshot；發生錯誤時不保留 mock 或舊假資料。</p>
 */
export function useAssetProjectionSnapshot() {
  const [data, setData] = useState<AssetProjectionSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setErrorCode(null);

    void fetchAssetProjectionSnapshot(controller.signal)
      .then((snapshot) => {
        if (!controller.signal.aborted) setData(snapshot);
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setData(null);
          setErrorCode(error instanceof Error ? error.message : 'ASSET_PROJECTION_REQUEST_FAILED');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [requestVersion]);

  const reload = useCallback(() => setRequestVersion((version) => version + 1), []);
  return { data, loading, errorCode, reload };
}
