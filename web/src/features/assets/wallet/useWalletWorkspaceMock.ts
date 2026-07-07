import { useEffect, useState } from 'react';

import { fetchWalletWorkspaceMock, type WalletWorkspaceData } from './mockWalletService';

export function useWalletWorkspaceMock() {
  const [data, setData] = useState<WalletWorkspaceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;

    async function loadWallet() {
      // 防止 unmount 後才回寫 state，避免看起來像是 wallet API 突然失敗。
      setLoading(true);
      setError(null);

      try {
        const snapshot = await fetchWalletWorkspaceMock();
        if (alive) {
          setData(snapshot);
        }
      } catch (loadError) {
        if (alive) {
          setError(loadError instanceof Error ? loadError.message : 'Unable to load wallet data.');
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void loadWallet();

    return () => {
      alive = false;
    };
  }, []);

  function reload() {
    // 重載仍然維持 mock fetch，讓 UI 測試時的行為可重現。
    setLoading(true);
    setError(null);
    void fetchWalletWorkspaceMock()
      .then((snapshot) => setData(snapshot))
      .catch((loadError) => setError(loadError instanceof Error ? loadError.message : 'Unable to load wallet data.'))
      .finally(() => setLoading(false));
  }

  return { data, loading, error, reload };
}
