package com.lumix.admin.asset;

import java.util.List;

/** 管理端歷史查詢只讀 port；不得在此建立補發、空投或帳本修正 command。 */
interface AdminUserLedgerHistoryQueryRepository { List<AdminUserLedgerHistoryItem> findLatestByUserId(String userId, int limit); }
