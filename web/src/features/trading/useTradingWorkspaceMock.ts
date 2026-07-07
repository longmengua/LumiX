import { useEffect, useState } from 'react';

import { fetchTradingWorkspaceMock, type TradingKind, type TradingWorkspaceData } from './mockTradingService';

export function useTradingWorkspaceMock(kind: TradingKind, symbol: string) {
  const [data, setData] = useState<TradingWorkspaceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadWorkspace() {
      // 以 alive 標記防止 unmount 後還更新 state，避免切頁時出現假錯誤訊息。
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchTradingWorkspaceMock(kind, symbol);
        if (alive) {
          setData(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load trading workspace.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadWorkspace();

    return () => {
      alive = false;
    };
  }, [kind, symbol]);

  function reload() {
    // 重新整理同樣走 mock fetch，讓頁面重載行為保持一致。
    setLoading(true);
    setError(null);
    void fetchTradingWorkspaceMock(kind, symbol)
      .then((snapshot) => setData(snapshot))
      .catch((loadError) => setError(loadError instanceof Error ? loadError.message : 'Unable to load trading workspace.'))
      .finally(() => setLoading(false));
  }

  return { data, loading, error, reload };
}
