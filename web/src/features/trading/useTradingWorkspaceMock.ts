import { useEffect, useState } from 'react';

import { fetchTradingWorkspaceMock, type TradingKind, type TradingWorkspaceData } from './mockTradingService';

export function useTradingWorkspaceMock(kind: TradingKind, symbol: string) {
  const [data, setData] = useState<TradingWorkspaceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadWorkspace() {
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
    setLoading(true);
    setError(null);
    void fetchTradingWorkspaceMock(kind, symbol)
      .then((snapshot) => setData(snapshot))
      .catch((loadError) => setError(loadError instanceof Error ? loadError.message : 'Unable to load trading workspace.'))
      .finally(() => setLoading(false));
  }

  return { data, loading, error, reload };
}

