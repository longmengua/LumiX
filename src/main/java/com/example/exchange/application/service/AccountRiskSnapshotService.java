/*
 * 檔案用途：應用服務，產生並保存 account risk snapshots。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.AccountRiskSnapshotStore;
import com.example.exchange.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountRiskSnapshotService {

    private final AccountRiskService accountRiskService;
    private final AccountRiskSnapshotStore snapshotStore;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;

    public AccountRiskSnapshot persist(long uid) {
        AccountRiskSnapshot snapshot = accountRiskService.snapshot(uid);
        snapshotStore.save(snapshot);
        return snapshot;
    }

    public List<AccountRiskSnapshot> persistKnownAccounts() {
        Set<Long> uids = discoverAccountUids();
        if (uids.isEmpty()) return List.of();
        return uids.stream()
                .map(this::persist)
                .toList();
    }

    public Optional<AccountRiskSnapshot> latest(long uid) {
        return snapshotStore.findLatest(uid);
    }

    public List<AccountRiskSnapshot> history(long uid, int limit) {
        return snapshotStore.findByUid(uid, limit);
    }

    private Set<Long> discoverAccountUids() {
        Set<Long> uids = new LinkedHashSet<>();
        accountRepository.findAll().forEach(account -> uids.add(account.uid()));
        positionRepository.findOpenPositions().forEach(position -> uids.add(position.getUid()));
        return uids;
    }
}
