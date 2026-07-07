import { useEffect, useState } from 'react';

import { fetchAssetOverviewMock, type AssetOverviewData } from './mockAssetService';

export function useAssetOverviewMock() {
  const [data, setData] = useState<AssetOverviewData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadAssets() {
      // 以 alive 標記防止 unmount 後還更新 state，避免測試或切頁時出現假警報。
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchAssetOverviewMock();
        if (alive) {
          setData(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load asset data.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadAssets();

    return () => {
      alive = false;
    };
  }, []);

  function reload() {
    setLoading(true);
    setError(null);
    void fetchAssetOverviewMock()
      .then((snapshot) => setData(snapshot))
      .catch((loadError) => setError(loadError instanceof Error ? loadError.message : 'Unable to load asset data.'))
      .finally(() => setLoading(false));
  }

  return { data, loading, error, reload };
}
