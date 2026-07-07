import { AppRouter } from '../routes/AppRouter';

export function App() {
  // App 本身只做路由組裝，避免把頁面狀態或資料邏輯塞進最外層 shell。
  return <AppRouter />;
}
