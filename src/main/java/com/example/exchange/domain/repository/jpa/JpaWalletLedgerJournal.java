/*
 * 檔案用途：JPA adapter，實作 durable wallet ledger journal。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.LedgerTamperEvidenceReport;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerEntryRecord;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.model.entity.WalletLedgerPostingRecord;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import com.example.exchange.domain.util.WalletLedgerHash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaWalletLedgerJournal implements WalletLedgerJournal {

    private final WalletLedgerEntryRecordJpaRepository entryRepository;
    private final WalletLedgerPostingRecordJpaRepository postingRepository;

    @Override
    @Transactional
    public void append(WalletLedgerEntry entry) {
        WalletLedgerJournal.validateBalancedEntry(entry);
        List<WalletLedgerPostingRecord> postings = new ArrayList<>();
        int lineNo = 1;
        for (WalletLedgerPosting posting : entry.getPostings()) {
            postings.add(WalletLedgerPostingRecord.from(entry, posting, lineNo++));
        }
        String previousHash = entryRepository.findTopByOrderByCreatedAtDescIdDesc()
                .map(WalletLedgerEntryRecord::getEntryHash)
                .filter(hash -> hash != null && !hash.isBlank())
                .orElse(WalletLedgerHash.GENESIS_HASH);
        WalletLedgerEntryRecord record = WalletLedgerEntryRecord.from(entry, WalletLedgerJournal.SCHEMA_VERSION);
        record.setPreviousHash(previousHash);
        record.setEntryHash(WalletLedgerHash.entryHash(previousHash, entry));
        entryRepository.save(record);
        postingRepository.saveAll(postings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletLedgerEntry> findByUid(long uid) {
        return toEntries(entryRepository.findByUidOrderByCreatedAtAscIdAsc(uid));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletLedgerEntry> findByUidAndAsset(long uid, String asset) {
        return toEntries(entryRepository.findByUidAndAssetOrderByCreatedAtAscIdAsc(uid, asset));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletLedgerEntry> findByRefId(String refId) {
        if (refId == null || refId.isBlank()) return List.of();
        return toEntries(entryRepository.findByRefIdOrderByCreatedAtAscIdAsc(refId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletLedgerEntry> findByCreatedAtBetween(Instant fromInclusive, Instant toExclusive) {
        if (fromInclusive == null || toExclusive == null || !fromInclusive.isBefore(toExclusive)) return List.of();
        return toEntries(entryRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
                fromInclusive,
                toExclusive
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerTamperEvidenceReport verifyTamperEvidence() {
        List<WalletLedgerEntryRecord> records = entryRepository.findAllByOrderByCreatedAtAscIdAsc();
        if (records.isEmpty()) {
            return new LedgerTamperEvidenceReport(0, 0, Instant.now(), List.of());
        }
        List<String> entryIds = records.stream().map(WalletLedgerEntryRecord::getId).toList();
        Map<String, List<WalletLedgerPosting>> postingsByEntryId = new LinkedHashMap<>();
        for (WalletLedgerPostingRecord postingRecord : postingRepository.findByEntryIdInOrderByEntryIdAscLineNoAsc(entryIds)) {
            postingsByEntryId
                    .computeIfAbsent(postingRecord.getEntryId(), ignored -> new ArrayList<>())
                    .add(postingRecord.toPosting());
        }
        List<String> issues = new ArrayList<>();
        String expectedPrevious = WalletLedgerHash.GENESIS_HASH;
        for (WalletLedgerEntryRecord record : records) {
            if (record.getPreviousHash() == null || record.getEntryHash() == null) {
                issues.add("MISSING_HASH:" + record.getId());
                expectedPrevious = record.getEntryHash();
                continue;
            }
            if (!expectedPrevious.equals(record.getPreviousHash())) {
                issues.add("PREVIOUS_HASH_MISMATCH:" + record.getId());
            }
            WalletLedgerEntry entry = toEntry(record, postingsByEntryId.getOrDefault(record.getId(), List.of()));
            String recalculated = WalletLedgerHash.entryHash(record.getPreviousHash(), entry);
            if (!recalculated.equals(record.getEntryHash())) {
                issues.add("ENTRY_HASH_MISMATCH:" + record.getId());
            }
            expectedPrevious = record.getEntryHash();
        }
        return new LedgerTamperEvidenceReport(records.size(), issues.size(), Instant.now(), issues);
    }

    private List<WalletLedgerEntry> toEntries(List<WalletLedgerEntryRecord> records) {
        if (records == null || records.isEmpty()) return List.of();
        List<String> entryIds = records.stream().map(WalletLedgerEntryRecord::getId).toList();
        Map<String, List<WalletLedgerPosting>> postingsByEntryId = new LinkedHashMap<>();
        for (WalletLedgerPostingRecord postingRecord : postingRepository.findByEntryIdInOrderByEntryIdAscLineNoAsc(entryIds)) {
            postingsByEntryId
                    .computeIfAbsent(postingRecord.getEntryId(), ignored -> new ArrayList<>())
                    .add(postingRecord.toPosting());
        }

        return records.stream()
                .sorted(Comparator
                        .comparing(WalletLedgerEntryRecord::getCreatedAt)
                        .thenComparing(WalletLedgerEntryRecord::getId))
                .map(record -> toEntry(record, postingsByEntryId.getOrDefault(record.getId(), List.of())))
                .toList();
    }

    private WalletLedgerEntry toEntry(WalletLedgerEntryRecord record, List<WalletLedgerPosting> postings) {
        return WalletLedgerEntry.builder()
                .id(UUID.fromString(record.getId()))
                .uid(record.getUid())
                .asset(record.getAsset())
                .reason(record.getReason())
                .refId(record.getRefId())
                .amount(record.getAmount())
                .balanceAfter(record.getBalanceAfter())
                .createdAt(record.getCreatedAt())
                .postings(postings)
                .build();
    }
}
