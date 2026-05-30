/*
 * 檔案用途：Spring Data JPA repository，提供 durable wallet ledger entry 存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.WalletLedgerEntryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface WalletLedgerEntryRecordJpaRepository
        extends JpaRepository<WalletLedgerEntryRecord, String> {

    List<WalletLedgerEntryRecord> findByUidOrderByCreatedAtAscIdAsc(Long uid);

    List<WalletLedgerEntryRecord> findByUidAndAssetOrderByCreatedAtAscIdAsc(Long uid, String asset);

    List<WalletLedgerEntryRecord> findByRefIdOrderByCreatedAtAscIdAsc(String refId);

    List<WalletLedgerEntryRecord> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
            Instant fromInclusive,
            Instant toExclusive
    );
}
