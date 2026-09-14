import type { ReactNode } from 'react';

import { AdminStatusPage } from '../pages/AdminStatusPages';

type AdminPermissionGuardProps = { allowed: boolean; children: ReactNode };

/**
 * 權限拒絕必須以 403 留在後台殼層，而不是把使用者悄悄導回首頁。
 * 目前 session projection 尚未提供 page-level permission，呼叫端只能傳入已由既有授權層確認的結果，不能自行猜測角色。
 */
export function AdminPermissionGuard({ allowed, children }: AdminPermissionGuardProps) {
  return allowed ? children : <AdminStatusPage status="forbidden" />;
}
