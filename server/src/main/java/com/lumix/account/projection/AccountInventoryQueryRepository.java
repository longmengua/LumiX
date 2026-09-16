package com.lumix.account.projection;

import java.util.List;

/** 帳戶 inventory read port；禁止在此邊界建立帳戶或更新帳戶狀態。 */
interface AccountInventoryQueryRepository { List<AccountInventoryItem> findByOwnerUserId(String userId); }
