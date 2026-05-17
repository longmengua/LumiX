/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.WalletTransfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransferRepository {

    void save(WalletTransfer transfer);

    Optional<WalletTransfer> findById(UUID id);

    List<WalletTransfer> findByUid(long uid);
}
