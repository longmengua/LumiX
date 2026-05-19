/*
 * 檔案用途：Spring Data JPA repository，提供 durable wallet ledger posting 存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.WalletLedgerPostingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletLedgerPostingRecordJpaRepository
        extends JpaRepository<WalletLedgerPostingRecord, Long> {

    List<WalletLedgerPostingRecord> findByEntryIdOrderByLineNoAsc(String entryId);

    List<WalletLedgerPostingRecord> findByEntryIdInOrderByEntryIdAscLineNoAsc(List<String> entryIds);
}
