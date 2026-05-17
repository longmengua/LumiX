/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.WalletTransfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransferRepository {

    /** 新增或更新 transfer state。 */
    void save(WalletTransfer transfer);

    /** 依 transfer id 查詢單筆入金/出金狀態。 */
    Optional<WalletTransfer> findById(UUID id);

    /** 依使用者查詢 transfer 歷史，順序由 repository 實作維持。 */
    List<WalletTransfer> findByUid(long uid);
}
